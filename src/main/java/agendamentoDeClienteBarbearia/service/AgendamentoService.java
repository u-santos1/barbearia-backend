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
    private final NotificacaoService notificacaoService;

    private static final int HORARIO_ABERTURA = 6;
    private static final int HORARIO_FECHAMENTO = 23;
    private static final int INTERVALO_AGENDA_MINUTOS = 30;
    private static final ZoneId TIMEZONE_BRASIL = ZoneId.of("America/Sao_Paulo");

    // --- 1. CORE: AGENDAR ---
    @Transactional
    public DetalhamentoAgendamentoDTO agendar(AgendamentoDTO dados) {
        log.info("Iniciando agendamento para Cliente ID: {}", dados.clienteId());

        // --- VALIDAÇÕES (MANTIDAS) ---
        Barbeiro barbeiro = barbeiroRepository.findById(dados.barbeiroId())
                .orElseThrow(() -> new RegraDeNegocioException("Barbeiro não encontrado"));

        if (!Boolean.TRUE.equals(barbeiro.getAtivo())) {
            throw new RegraDeNegocioException("Este barbeiro não está atendendo no momento.");
        }

        Cliente cliente = clienteRepository.findById(dados.clienteId())
                .orElseThrow(() -> new RegraDeNegocioException("Cliente não encontrado."));

        Servico servico = servicoRepository.findById(dados.servicoId())
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        LocalDateTime dataInicio = dados.dataHoraInicio();

        // Dica: Valide se dataInicio é nula antes de checar horario
        if (dataInicio == null) throw new RegraDeNegocioException("Data é obrigatória");

        // Validação de passado (Ajuste para ZoneId se necessário)
        if (dataInicio.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException("Não é possível agendar em datas passadas.");
        }

        // Chama seu validador de horário comercial
        validarHorarioFuncionamento(dataInicio);

        LocalDateTime dataFim = dataInicio.plusMinutes(servico.getDuracaoEmMinutos());

        // Validação de Conflito (Check-Then-Act)
        if (agendamentoRepository.existeConflitoDeHorario(barbeiro.getId(), dataInicio, dataFim)) {
            throw new RegraDeNegocioException("Este horário já está ocupado.");
        }

        // --- CRIAÇÃO ---
        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServico(servico);
        agendamento.setDataHoraInicio(dataInicio);
        agendamento.setDataHoraFim(dataFim);
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setValorCobrado(servico.getPreco());

        calcularDivisaoFinanceira(agendamento, barbeiro);

        // --- PERSISTÊNCIA ---
        // Salvamos primeiro para garantir que o ID foi gerado e não deu erro de banco
        Agendamento agendamentoSalvo = agendamentoRepository.save(agendamento);

        // 👇 2. A MÁGICA ACONTECE AQUI
        // Chamamos o serviço @Async. Ele vai rodar em paralelo e não vai travar o retorno.
        notificacaoService.notificarBarbeiro(barbeiro, agendamentoSalvo);

        log.info("Agendamento realizado com sucesso! ID: {}", agendamentoSalvo.getId());

        return new DetalhamentoAgendamentoDTO(agendamentoSalvo);
    }

    // --- 2. STATUS E CANCELAMENTO (FIX ERRO 500) ---
    @Transactional
    public void cancelar(Long id) {
        log.info("Processando cancelamento para ID: {}", id);
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        if (agendamento.getCliente() == null) {
            agendamentoRepository.delete(agendamento);
            return;
        }

        // Mapeado para "CANCELADO" (curto) para evitar erro de VARCHAR(20) no Postgres
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamentoRepository.save(agendamento);
    }

    @Transactional
    public void cancelarPeloBarbeiro(Long id) {
        this.cancelar(id);
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

    // --- 3. BUSCAS E DISPONIBILIDADE ---
    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> buscarPorTelefoneCliente(String telefone) {
        try {
            String telLimpo = telefone.replaceAll("\\D", "");
            LocalDateTime agora = LocalDateTime.now(TIMEZONE_BRASIL);
            return agendamentoRepository.buscarAgendamentosAtivosPorTelefone(telLimpo, agora)
                    .stream().map(DetalhamentoAgendamentoDTO::new).toList();
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

    public List<String> consultarDisponibilidade(Long barbeiroId, LocalDate data, Long servicoId) {
        return listarHorariosDisponiveis(barbeiroId, servicoId, data);
    }

    // --- 4. LISTAGENS SAAS (MAPEADAS DO REPOSITORY) ---
    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarTodosDoDono(String emailDono) {
        return agendamentoRepository.findAllByDonoEmail(emailDono)
                .stream().map(DetalhamentoAgendamentoDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarMeusAgendamentos(String emailBarbeiro) {
        return agendamentoRepository.findByBarbeiroEmailOrderByDataHoraInicioDesc(emailBarbeiro)
                .stream().map(DetalhamentoAgendamentoDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarPorCliente(Long clienteId) {
        return agendamentoRepository.findByClienteIdOrderByDataHoraInicioDesc(clienteId)
                .stream().map(DetalhamentoAgendamentoDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarPorBarbeiroId(Long barbeiroId) {
        return agendamentoRepository.findByBarbeiroIdOrderByDataHoraInicioDesc(barbeiroId)
                .stream().map(DetalhamentoAgendamentoDTO::new).toList();
    }

    // --- 5. FINANCEIRO ---
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

    // --- 6. BLOQUEIOS ---
    @Transactional
    public void bloquearHorario(String emailBarbeiro, BloqueioDTO dados) {
        if (!dados.isHorarioValido()) throw new RegraDeNegocioException("Intervalo inválido.");

        Barbeiro barbeiro = barbeiroRepository.findByEmail(emailBarbeiro)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));

        if (agendamentoRepository.existeConflitoDeHorario(emailBarbeiro, dados.dataHoraInicio(), dados.dataHoraFim())) {
            throw new RegraDeNegocioException("Já existe um agendamento ou bloqueio neste horário.");
        }

        Agendamento bloqueio = new Agendamento();
        bloqueio.setBarbeiro(barbeiro);
        bloqueio.setDataHoraInicio(dados.dataHoraInicio());
        bloqueio.setDataHoraFim(dados.dataHoraFim());
        bloqueio.setStatus(StatusAgendamento.BLOQUEADO);
        bloqueio.setObservacao("🔒 BLOQUEIO: " + (dados.motivo() != null ? dados.motivo() : "Manual"));
        bloqueio.setValorCobrado(BigDecimal.ZERO);
        bloqueio.setValorTotal(BigDecimal.ZERO);
        bloqueio.setValorBarbeiro(BigDecimal.ZERO);
        bloqueio.setValorCasa(BigDecimal.ZERO);

        agendamentoRepository.save(bloqueio);
    }

    // --- AUXILIARES ---
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
    // --- 1. Implementação: findByBarbeiroIdAndDataHoraInicioBetween ---
    /**
     * Busca agendamentos de um barbeiro em um intervalo específico.
     * Útil para validações de agenda ou relatórios por ID.
     */
    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarPorBarbeiroEPeriodo(Long barbeiroId, LocalDateTime inicio, LocalDateTime fim) {
        return agendamentoRepository.findByBarbeiroIdAndDataHoraInicioBetween(barbeiroId, inicio, fim)
                .stream()
                .map(DetalhamentoAgendamentoDTO::new)
                .toList();
    }

    // --- 2. Implementação: findAllByBarbeiroDonoId ---
    /**
     * Lista todos os agendamentos vinculados a um Dono através do ID.
     * Diferente do método por e-mail, este é usado quando o sistema já possui o ID do Dono.
     */
    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarTodosPorDonoId(Long donoId) {
        return agendamentoRepository.findAllByBarbeiroDonoId(donoId)
                .stream()
                .map(DetalhamentoAgendamentoDTO::new)
                .toList();
    }
}