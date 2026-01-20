package agendamentoDeClienteBarbearia.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CadastroServicoDTO(


        @NotBlank(message = "O nome do serviço é obrigatório")
        String nome,

        String descricao, // Pode ser null, então sem validação obrigatória

        @NotNull(message = "O preço é obrigatório")
        @Positive(message = "O preço deve ser maior que zero")
        BigDecimal preco,

        @NotNull(message = "A duração é obrigatória")
        @Min(value = 15, message = "A duração mínima deve ser de 15 minutos")
        Integer duracaoEmMinutos
) {}