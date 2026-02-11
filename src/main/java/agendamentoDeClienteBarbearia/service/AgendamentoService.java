package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.StatusAgendamento;
import agendamentoDeClienteBarbearia.dtos.AgendamentoDTO;
import agendamentoDeClienteBarbearia.dtos.BloqueioDTO;
import agendamentoDeClienteBarbearia.dtos.ResumoFinanceiroDTO;
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
    private final AgendamentoRepository repository;



    // Horário flexível (06h às 23h) para não travar o dono
    private static final int HORARIO_ABERTURA = 6;
    private static final int HORARIO_FECHAMENTO = 23;
    private static final int INTERVALO_AGENDA_MINUTOS = 30;

    // ✅ AJUSTE 1: Nome deve ser 'agendar' para bater com o Controller
    @Transactional
    public DetalhamentoAgendamentoDTO agendar(AgendamentoDTO dados) {
        log.info("Iniciando agendamento para Cliente ID: {}", dados.clienteId());

        // 1. Validar Barbeiro
        Barbeiro barbeiro = barbeiroRepository.findById(dados.barbeiroId())
                .orElseThrow(() -> new RegraDeNegocioException("Barbeiro não encontrado"));

        if (!barbeiro.getAtivo()) {
            throw new RegraDeNegocioException("Este barbeiro não está atendendo no momento.");
        }

        // 2. Validar Cliente
        Cliente cliente = clienteRepository.findById(dados.clienteId())
                .orElseThrow(() -> new RegraDeNegocioException("Cliente não encontrado. Realize o cadastro antes."));

        // 3. Validar Serviço
        Servico servico = servicoRepository.findById(dados.servicoId())
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        // 4. Validar Data/Hora
        LocalDateTime dataInicio = dados.dataHoraInicio();

        validarHorarioFuncionamento(dataInicio);

        if (dataInicio.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException("Não é possível agendar em datas passadas.");
        }

        // 5. Calcular Fim e Checar Conflito
        LocalDateTime dataFim = dataInicio.plusMinutes(servico.getDuracaoEmMinutos());

        if (agendamentoRepository.existeConflitoDeHorario(barbeiro.getId(), dataInicio, dataFim)) {
            throw new RegraDeNegocioException("Este horário já está ocupado.");
        }

        // 6. Montar Agendamento
        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServico(servico);
        agendamento.setDataHoraInicio(dataInicio);
        agendamento.setDataHoraFim(dataFim);
        agendamento.setStatus(StatusAgendamento.AGENDADO);

        // Se o seu DTO AgendamentoDTO não tem o campo observacao, mantenha null.
        // Se você adicionou 'String observacao' no DTO, mude para: dados.observacao()
        agendamento.setObservacao(null);

        // Financeiro
        agendamento.setValorCobrado(servico.getPreco());
        calcularDivisaoFinanceira(agendamento, barbeiro);

        // 7. Salvar e Notificar
        agendamentoRepository.save(agendamento);
        enviarNotificacaoSegura(agendamento);

        return new DetalhamentoAgendamentoDTO(agendamento);
    }

    // --- Métodos de Status ---

    @Transactional
    public void cancelar(Long id) { alterarStatus(id, StatusAgendamento.CANCELADO_PELO_CLIENTE); }

    // ✅ AJUSTE 2: Adicionado método que faltava para o Controller
    @Transactional
    public void cancelarPeloBarbeiro(Long id) {
        alterarStatus(id, StatusAgendamento.CANCELADO_PELO_BARBEIRO);
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

    // --- Disponibilidade ---

    @Transactional(readOnly = true)
    public List<String> listarHorariosDisponiveis(Long barbeiroId, Long servicoId, LocalDate data) {
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        int duracaoMinutos = servico.getDuracaoEmMinutos();
        LocalDateTime inicioDia = data.atTime(HORARIO_ABERTURA, 0);
        LocalDateTime fimDia = data.atTime(HORARIO_FECHAMENTO, 0);

        List<Agendamento> agendamentos = agendamentoRepository.findAgendaDoDia(barbeiroId, inicioDia, fimDia);

        // Se ainda não tiver bloqueios, usa lista vazia
        List<Bloqueio> bloqueios = new ArrayList<>();

        List<String> horariosLivres = new ArrayList<>();
        LocalDateTime slotAtual = inicioDia;

        while (!slotAtual.plusMinutes(duracaoMinutos).isAfter(fimDia)) {
            LocalDateTime slotFim = slotAtual.plusMinutes(duracaoMinutos);
            if (isHorarioLivre(slotAtual, slotFim, agendamentos, bloqueios)) {
                horariosLivres.add(slotAtual.toLocalTime().toString());
            }
            slotAtual = slotAtual.plusMinutes(INTERVALO_AGENDA_MINUTOS);
        }
        return horariosLivres;
    }

    // Método ponte para o Controller
    public List<String> consultarDisponibilidade(Long barbeiroId, LocalDate data, Long servicoId) {
        return listarHorariosDisponiveis(barbeiroId, servicoId, data);
    }

    // --- Listagens ---

    @Transactional(readOnly = true)
    public ResumoFinanceiroDTO gerarRelatorioFinanceiro(LocalDate inicio, LocalDate fim) {
        if (inicio == null) inicio = LocalDate.now().minusDays(30);
        if (fim == null) fim = LocalDate.now();

        List<Agendamento> agendamentos = agendamentoRepository
                .findByDataHoraInicioBetweenAndStatus(inicio.atStartOfDay(), fim.atTime(LocalTime.MAX), StatusAgendamento.CONCLUIDO);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal casa = BigDecimal.ZERO;
        BigDecimal repasse = BigDecimal.ZERO;

        for (Agendamento a : agendamentos) {
            if (a.getValorTotal() == null) calcularDivisaoFinanceira(a, a.getBarbeiro());
            total = total.add(a.getValorTotal());
            repasse = repasse.add(a.getValorBarbeiro());
            casa = casa.add(a.getValorCasa());
        }
        return new ResumoFinanceiroDTO(total.doubleValue(), casa.doubleValue(), repasse.doubleValue(), agendamentos.size());
    }

    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarTodosDoDono(String emailLogado) {
        return agendamentoRepository.findAll().stream()
                .map(DetalhamentoAgendamentoDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarPorCliente(Long clienteId) {
        return agendamentoRepository.findByClienteIdOrderByDataHoraInicioDesc(clienteId).stream()
                .map(DetalhamentoAgendamentoDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarMeusAgendamentos(String emailBarbeiro) {
        try {
            return agendamentoRepository.findAll().stream()
                    .filter(a -> a.getBarbeiro().getEmail().equals(emailBarbeiro))
                    .sorted((a1, a2) -> a2.getDataHoraInicio().compareTo(a1.getDataHoraInicio()))
                    .map(DetalhamentoAgendamentoDTO::new)
                    .toList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // --- Auxiliares ---

    private void validarHorarioFuncionamento(LocalDateTime dataInicio) {
        // Regra do Dono: Aceita qualquer dia, entre 06h e 23h
        int hora = dataInicio.getHour();
        if (hora < HORARIO_ABERTURA || hora > HORARIO_FECHAMENTO) {
            throw new RegraDeNegocioException("Horário fora do funcionamento (06h - 23h).");
        }
    }

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

    private boolean isHorarioLivre(LocalDateTime slotInicio, LocalDateTime slotFim,
                                   List<Agendamento> agendamentos, List<Bloqueio> bloqueios) {
        for (Agendamento ag : agendamentos) {
            if (slotInicio.isBefore(ag.getDataHoraFim()) && slotFim.isAfter(ag.getDataHoraInicio())) return false;
        }
        return true;
    }

    private void enviarNotificacaoSegura(Agendamento agendamento) {
        try {
            // notificacaoService.notificarBarbeiro(agendamento.getBarbeiro(), agendamento);
        } catch (Exception e) {
            log.error("Erro na notificação: {}", e.getMessage());
        }
    }
    @Transactional
    public void bloquearHorario(String emailBarbeiro, BloqueioDTO dados) {
        // 1. Validação de Coerência Temporal
        if (!dados.isHorarioValido()) {
            throw new IllegalArgumentException("A hora final deve ser posterior à hora inicial.");
        }

        // 2. Busca o Profissional
        Barbeiro barbeiro = barbeiroRepository.findByEmail(emailBarbeiro)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));

        // 3. DEFESA CRÍTICA: Verifica colisão de horário (Overlap)
        boolean horarioOcupado = repository.existeConflitoDeHorario(
                emailBarbeiro,
                dados.dataHoraInicio(),
                dados.dataHoraFim()
        );

        if (horarioOcupado) {
            throw new IllegalStateException("Já existe um agendamento ou bloqueio neste intervalo de tempo.");
        }

        // 4. Montagem da Entidade de Bloqueio
        Agendamento bloqueio = new Agendamento();
        bloqueio.setBarbeiro(barbeiro);
        bloqueio.setDataHoraInicio(dados.dataHoraInicio());
        bloqueio.setDataHoraFim(dados.dataHoraFim());

        // Identificação visual no sistema
        bloqueio.setObservacao("🔒 BLOQUEIO: " + (dados.motivo() != null ? dados.motivo() : "Manual"));

        // Tratamento de Status (Crie este ENUM se não tiver, ou use uma String constante)
        bloqueio.setStatus(StatusAgendamento.BLOQUEADO); // Ou "BLOQUEADO" se for String

        // 5. Tratamento Financeiro (Zeragem)
        // Isso impede que o bloqueio apareça como "R$ 0,00" pendente no caixa
        bloqueio.setValorCobrado(BigDecimal.ZERO);
        bloqueio.setValorTotal(BigDecimal.ZERO);
        bloqueio.setValorBarbeiro(BigDecimal.ZERO);
        bloqueio.setValorCasa(BigDecimal.ZERO);

        // 6. Tratamento de Nulos (Para funcionar com sua Entidade alterada)
        bloqueio.setCliente(null);
        bloqueio.setServico(null);

        repository.save(bloqueio);
    }
}