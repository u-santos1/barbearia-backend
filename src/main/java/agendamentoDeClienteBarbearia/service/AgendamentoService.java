package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.StatusAgendamento;
import agendamentoDeClienteBarbearia.dtos.*;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.*;
import agendamentoDeClienteBarbearia.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;

    private static final int HORARIO_ABERTURA = 6;
    private static final int HORARIO_FECHAMENTO = 23;
    private static final int INTERVALO_AGENDA_MINUTOS = 30;
    private static final ZoneId TIMEZONE_BRASIL = ZoneId.of("America/Sao_Paulo");

    // --- CORE: AGENDAR ---

    @Transactional
    public DetalhamentoAgendamentoDTO agendar(AgendamentoDTO dados) {
        log.info("Iniciando agendamento para Cliente ID: {}", dados.clienteId());

        Barbeiro barbeiro = barbeiroRepository.findById(dados.barbeiroId())
                .orElseThrow(() -> new RegraDeNegocioException("Barbeiro não encontrado"));

        if (!barbeiro.getAtivo()) {
            throw new RegraDeNegocioException("Este barbeiro não está atendendo no momento.");
        }

        Cliente cliente = clienteRepository.findById(dados.clienteId())
                .orElseThrow(() -> new RegraDeNegocioException("Cliente não encontrado. Realize o cadastro antes."));

        Servico servico = servicoRepository.findById(dados.servicoId())
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        LocalDateTime dataInicio = dados.dataHoraInicio();
        validarHorarioFuncionamento(dataInicio);

        // Validação de fuso horário centralizada
        if (dataInicio.isBefore(LocalDateTime.now(TIMEZONE_BRASIL))) {
            throw new RegraDeNegocioException("Não é possível agendar em datas passadas.");
        }

        LocalDateTime dataFim = dataInicio.plusMinutes(servico.getDuracaoEmMinutos());

        if (agendamentoRepository.existeConflitoDeHorario(barbeiro.getId(), dataInicio, dataFim)) {
            throw new RegraDeNegocioException("Este horário já está ocupado.");
        }

        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServico(servico);
        agendamento.setDataHoraInicio(dataInicio);
        agendamento.setDataHoraFim(dataFim);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setValorCobrado(servico.getPreco());

        calcularDivisaoFinanceira(agendamento, barbeiro);

        return new DetalhamentoAgendamentoDTO(agendamentoRepository.save(agendamento));
    }

    // --- MÉTODOS DE STATUS E CANCELAMENTO ---

    @Transactional
    public void cancelar(Long id) {
        log.info("Processando cancelamento para ID: {}", id);
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        // Se for bloqueio (cliente null), removemos para limpar a agenda
        if (agendamento.getCliente() == null) {
            agendamentoRepository.delete(agendamento);
            return;
        }

        // Status encurtado para evitar erro de limite de caracteres (character varying 20)
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);
    }

    @Transactional
    public void cancelarPeloBarbeiro(Long id) {
        this.cancelar(id); // Reuso de lógica para consistência
    }

    @Transactional
    public void confirmar(Long id) { alterarStatus(id, StatusAgendamento.CONFIRMADO); }

    @Transactional
    public void concluir(Long id) { alterarStatus(id, StatusAgendamento.CONCLUIDO); }

    private void alterarStatus(Long id, StatusAgendamento novoStatus) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Agendamento não encontrado"));
        agendamento.setStatus(novoStatus);
        agendamentoRepository.save(agendamento);
    }

    // --- BUSCA E DISPONIBILIDADE ---

    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> buscarPorTelefoneCliente(String telefone) {
        try {
            String telLimpo = telefone.replaceAll("\\D", "");
            LocalDateTime agora = LocalDateTime.now(TIMEZONE_BRASIL);

            // JOIN FETCH é obrigatório no Repository para evitar LazyInitializationException
            return agendamentoRepository.buscarAgendamentosAtivosPorTelefone(telLimpo, agora)
                    .stream()
                    .map(DetalhamentoAgendamentoDTO::new)
                    .toList();
        } catch (Exception e) {
            log.error("Erro na busca por telefone: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public List<String> listarHorariosDisponiveis(Long barbeiroId, Long servicoId, LocalDate data) {
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        LocalDateTime inicioDia = data.atTime(HORARIO_ABERTURA, 0);
        LocalDateTime fimDia = data.atTime(HORARIO_FECHAMENTO, 0);

        List<Agendamento> agendamentos = agendamentoRepository.findAgendaDoDia(barbeiroId, inicioDia, fimDia);

        List<String> horariosLivres = new ArrayList<>();
        LocalDateTime slotAtual = inicioDia;

        while (!slotAtual.plusMinutes(servico.getDuracaoEmMinutos()).isAfter(fimDia)) {
            if (isHorarioLivre(slotAtual, slotAtual.plusMinutes(servico.getDuracaoEmMinutos()), agendamentos)) {
                horariosLivres.add(slotAtual.toLocalTime().toString());
            }
            slotAtual = slotAtual.plusMinutes(INTERVALO_AGENDA_MINUTOS);
        }
        return horariosLivres;
    }

    // --- FINANCEIRO E ADMIN ---

    @Transactional
    public void bloquearHorario(String emailBarbeiro, BloqueioDTO dados) {
        if (!dados.isHorarioValido()) throw new RegraDeNegocioException("Intervalo de horário inválido.");

        Barbeiro barbeiro = barbeiroRepository.findByEmail(emailBarbeiro)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));

        Agendamento bloqueio = new Agendamento();
        bloqueio.setBarbeiro(barbeiro);
        bloqueio.setDataHoraInicio(dados.dataHoraInicio());
        bloqueio.setDataHoraFim(dados.dataHoraFim());
        bloqueio.setObservacao("🔒 BLOQUEIO: " + (dados.motivo() != null ? dados.motivo() : "Manual"));
        bloqueio.setStatus(StatusAgendamento.BLOQUEADO);

        // Zera valores financeiros para bloqueios
        bloqueio.setValorCobrado(BigDecimal.ZERO);
        bloqueio.setValorTotal(BigDecimal.ZERO);
        bloqueio.setValorBarbeiro(BigDecimal.ZERO);
        bloqueio.setValorCasa(BigDecimal.ZERO);

        agendamentoRepository.save(bloqueio);
    }

    @Transactional(readOnly = true)
    public ResumoFinanceiroDTO gerarRelatorioFinanceiro(String emailDono, LocalDate inicio, LocalDate fim) {
        LocalDate start = (inicio != null) ? inicio : LocalDate.now(TIMEZONE_BRASIL).minusDays(30);
        LocalDate end = (fim != null) ? fim : LocalDate.now(TIMEZONE_BRASIL);

        List<Agendamento> agendamentos = agendamentoRepository.buscarFinanceiroPorDono(
                emailDono, start.atStartOfDay(), end.atTime(LocalTime.MAX), StatusAgendamento.CONCLUIDO);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal casa = BigDecimal.ZERO;
        BigDecimal repasse = BigDecimal.ZERO;

        for (Agendamento a : agendamentos) {
            total = total.add(a.getValorTotal() != null ? a.getValorTotal() : BigDecimal.ZERO);
            repasse = repasse.add(a.getValorBarbeiro() != null ? a.getValorBarbeiro() : BigDecimal.ZERO);
            casa = casa.add(a.getValorCasa() != null ? a.getValorCasa() : BigDecimal.ZERO);
        }

        return new ResumoFinanceiroDTO(total.doubleValue(), casa.doubleValue(), repasse.doubleValue(), agendamentos.size());
    }

    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarTodosDoDono(String emailDono) {
        return agendamentoRepository.findAllByDonoEmail(emailDono).stream()
                .map(DetalhamentoAgendamentoDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarMeusAgendamentos(String emailBarbeiro) {
        return agendamentoRepository.findByBarbeiroEmailOrderByDataHoraInicioDesc(emailBarbeiro).stream()
                .map(DetalhamentoAgendamentoDTO::new).toList();
    }

    // --- AUXILIARES PRIVADOS ---

    private void calcularDivisaoFinanceira(Agendamento agendamento, Barbeiro barbeiro) {
        BigDecimal preco = agendamento.getValorCobrado();
        BigDecimal comissao = barbeiro.getComissaoPorcentagem() != null ? barbeiro.getComissaoPorcentagem() : new BigDecimal("50.0");
        BigDecimal valorBarbeiro = preco.multiply(comissao).divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);

        agendamento.setValorTotal(preco);
        agendamento.setValorBarbeiro(valorBarbeiro);
        agendamento.setValorCasa(preco.subtract(valorBarbeiro));
    }

    private boolean isHorarioLivre(LocalDateTime slotInicio, LocalDateTime slotFim, List<Agendamento> agendamentos) {
        return agendamentos.stream().noneMatch(ag -> slotInicio.isBefore(ag.getDataHoraFim()) && slotFim.isAfter(ag.getDataHoraInicio()));
    }

    private void validarHorarioFuncionamento(LocalDateTime dataInicio) {
        int hora = dataInicio.getHour();
        if (hora < HORARIO_ABERTURA || hora > HORARIO_FECHAMENTO) {
            throw new RegraDeNegocioException("Horário fora do funcionamento (06h - 23h).");
        }
    }
    // Adicione ou corrija este método no seu AgendamentoService
    public List<String> consultarDisponibilidade(Long barbeiroId, LocalDate data, Long servicoId) {
        log.info("Consultando disponibilidade: Barbeiro {}, Data {}, Serviço {}", barbeiroId, data, servicoId);
        return listarHorariosDisponiveis(barbeiroId, servicoId, data);
    }
}