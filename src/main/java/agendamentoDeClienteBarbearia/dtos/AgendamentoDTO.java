package agendamentoDeClienteBarbearia.dtos;

import agendamentoDeClienteBarbearia.StatusAgendamento;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgendamentoDTO(

        @NotNull Long barbeiroId,
        @NotNull Long clienteId,
        @NotNull Long servicoId,
        @NotNull @Future LocalDateTime dataHoraInicio // O único horário que importa na entrada
) {}
