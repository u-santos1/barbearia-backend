package agendamentoDeClienteBarbearia.dtos;


public record RespostaPixDTO(
        Long pagamentoId,
        String qrCodeCopiaCola,
        String qrCodeBase64,
        String status
) {}