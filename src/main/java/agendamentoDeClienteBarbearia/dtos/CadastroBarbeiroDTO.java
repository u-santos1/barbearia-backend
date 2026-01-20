package agendamentoDeClienteBarbearia.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CadastroBarbeiroDTO(


        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "A especialidade é obrigatória")
        String especialidade // Ex: "Corte", "Barba", "Colorimetria"
) {}