package agendamentoDeClienteBarbearia.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;



import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size; // <--- Importante adicionar

public record CadastroBarbeiroDTO(

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres") // Segurança básica
        String senha,

        @NotBlank(message = "A especialidade é obrigatória")
        String especialidade // Ex: "Corte", "Barba", "Colorimetria"

        ,Boolean vaiCortarCabelo
) {}