package agendamentoDeClienteBarbearia.dtos;



import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record DadosBloqueioDTO(
        Long barbeiroId, // Pode ser null se for bloqueio próprio

        @NotNull
        LocalDateTime inicio,

        @NotNull
        LocalDateTime fim,

        String motivo
) {}
