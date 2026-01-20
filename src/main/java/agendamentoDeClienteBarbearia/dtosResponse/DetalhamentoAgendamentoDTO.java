package agendamentoDeClienteBarbearia.dtosResponse;


import agendamentoDeClienteBarbearia.StatusAgendamento;
import agendamentoDeClienteBarbearia.model.Agendamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DetalhamentoAgendamentoDTO(
        Long id,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        StatusAgendamento status,
        BigDecimal valorCobrado,

        // Aninhando os DTOs para o JSON ficar completo
        DetalhamentoBarbeiroDTO barbeiro,
        DetalhamentoClienteDTO cliente,
        DetalhamentoServicoDTO servico
) {
    public static DetalhamentoAgendamentoDTO toDTO(Agendamento agendamento) {
        return new DetalhamentoAgendamentoDTO(
                agendamento.getId(),
                agendamento.getDataHoraInicio(),
                agendamento.getDataHoraFim(),
                agendamento.getStatus(),
                agendamento.getValorCobrado(),
                new DetalhamentoBarbeiroDTO(agendamento.getBarbeiro()),
                new DetalhamentoClienteDTO(agendamento.getCliente()),
                new DetalhamentoServicoDTO(agendamento.getServico())
        );
    }
}