package agendamentoDeClienteBarbearia.dtos;



import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BloqueioDTO(
        @NotNull(message = "O início do bloqueio é obrigatório")
        @FutureOrPresent(message = "Não é possível bloquear horários no passado")
        LocalDateTime dataHoraInicio,

        @NotNull(message = "O fim do bloqueio é obrigatório")
        LocalDateTime dataHoraFim,

        String motivo
) {
    // Validação extra: Garantir que Fim > Início
    public boolean isHorarioValido() {
        return dataHoraFim.isAfter(dataHoraInicio);
    }
}