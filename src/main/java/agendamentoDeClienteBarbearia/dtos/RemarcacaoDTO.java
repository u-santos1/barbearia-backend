package agendamentoDeClienteBarbearia.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RemarcacaoDTO(
        @NotNull @FutureOrPresent @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime novaDataHoraInicio
) {}
