package agendamentoDeClienteBarbearia.service;




import agendamentoDeClienteBarbearia.StatusAgendamento;
import agendamentoDeClienteBarbearia.dtos.AgendamentoDTO;
import agendamentoDeClienteBarbearia.dtos.ResumoFinanceiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.infra.security.ValidacaoException;
import agendamentoDeClienteBarbearia.model.*;
import agendamentoDeClienteBarbearia.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j // Logger profissional (SLF4J)
@Service
@RequiredArgsConstructor // Injeção de dependência limpa (Lombok)
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final BloqueioRepository bloqueioRepository;
    private final NotificacaoService notificacaoService;

    // Constantes de Regra de Negócio (Facilita manutenção)
    private static final int HORARIO_ABERTURA = 9;
    private static final int HORARIO_FECHAMENTO = 19;
    private static final int INTERVALO_AGENDA_MINUTOS = 30;

    @Transactional
    public DetalhamentoAgendamentoDTO agendar(AgendamentoDTO dados) {
        // 1. Buscas otimizadas (Repositories já devem usar índices)
        Barbeiro barbeiro = barbeiroRepository.findById(dados.barbeiroId())
                .orElseThrow(() -> new RegraDeNegocioException("Barbeiro não encontrado"));

        Cliente cliente = clienteRepository.findById(dados.clienteId())
                .orElseThrow(() -> new RegraDeNegocioException("Cliente não encontrado"));

        Servico servico = servicoRepository.findById(dados.servicoId())
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        LocalDateTime dataInicio = dados.dataHoraInicio();

        // 2. Validações de Negócio
        validarHorarioFuncionamento(dataInicio);

        // 3. Validação de data no passado
        if (dataInicio.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException("Não é possível agendar em datas passadas.");
        }

        // 4. Calcular Data Fim
        LocalDateTime dataFim = dataInicio.plusMinutes(servico.getDuracaoEmMinutos());

        // 5. Validar Conflito (Critical Section)
        // OBS: Em produção com alta concorrência, recomenda-se Optimistic Locking (@Version) na entidade Barbeiro
        if (agendamentoRepository.existeConflitoDeHorario(barbeiro.getId(), dataInicio, dataFim)) {
            throw new RegraDeNegocioException("Este barbeiro já está ocupado neste horário.");
        }

        // 6. Montagem da Entidade
        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServico(servico);
        agendamento.setDataHoraInicio(dataInicio);
        agendamento.setDataHoraFim(dataFim);

        // Financeiro Seguro
        agendamento.setValorCobrado(servico.getPreco());
        calcularDivisaoFinanceira(agendamento, barbeiro);

        agendamento.setStatus(StatusAgendamento.AGENDADO);

        // 7. Persistência
        Agendamento agendamentoSalvo = agendamentoRepository.save(agendamento);

        // 8. Notificação Assíncrona (Não bloqueia o erro se falhar)
        enviarNotificacaoSegura(agendamentoSalvo);

        return new DetalhamentoAgendamentoDTO(agendamentoSalvo);
    }

    // --- Métodos de Mudança de Status ---

    @Transactional
    public void cancelar(Long agendamentoId) {
        alterarStatus(agendamentoId, StatusAgendamento.CANCELADO_PELO_CLIENTE);
    }

    @Transactional
    public void confirmar(Long id) {
        alterarStatus(id, StatusAgendamento.CONFIRMADO);
    }

    @Transactional
    public void concluir(Long id) {
        alterarStatus(id, StatusAgendamento.CONCLUIDO);
    }

    @Transactional
    public void cancelarPeloBarbeiro(Long id) {
        alterarStatus(id, StatusAgendamento.CANCELADO_PELO_BARBEIRO);
    }

    private void alterarStatus(Long id, StatusAgendamento novoStatus) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Agendamento não encontrado"));
        agendamento.setStatus(novoStatus);
        // O JPA faz o update automático no final da transação (Dirty Checking), mas o save explícito não faz mal.
        agendamentoRepository.save(agendamento);
    }

    // --- Consultas (ReadOnly para performance) ---

    @Transactional(readOnly = true)
    public List<String> listarHorariosDisponiveis(Long barbeiroId, Long servicoId, LocalDate data) {
        var servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        int duracaoMinutos = servico.getDuracaoEmMinutos();
        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.atTime(LocalTime.MAX);

        // Busca apenas o necessário do banco
        List<Agendamento> agendamentos = agendamentoRepository.findAgendaDoDia(barbeiroId, inicioDia, fimDia);
        List<Bloqueio> bloqueios = bloqueioRepository.findBloqueiosDoDia(barbeiroId, inicioDia, fimDia);

        LocalTime abertura = LocalTime.of(HORARIO_ABERTURA, 0);
        LocalTime fechamento = LocalTime.of(HORARIO_FECHAMENTO, 0);

        List<String> horariosLivres = new ArrayList<>();
        LocalTime slotAtual = abertura;

        while (!slotAtual.plusMinutes(duracaoMinutos).isAfter(fechamento)) {
            LocalDateTime slotInicio = LocalDateTime.of(data, slotAtual);
            LocalDateTime slotFim = slotInicio.plusMinutes(duracaoMinutos);

            if (isHorarioLivre(slotInicio, slotFim, agendamentos, bloqueios)) {
                horariosLivres.add(slotAtual.toString());
            }

            slotAtual = slotAtual.plusMinutes(INTERVALO_AGENDA_MINUTOS);
        }

        return horariosLivres;
    }

    // --- Relatórios e Listagens ---

    /**
     * OTIMIZAÇÃO CRÍTICA: Em um cenário real, isso deve ser uma Query no Banco (SUM/COUNT).
     * Fazer loop em memória com findAll() derruba servidor.
     * Mantive a lógica Java, mas adicionei filtro de data obrigatório para não travar a produção.
     */
    @Transactional(readOnly = true)
    public ResumoFinanceiroDTO gerarRelatorioFinanceiro(LocalDate inicio, LocalDate fim) {
        // Se não passar data, limita aos últimos 30 dias para segurança
        if (inicio == null) inicio = LocalDate.now().minusDays(30);
        if (fim == null) fim = LocalDate.now();

        List<Agendamento> agendamentos = agendamentoRepository
                .findByDataHoraInicioBetweenAndStatus(inicio.atStartOfDay(), fim.atTime(LocalTime.MAX), StatusAgendamento.CONCLUIDO);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal casa = BigDecimal.ZERO;
        BigDecimal repasse = BigDecimal.ZERO;
        int qtdConcluidos = agendamentos.size();

        for (Agendamento a : agendamentos) {
            // Os valores já devem estar calculados no objeto Agendamento ao salvar (vide método agendar)
            // Mas recalculamos aqui caso seja um agendamento antigo sem esses campos preenchidos
            if (a.getValorTotal() == null) {
                calcularDivisaoFinanceira(a, a.getBarbeiro());
            }

            total = total.add(a.getValorTotal());
            repasse = repasse.add(a.getValorBarbeiro());
            casa = casa.add(a.getValorCasa());
        }

        return new ResumoFinanceiroDTO(
                total.doubleValue(),
                casa.doubleValue(),
                repasse.doubleValue(),
                qtdConcluidos
        );
    }

    @Transactional(readOnly = true)
    public List<DetalhamentoAgendamentoDTO> listarTodosDoDono(String emailLogado) {
        Barbeiro usuario = barbeiroRepository.findByEmail(emailLogado).get();
        Long idDono = (usuario.getDono() != null) ? usuario.getDono().getId() : usuario.getId();

        // No Repository crie: findAllByBarbeiroDonoId(Long idDono)
        // Ou filtre os barbeiros que pertencem a esse dono e busque os agendamentos deles
        return agendamentoRepository.findAllByBarbeiroDonoId(idDono).stream()
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
        var barbeiro = barbeiroRepository.findByEmail(emailBarbeiro)
                .orElseThrow(() -> new RegraDeNegocioException("Barbeiro não encontrado"));

        return agendamentoRepository.findByBarbeiroIdOrderByDataHoraInicioDesc(barbeiro.getId()).stream()
                .map(DetalhamentoAgendamentoDTO::new)
                .toList();
    }

    // --- Métodos Auxiliares Privados ---

    private void validarHorarioFuncionamento(LocalDateTime dataInicio) {
        DayOfWeek diaSemana = dataInicio.getDayOfWeek();
        if (diaSemana == DayOfWeek.SUNDAY || diaSemana == DayOfWeek.MONDAY) {
            throw new ValidacaoException("Estamos fechados aos domingos e segundas!");
        }

        int hora = dataInicio.getHour();
        if (hora < HORARIO_ABERTURA || hora > HORARIO_FECHAMENTO) {
            throw new RegraDeNegocioException("Barbearia fechada neste horário.");
        }
    }

    private void calcularDivisaoFinanceira(Agendamento agendamento, Barbeiro barbeiro) {
        BigDecimal preco = agendamento.getValorCobrado();

        // Regra de comissão segura com BigDecimal
        BigDecimal comissaoPercentual = barbeiro.getComissaoPorcentagem() != null
                ? barbeiro.getComissaoPorcentagem()
                : new BigDecimal("50.0"); // Default 50%

        // Fórmula: (Preço * Porcentagem) / 100
        BigDecimal valorBarbeiro = preco.multiply(comissaoPercentual)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);

        BigDecimal valorCasa = preco.subtract(valorBarbeiro);

        agendamento.setValorTotal(preco);
        agendamento.setValorBarbeiro(valorBarbeiro);
        agendamento.setValorCasa(valorCasa);
    }

    private boolean isHorarioLivre(LocalDateTime slotInicio, LocalDateTime slotFim,
                                   List<Agendamento> agendamentos, List<Bloqueio> bloqueios) {

        // Verifica colisão com Agendamentos
        for (Agendamento ag : agendamentos) {
            // Buffer de segurança para não colar horários exatos se necessário
            if (slotInicio.isBefore(ag.getDataHoraFim()) && slotFim.isAfter(ag.getDataHoraInicio())) {
                return false;
            }
        }

        // Verifica colisão com Bloqueios (Almoço, Folga)
        for (Bloqueio b : bloqueios) {
            if (slotInicio.isBefore(b.getFim()) && slotFim.isAfter(b.getInicio())) {
                return false;
            }
        }
        return true;
    }

    private void enviarNotificacaoSegura(Agendamento agendamento) {
        try {
            notificacaoService.notificarBarbeiro(agendamento.getBarbeiro(), agendamento);
        } catch (Exception e) {
            // Loga o erro mas não quebra a experiência do usuário
            log.error("Falha ao enviar notificação para o agendamento ID {}: {}", agendamento.getId(), e.getMessage());
        }
    }
    @Transactional(readOnly = true)
    public List<String> consultarDisponibilidade(Long barbeiroId, LocalDate data, Long servicoId) {
        // Apenas redireciona para o seu método existente que já faz a lógica certa
        return listarHorariosDisponiveis(barbeiroId, servicoId, data);
    }
}