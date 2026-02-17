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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final NotificacaoService notificacaoService;
    private final ExpedienteRepository expedienteRepository;

    // Constantes de negócio
    private static final int INTERVALO_AGENDA_MINUTOS = 30;
    private static final ZoneId TIMEZONE_BRASIL = ZoneId.of("America/Sao_Paulo");

    // --- 1. CORE: AGENDAR ---
    @Transactional
    public DetalhamentoAgendamentoDTO agendar(AgendamentoDTO dados) {
        log.info("Iniciando agendamento para Cliente ID: {}", dados.clienteId());

        // --- VALIDAÇÕES ---
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

        if (dataInicio == null) throw new RegraDeNegocioException("Data é obrigatória");

        if (dataInicio.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException("Não é possível agendar em datas passadas.");
        }

        // Valida se o horário solicitado está dentro do expediente do barbeiro
        validarHorarioFuncionamento(barbeiro.getId(), dataInicio, servico.getDuracaoEmMinutos());

        LocalDateTime dataFim = dataInicio.plusMinutes(servico.getDuracaoEmMinutos());

        // Validação de Conflito
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

        Agendamento agendamentoSalvo = agendamentoRepository.save(agendamento);

        notificacaoService.notificarBarbeiro(barbeiro, agendamentoSalvo);

        log.info("Agendamento realizado com sucesso! ID: {}", agendamentoSalvo.getId());

        return new DetalhamentoAgendamentoDTO(agendamentoSalvo);
    }

    // --- 2. STATUS E CANCELAMENTO ---
    @Transactional
    public void cancelar(Long id) {
        log.info("Processando cancelamento para ID: {}", id);
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        if (agendamento.getCliente() == null) {
            agendamentoRepository.delete(agendamento);
            return;
        }

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

    // --- 3. BUSCAS E DISPONIBILIDADE (REFATORADO) ---

    /**
     * Ponto de entrada para o Controller.
     * Retorna lista de Strings formatadas (HH:mm) para o Frontend.
     */
    public List<String> consultarDisponibilidade(Long barbeiroId, LocalDate data, Long servicoId) {
        return listarHorariosDisponiveis(barbeiroId, servicoId, data);
    }

    /**
     * Lógica central de disponibilidade baseada no Expediente (Banco de Dados).
     */
    @Transactional(readOnly = true)
    public List<String> listarHorariosDisponiveis(Long barbeiroId, Long servicoId, LocalDate data) {
        // 1. Validar Serviço
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        int duracaoMinutos = servico.getDuracaoEmMinutos();

        // 2. Buscar Configuração de Expediente
        DayOfWeek diaSolicitado = data.getDayOfWeek();
        Optional<Expediente> expedienteOpt = expedienteRepository.findByBarbeiroIdAndDiaSemana(barbeiroId, diaSolicitado);

        // Se não tem configuração ou está marcado como folga, retorna lista vazia
        if (expedienteOpt.isEmpty() || !expedienteOpt.get().isTrabalha()) {
            return new ArrayList<>();
        }

        Expediente expediente = expedienteOpt.get();
        LocalTime inicioExpediente = expediente.getAbertura();
        LocalTime fimExpediente = expediente.getFechamento();

        List<String> horariosLivres = new ArrayList<>();
        LocalTime slotAtual = inicioExpediente;

        // 3. Loop de Slots (Intervalo padrão de 30min para visualização)
        // Verifica se o serviço inteiro cabe antes do fechamento
        while (!slotAtual.plusMinutes(duracaoMinutos).isAfter(fimExpediente)) {

            LocalDateTime dataHoraInicio = data.atTime(slotAtual);
            LocalDateTime dataHoraFim = dataHoraInicio.plusMinutes(duracaoMinutos);

            // 4. Verifica colisão com Agendamentos ou Bloqueios existentes
            boolean existeConflito = agendamentoRepository.existeConflitoDeHorario(
                    barbeiroId,
                    dataHoraInicio,
                    dataHoraFim
            );

            if (!existeConflito) {
                horariosLivres.add(slotAtual.toString());
            }

            // Avança o slot
            slotAtual = slotAtual.plusMinutes(INTERVALO_AGENDA_MINUTOS);
        }

        return horariosLivres;
    }

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

    // --- 4. LISTAGENS SAAS ---
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

    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarPorBarbeiroEPeriodo(Long barbeiroId, LocalDateTime inicio, LocalDateTime fim) {
        return agendamentoRepository.findByBarbeiroIdAndDataHoraInicioBetween(barbeiroId, inicio, fim)
                .stream()
                .map(DetalhamentoAgendamentoDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarTodosPorDonoId(Long donoId) {
        return agendamentoRepository.findAllByBarbeiroDonoId(donoId)
                .stream()
                .map(DetalhamentoAgendamentoDTO::new)
                .toList();
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

    // --- AUXILIARES E VALIDAÇÕES INTERNAS ---

    private void calcularDivisaoFinanceira(Agendamento agendamento, Barbeiro barbeiro) {
        BigDecimal preco = agendamento.getValorCobrado();
        BigDecimal comissao = barbeiro.getComissaoPorcentagem() != null ? barbeiro.getComissaoPorcentagem() : new BigDecimal("50.0");
        BigDecimal valorBarbeiro = preco.multiply(comissao).divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
        agendamento.setValorTotal(preco);
        agendamento.setValorBarbeiro(valorBarbeiro);
        agendamento.setValorCasa(preco.subtract(valorBarbeiro));
    }

    /**
     * Valida se o horário escolhido respeita o Expediente do banco de dados.
     * Substitui a antiga validação de horário fixo (06-23h).
     */
    private void validarHorarioFuncionamento(Long barbeiroId, LocalDateTime dataInicio, int duracaoMinutos) {
        DayOfWeek dia = dataInicio.getDayOfWeek();
        LocalTime horaAgendamento = dataInicio.toLocalTime();
        LocalTime horaFimAgendamento = horaAgendamento.plusMinutes(duracaoMinutos);

        // Busca configuração
        Expediente expediente = expedienteRepository.findByBarbeiroIdAndDiaSemana(barbeiroId, dia)
                .orElseThrow(() -> new RegraDeNegocioException("O profissional não possui agenda configurada para " + dia));

        if (!expediente.isTrabalha()) {
            throw new RegraDeNegocioException("O profissional não trabalha neste dia (" + dia + ").");
        }

        if (horaAgendamento.isBefore(expediente.getAbertura()) || horaFimAgendamento.isAfter(expediente.getFechamento())) {
            throw new RegraDeNegocioException(
                    String.format("Horário indisponível. Funcionamento hoje: %s às %s",
                            expediente.getAbertura(), expediente.getFechamento())
            );
        }
    }
}