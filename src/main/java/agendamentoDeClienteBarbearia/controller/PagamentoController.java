package agendamentoDeClienteBarbearia.controller;



import agendamentoDeClienteBarbearia.service.PagamentoService;
import com.mercadopago.resources.payment.Payment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import lombok.RequiredArgsConstructor;




@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService service;

    @PostMapping("/criar/{idBarbeiro}")
    public ResponseEntity<?> criarPagamento(@PathVariable Long idBarbeiro) {
        Payment pagamento = service.gerarPixUpgrade(idBarbeiro);

        // Retorna o payload necessário para o Front-end gerar o QR Code
        return ResponseEntity.ok(Map.of(
                "id_pagamento", pagamento.getId(),
                "qr_code", pagamento.getPointOfInteraction().getTransactionData().getQrCode(),
                "qr_code_base64", pagamento.getPointOfInteraction().getTransactionData().getQrCodeBase64(),
                "status", pagamento.getStatus()
        ));
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> receberNotificacao(@RequestParam("id") Long id, @RequestParam("topic") String topic) {
        if ("payment".equals(topic)) {
            service.processarWebhook(id);
        }
        return ResponseEntity.ok().build();
    }
}