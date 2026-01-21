package agendamentoDeClienteBarbearia.service;


import agendamentoDeClienteBarbearia.StatusAgendamento;
import agendamentoDeClienteBarbearia.dtos.AgendamentoDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.infra.security.ValidacaoException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Cliente;
import agendamentoDeClienteBarbearia.model.Servico;
import jakarta.transaction.Transactional;
import agendamentoDeClienteBarbearia.repository.ServicoRepository;
import agendamentoDeClienteBarbearia.repository.AgendamentoRepository;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.ClienteRepository;
import agendamentoDeClienteBarbearia.model.Agendamento;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;



@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;

    public AgendamentoService(
            AgendamentoRepository agendamentoRepository,
            BarbeiroRepository barbeiroRepository,
            ClienteRepository clienteRepository,
            ServicoRepository servicoRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.barbeiroRepository = barbeiroRepository;
        this.clienteRepository = clienteRepository;
        this.servicoRepository = servicoRepository;
    }

    @Transactional
    public DetalhamentoAgendamentoDTO agendar(AgendamentoDTO dados) {

        // 1. Validar se as entidades existem
        Barbeiro barbeiro = barbeiroRepository.findById(dados.barbeiroId())
                .orElseThrow(() -> new RegraDeNegocioException("Barbeiro não encontrado"));

        Cliente cliente = clienteRepository.findById(dados.clienteId())
                .orElseThrow(() -> new RegraDeNegocioException("Cliente não encontrado"));

        Servico servico = servicoRepository.findById(dados.servicoId())
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

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

        // 7. Salvar
        Agendamento agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServico(servico);
        agendamento.setDataHoraInicio(dataInicio);
        agendamento.setDataHoraFim(dataFim);
        agendamento.setValorCobrado(servico.getPreco());
        agendamento.setStatus(StatusAgendamento.AGENDADO);

        Agendamento salvar = agendamentoRepository.save(agendamento);

        return DetalhamentoAgendamentoDTO.toDTO(salvar);
    }

    public void cancelar(Long agendamentoId) {
        var agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RegraDeNegocioException("Agendamento não encontrado"));

        agendamento.setStatus(StatusAgendamento.CANCELADO_PELO_CLIENTE);
    }
    // 1. Confirmar (Quando o barbeiro vê e dá ok)
    public void confirmar(Long id) {
        var agendamento = buscarPorId(id);
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        agendamentoRepository.save(agendamento);
    }

    // 2. Concluir (Quando o corte termina e o cliente paga)
    public void concluir(Long id) {
        var agendamento = buscarPorId(id);
        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        agendamentoRepository.save(agendamento);
    }

    // 3. Cancelar pelo Barbeiro (Imprevisto da barbearia)
    public void cancelarPeloBarbeiro(Long id) {
        var agendamento = buscarPorId(id);
        agendamento.setStatus(StatusAgendamento.CANCELADO_PELO_BARBEIRO);
        agendamentoRepository.save(agendamento);
    }

    // Método auxiliar para não repetir código
    private Agendamento buscarPorId(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Agendamento não encontrado"));
    }
}