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
        DetalhamentoBarbeiroDTO barbeiro,
        DetalhamentoClienteDTO cliente,
        DetalhamentoServicoDTO servico
) {
    // Construtor Canônico Otimizado
    public DetalhamentoAgendamentoDTO(Agendamento agendamento) {
        this(
                agendamento.getId(),
                agendamento.getDataHoraInicio(),
                agendamento.getDataHoraFim(),
                agendamento.getStatus(),
                agendamento.getValorCobrado(),
                // Proteção contra NullPointerException se o relacionamento não vier
                agendamento.getBarbeiro() != null ? new DetalhamentoBarbeiroDTO(agendamento.getBarbeiro()) : null,
                agendamento.getCliente() != null ? new DetalhamentoClienteDTO(agendamento.getCliente()) : null,
                agendamento.getServico() != null ? new DetalhamentoServicoDTO(agendamento.getServico()) : null
        );
    }
}