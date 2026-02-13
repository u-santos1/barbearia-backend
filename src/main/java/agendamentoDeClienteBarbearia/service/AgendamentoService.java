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

        // ✅ Produção: Sincronização de Fuso para evitar agendamentos retroativos
        LocalDateTime agoraBrasil = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        if (dataInicio.isBefore(agoraBrasil)) {
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
        agendamentoRepository.save(agendamento);

        return new DetalhamentoAgendamentoDTO(agendamento);
    }

    // --- MÉTODOS DE STATUS E CANCELAMENTO ---

    @Transactional
    public void cancelar(Long id) {
        log.info("Cancelamento solicitado para ID: {}", id);
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        // ✅ Blindagem: Trata bloqueios administrativos sem cliente para evitar NPE
        if (agendamento.getCliente() == null) {
            agendamentoRepository.delete(agendamento);
            return;
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO_PELO_CLIENTE);
        agendamentoRepository.save(agendamento);
    }

    @Transactional
    public void cancelarPeloBarbeiro(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Agendamento não encontrado"));

        if (agendamento.getCliente() == null) {
            agendamentoRepository.delete(agendamento);
            return;
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO_PELO_BARBEIRO);
        agendamentoRepository.save(agendamento);
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

    /**
     * ✅ Produção: O uso de readOnly = true mantém a Hibernate Session aberta para
     * evitar o erro "could not initialize proxy - no Session".
     */
    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> buscarPorTelefoneCliente(String telefone) {
        try {
            LocalDateTime agoraBrasil = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
            log.info("Buscando para telefone: {} | Ref: {}", telefone, agoraBrasil);

            // IMPORTANTE: Use JOIN FETCH no seu Repository para carregar Barbeiro e Servico
            var agendamentos = agendamentoRepository.buscarAgendamentosAtivosPorTelefone(telefone, agoraBrasil);

            return agendamentos.stream()
                    .map(a -> {
                        // Proteção contra dados incompletos ou proxies não hidratados
                        if (a.getBarbeiro() == null || a.getServico() == null) return null;
                        return new DetalhamentoAgendamentoDTO(a);
                    })
                    .filter(dto -> dto != null)
                    .toList();
        } catch (Exception e) {
            log.error("Erro na busca por telefone: ", e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public List<String> listarHorariosDisponiveis(Long barbeiroId, Long servicoId, LocalDate data) {
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        int duracaoMinutos = servico.getDuracaoEmMinutos();
        LocalDateTime inicioDia = data.atTime(HORARIO_ABERTURA, 0);
        LocalDateTime fimDia = data.atTime(HORARIO_FECHAMENTO, 0);

        List<Agendamento> agendamentos = agendamentoRepository.findAgendaDoDia(barbeiroId, inicioDia, fimDia);

        List<String> horariosLivres = new ArrayList<>();
        LocalDateTime slotAtual = inicioDia;

        while (!slotAtual.plusMinutes(duracaoMinutos).isAfter(fimDia)) {
            LocalDateTime slotFim = slotAtual.plusMinutes(duracaoMinutos);
            if (isHorarioLivre(slotAtual, slotFim, agendamentos)) {
                horariosLivres.add(slotAtual.toLocalTime().toString());
            }
            slotAtual = slotAtual.plusMinutes(INTERVALO_AGENDA_MINUTOS);
        }
        return horariosLivres;
    }

    public List<String> consultarDisponibilidade(Long barbeiroId, LocalDate data, Long servicoId) {
        return listarHorariosDisponiveis(barbeiroId, servicoId, data);
    }

    // --- FINANCEIRO E ADMIN ---

    @Transactional
    public void bloquearHorario(String emailBarbeiro, BloqueioDTO dados) {
        if (!dados.isHorarioValido()) throw new IllegalArgumentException("Horário inválido.");

        Barbeiro barbeiro = barbeiroRepository.findByEmail(emailBarbeiro)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));

        Agendamento bloqueio = new Agendamento();
        bloqueio.setBarbeiro(barbeiro);
        bloqueio.setDataHoraInicio(dados.dataHoraInicio());
        bloqueio.setDataHoraFim(dados.dataHoraFim());
        bloqueio.setObservacao("🔒 BLOQUEIO: " + (dados.motivo() != null ? dados.motivo() : "Manual"));
        bloqueio.setStatus(StatusAgendamento.BLOQUEADO);
        bloqueio.setValorCobrado(BigDecimal.ZERO);
        bloqueio.setValorTotal(BigDecimal.ZERO);
        bloqueio.setValorBarbeiro(BigDecimal.ZERO);
        bloqueio.setValorCasa(BigDecimal.ZERO);

        agendamentoRepository.save(bloqueio);
    }

    // --- AUXILIARES ---

    private void calcularDivisaoFinanceira(Agendamento agendamento, Barbeiro barbeiro) {
        BigDecimal preco = agendamento.getValorCobrado();
        BigDecimal comissao = barbeiro.getComissaoPorcentagem() != null
                ? barbeiro.getComissaoPorcentagem() : new BigDecimal("50.0");

        BigDecimal valorBarbeiro = preco.multiply(comissao)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);

        agendamento.setValorTotal(preco);
        agendamento.setValorBarbeiro(valorBarbeiro);
        agendamento.setValorCasa(preco.subtract(valorBarbeiro));
    }

    private boolean isHorarioLivre(LocalDateTime slotInicio, LocalDateTime slotFim, List<Agendamento> agendamentos) {
        for (Agendamento ag : agendamentos) {
            if (slotInicio.isBefore(ag.getDataHoraFim()) && slotFim.isAfter(ag.getDataHoraInicio())) return false;
        }
        return true;
    }

    private void validarHorarioFuncionamento(LocalDateTime dataInicio) {
        int hora = dataInicio.getHour();
        if (hora < HORARIO_ABERTURA || hora > HORARIO_FECHAMENTO) {
            throw new RegraDeNegocioException("Horário fora do funcionamento (06h - 23h).");
        }
    }

    // --- FINANCEIRO (Corrigido) ---
    @Transactional(readOnly = true)
    public ResumoFinanceiroDTO gerarRelatorioFinanceiro(String emailDono, LocalDate inicio, LocalDate fim) {
        // 1. Define datas padrão se vierem nulas
        LocalDate dataInicio = (inicio != null) ? inicio : LocalDate.now().minusDays(30);
        LocalDate dataFim = (fim != null) ? fim : LocalDate.now();

        // 2. Busca no banco filtrando pelo DONO do barbeiro
        List<Agendamento> agendamentos = agendamentoRepository.buscarFinanceiroPorDono(
                emailDono,
                dataInicio.atStartOfDay(),
                dataFim.atTime(LocalTime.MAX),
                StatusAgendamento.CONCLUIDO
        );

        // 3. Calcula totais
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

    // --- LISTAR TODOS DO DONO (SaaS) ---
    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarTodosDoDono(String emailDono) {
        // Busca todos os agendamentos de todos os barbeiros que pertencem a este dono
        return agendamentoRepository.findAllByDonoEmail(emailDono).stream()
                .map(DetalhamentoAgendamentoDTO::new)
                .toList();
    }

    // --- LISTAR MEUS (Barbeiro Logado) ---
    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarMeusAgendamentos(String emailBarbeiro) {
        // Busca apenas agendamentos deste barbeiro específico
        return agendamentoRepository.findByBarbeiroEmailOrderByDataHoraInicioDesc(emailBarbeiro).stream()
                .map(DetalhamentoAgendamentoDTO::new)
                .toList();
    }
}