package agendamentoDeClienteBarbearia.dtos;
import java.time.LocalTime;

public record DadosRegraLembreteDTO(
        String nome,
        String tempo,
        LocalTime hora,
        String msg,
        Boolean ativo
) {}