package agendamentoDeClienteBarbearia.service;


import agendamentoDeClienteBarbearia.StatusAgendamento;
import agendamentoDeClienteBarbearia.dtos.AgendamentoDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.infra.security.ValidacaoException;
import agendamentoDeClienteBarbearia.model.*;
import agendamentoDeClienteBarbearia.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static agendamentoDeClienteBarbearia.StatusAgendamento.CONCLUIDO;


@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final BloqueioRepository bloqueioRepository;
    private final NotificacaoService notificacaoService;

    public AgendamentoService(
            BloqueioRepository bloqueioRepository,
            AgendamentoRepository agendamentoRepository,
            BarbeiroRepository barbeiroRepository,
            ClienteRepository clienteRepository,
            ServicoRepository servicoRepository,
            NotificacaoService notificacaoService) {
        this.agendamentoRepository = agendamentoRepository;
        this.barbeiroRepository = barbeiroRepository;
        this.clienteRepository = clienteRepository;
        this.servicoRepository = servicoRepository;
        this.bloqueioRepository = bloqueioRepository;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public DetalhamentoAgendamentoDTO agendar(AgendamentoDTO dados) { // Ajustei o tipo para DadosAgendamentoDTO se for o padrão

        // 1. Validar se as entidades existem
        Barbeiro barbeiro = barbeiroRepository.findById(dados.barbeiroId())
                .orElseThrow(() -> new RegraDeNegocioException("Barbeiro não encontrado"));

        Cliente cliente = clienteRepository.findById(dados.clienteId())
                .orElseThrow(() -> new RegraDeNegocioException("Cliente não encontrado"));

        Servico servico = servicoRepository.findById(dados.servicoId())
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        // --- REMOVIDO DAQUI: O SAVE E O ALERTA ESTAVAM NO LUGAR ERRADO ---

        // Variável principal da data
        LocalDateTime dataInicio = dados.dataHoraInicio();

        // 2. REGRA: BARBEARIA FECHADA (Domingo e Segunda)
        DayOfWeek diaSemana = dataInicio.getDayOfWeek();
        if (diaSemana == DayOfWeek.SUNDAY || diaSemana == DayOfWeek.MONDAY) {
            throw new ValidacaoException("Estamos fechados aos domingos e segundas!");
        }

        // 3. Validar Data no Passado
        if (dataInicio.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException("Não é possível agendar em datas passadas.");
        }

        // 4. Validar Horário de Funcionamento (09:00 as 18:00)
        var hora = dataInicio.getHour();
        if (hora < 9 || hora > 18) {
            throw new RegraDeNegocioException("Barbearia fechada neste horário (Funcionamos das 09h às 18h).");
        }

        // 5. Calcular Data Fim
        var dataFim = dataInicio.plusMinutes(servico.getDuracaoEmMinutos());

        // 6. Validar Conflito de Horário
        boolean existeConflito = agendamentoRepository.existeConflitoDeHorario(
                barbeiro.getId(),
                dataInicio,
                dataFim
        );

        if (existeConflito) {
            throw new RegraDeNegocioException("Este barbeiro já está ocupado neste horário.");
        }

        // 7. Montar Objeto
        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServico(servico);
        agendamento.setDataHoraInicio(dataInicio);
        agendamento.setDataHoraFim(dataFim);
        agendamento.setValorCobrado(servico.getPreco());
        agendamento.setStatus(StatusAgendamento.AGENDADO);

        // 8. Salvar REALMENTE no banco
        Agendamento agendamentoSalvo = agendamentoRepository.save(agendamento);

        // 🔥 DISPARA O ALERTA (AGORA SIM, NO LUGAR CERTO)
        // Só dispara se tudo acima deu certo e salvou no banco
        try {
            // Usa 'agendamentoSalvo' que acabou de ser criado
            notificacaoService.notificarBarbeiro(agendamentoSalvo.getBarbeiro(), agendamentoSalvo);
        } catch (Exception e) {
            // Loga o erro, mas não cancela o agendamento que já foi salvo
            System.err.println("Erro ao enviar push: " + e.getMessage());
        }

        return DetalhamentoAgendamentoDTO.toDTO(agendamentoSalvo);
    }

    @Transactional
    public void cancelar(Long agendamentoId) {
        var agendamento = buscarPorId(agendamentoId);
        agendamento.setStatus(StatusAgendamento.CANCELADO_PELO_CLIENTE);
        agendamentoRepository.save(agendamento);
    }

    @Transactional
    public void confirmar(Long id) {
        var agendamento = buscarPorId(id);
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamentoRepository.save(agendamento);
    }

    // --- CORREÇÃO AQUI: Apenas UM método concluir e usando o Enum correto ---
    @Transactional
    public void concluir(Long id) {
        var agendamento = buscarPorId(id);
        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        agendamentoRepository.save(agendamento);
    }

    @Transactional
    public void cancelarPeloBarbeiro(Long id) {
        var agendamento = buscarPorId(id);
        agendamento.setStatus(StatusAgendamento.CANCELADO_PELO_BARBEIRO);
        agendamentoRepository.save(agendamento);
    }

    // Método auxiliar privado
    private Agendamento buscarPorId(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Agendamento não encontrado"));
    }

    // Cálculo de Disponibilidade
    public List<String> listarHorariosDisponiveis(Long barbeiroId, Long servicoId, LocalDate data) {
        // 1. Descobrir duração do serviço
        var servico = servicoRepository.findById(servicoId).orElseThrow();
        int duracaoMinutos = servico.getDuracaoEmMinutos();

        // 2. Pegar agenda ocupada do dia (Agendamentos e Bloqueios)
        LocalDateTime inicioDia = data.atStartOfDay();
        LocalDateTime fimDia = data.atTime(LocalTime.MAX);

        List<Agendamento> agendamentos = agendamentoRepository.findAgendaDoDia(barbeiroId, inicioDia, fimDia);
        List<Bloqueio> bloqueios = bloqueioRepository.findBloqueiosDoDia(barbeiroId, inicioDia, fimDia);

        // 3. Configurar horário de trabalho (Ex: 09:00 as 19:00)
        LocalTime abertura = LocalTime.of(9, 0);
        LocalTime fechamento = LocalTime.of(19, 0);

        List<String> horariosLivres = new ArrayList<>();
        LocalTime slotAtual = abertura;

        while (!slotAtual.plusMinutes(duracaoMinutos).isAfter(fechamento)) {

            boolean estaLivre = true;
            LocalDateTime slotInicio = LocalDateTime.of(data, slotAtual);
            LocalDateTime slotFim = slotInicio.plusMinutes(duracaoMinutos);

            // Validação 1: Colisão com Agendamentos
            for (Agendamento ag : agendamentos) {
                LocalDateTime agInicio = ag.getDataHoraInicio();
                // Assumindo que o agendamento salvo já tem a duração correta ou pegamos do serviço
                LocalDateTime agFim = agInicio.plusMinutes(ag.getServico().getDuracaoEmMinutos());

                if (slotInicio.isBefore(agFim) && slotFim.isAfter(agInicio)) {
                    estaLivre = false;
                    break;
                }
            }

            // Validação 2: Colisão com Bloqueios
            if (estaLivre) {
                for (Bloqueio b : bloqueios) {
                    if (slotInicio.isBefore(b.getFim()) && slotFim.isAfter(b.getInicio())) {
                        estaLivre = false;
                        break;
                    }
                }
            }

            if (estaLivre) {
                horariosLivres.add(slotAtual.toString());
            }

            slotAtual = slotAtual.plusMinutes(30);
        }

        return horariosLivres;
    }
}