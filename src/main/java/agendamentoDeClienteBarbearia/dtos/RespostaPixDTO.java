package agendamentoDeClienteBarbearia.dtos;


public record RespostaPixDTO(
        Long pagamentoId,
        String qrCodeCopiaCola,
        String qrCodeBase64,
        String ticketUrl,   // <--- Adicionamos este aqui!
        String status
) {}