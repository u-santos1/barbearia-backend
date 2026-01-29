package agendamentoDeClienteBarbearia.controller;



import agendamentoDeClienteBarbearia.service.PagamentoService;
import com.mercadopago.resources.payment.Payment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/pagamentos")
public class PagamentoController {

    private final PagamentoService service;

    public PagamentoController(PagamentoService service) {
        this.service = service;
    }

    // Front chama essa rota quando clicar no botão
    @PostMapping("/criar/{idBarbeiro}")
    public ResponseEntity<?> criarPagamento(@PathVariable Long idBarbeiro) {
        Payment pagamento = service.gerarPixUpgrade(idBarbeiro);

        // Retorna o Copia e Cola e a Imagem QR Code (Base64)
        return ResponseEntity.ok(Map.of(
                "id_pagamento", pagamento.getId(),
                "qr_code", pagamento.getPointOfInteraction().getTransactionData().getQrCode(),
                "qr_code_base64", pagamento.getPointOfInteraction().getTransactionData().getQrCodeBase64()
        ));
    }

    // O Mercado Pago chama essa rota SOZINHO quando houver atualização
    @PostMapping("/webhook")
    public ResponseEntity<?> receberNotificacao(@RequestParam("id") Long id, @RequestParam("topic") String topic) {
        if ("payment".equals(topic)) {
            service.processarWebhook(id);
        }
        return ResponseEntity.ok().build(); // Responde 200 OK pro Mercado Pago não ficar tentando de novo
    }
}