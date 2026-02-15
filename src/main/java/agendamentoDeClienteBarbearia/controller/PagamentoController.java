package agendamentoDeClienteBarbearia.controller;


import agendamentoDeClienteBarbearia.dtos.RespostaPixDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.service.BarbeiroService;
import agendamentoDeClienteBarbearia.service.PagamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService service;
    private final BarbeiroService barbeiroService;

    // ========================================================
    // 1. CRIAR PAGAMENTO (PIX)
    // ========================================================
    // Mudamos de @PathVariable para pegar do Token (Mais seguro)
    @PostMapping("/upgrade")
    public ResponseEntity<RespostaPixDTO> criarPagamento() {
        // 1. Identifica quem está logado
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro barbeiro = barbeiroService.buscarPorEmail(email);

        // 2. O Service já devolve o DTO pronto com QR Code e Base64
        RespostaPixDTO dto = service.gerarPixUpgrade(barbeiro.getId());

        // 3. Retorna direto para o Front
        return ResponseEntity.ok(dto);
    }

    // ========================================================
    // 2. WEBHOOK (MERCADO PAGO)
    // ========================================================
    @PostMapping("/webhook")
    public ResponseEntity<Void> receberNotificacao(@RequestBody Map<String, Object> payload) {
        log.info("Webhook recebido: {}", payload);

        try {
            // Lógica robusta para extrair o ID do JSON do Mercado Pago
            // O formato geralmente é: { "data": { "id": "12345" }, "type": "payment" }
            if (payload.containsKey("data") && payload.get("data") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) payload.get("data");

                if (data.containsKey("id")) {
                    String idStr = String.valueOf(data.get("id"));
                    Long idPagamento = Long.parseLong(idStr);

                    // Chama o processamento no Service
                    service.processarWebhook(idPagamento);
                }
            }
            // Caso venha via Query Param (IPN legado), você pode adicionar um fallback aqui
            // mas o padrão Webhook V1 é o corpo JSON acima.

        } catch (Exception e) {
            log.error("Erro ao processar webhook", e);
        }

        // Sempre retorna 200 OK para o Mercado Pago não reenviar
        return ResponseEntity.ok().build();
    }
}