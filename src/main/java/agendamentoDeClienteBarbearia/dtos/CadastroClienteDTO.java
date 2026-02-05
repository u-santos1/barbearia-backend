package agendamentoDeClienteBarbearia.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;



import jakarta.validation.constraints.NotBlank;

public record CadastroClienteDTO(
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "O telefone é obrigatório")
        String telefone,

        // E-mail agora é 100% opcional (sem anotações de validação obrigatória)
        String email,

        Long barbeiroId // ✅ NOVO CAMPO (Opcional no Java, mas usaremos na lógica)
) {
}