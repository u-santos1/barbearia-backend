package agendamentoDeClienteBarbearia.dtos;

import jakarta.validation.constraints.NotBlank;

public record PushTokenDTO(
        @NotBlank(message = "O token de push não pode estar em branco")
        String token
) {}