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
                // ✅ Proteção: Se o valor for null, retorna ZERO para não quebrar o JSON
                agendamento.getValorCobrado() != null ? agendamento.getValorCobrado() : BigDecimal.ZERO,

                agendamento.getBarbeiro() != null ? new DetalhamentoBarbeiroDTO(agendamento.getBarbeiro()) : null,
                agendamento.getCliente() != null ? new DetalhamentoClienteDTO(agendamento.getCliente()) : null,
                agendamento.getServico() != null ? new DetalhamentoServicoDTO(agendamento.getServico()) : null
        );
    }
}