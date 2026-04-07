package agendamentoDeClienteBarbearia.dtos;

import agendamentoDeClienteBarbearia.StatusAgendamento;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AgendamentoDTO(

        @NotNull Long barbeiroId,
        @NotNull Long clienteId,
        @NotNull Long servicoId,
        @NotNull @Future @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime dataHoraInicio,// O único horário que importa na entrada
        String observacao
) {}
