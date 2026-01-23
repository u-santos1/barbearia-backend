package agendamentoDeClienteBarbearia.dtosResponse;


public record TokenJWTData(
        String token,
        String nome,
        Long id
) {}