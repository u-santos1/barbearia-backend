package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.RespostaPixDTO;
import agendamentoDeClienteBarbearia.dtos.UpgradeRequestDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
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
    // 1. GERAR PIX PARA RENOVAÇÃO / UPGRADE (SAAS)
    // ========================================================
    @PostMapping("/upgrade")
    public ResponseEntity<RespostaPixDTO> criarPagamento(@RequestBody UpgradeRequestDTO dados) {
        // 1. Identifica o barbeiro logado com segurança pelo Token JWT
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        DetalhamentoBarbeiroDTO barbeiro = barbeiroService.buscarPorEmail(email);

        // 2. Chama o service passando o ID real do banco e os dados coletados (Nome e CPF)
        RespostaPixDTO resposta = service.gerarPixUpgrade(barbeiro.id(), dados);

        return ResponseEntity.ok(resposta);
    }

    // ========================================================
    // 2. CANCELAMENTO / DOWNGRADE DE ASSINATURA
    // ========================================================
    @DeleteMapping("/cancelar")
    public ResponseEntity<Void> cancelarAssinatura() {
        // 1. Pega o e-mail pelo Token de segurança (à prova de hackers)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Aciona o serviço para fazer o downgrade imediato para o plano SOLO
        service.cancelarAssinatura(email);

        return ResponseEntity.noContent().build(); // Retorna 204 No Content (Sucesso, sem corpo na resposta)
    }

    // ========================================================
    // 3. WEBHOOK (MERCADO PAGO)
    // ========================================================
    @PostMapping("/webhook")
    public ResponseEntity<Void> receberNotificacao(@RequestBody Map<String, Object> payload) {
        log.info("Webhook recebido do Mercado Pago. Payload parcial: {}", payload);

        try {
            // Verifica se a notificação contém os dados do pagamento
            if (payload.containsKey("data") && payload.get("data") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) payload.get("data");

                if (data.containsKey("id")) {
                    String idStr = String.valueOf(data.get("id"));
                    Long idPagamento = Long.parseLong(idStr);

                    // Deixa que o PagamentoService verifique se foi aprovado e renove os 30 dias do SaaS
                    service.processarWebhook(idPagamento);
                }
            }
            // Retorna 200 OK obrigatoriamente para o Mercado Pago não fazer retentativas infinitas
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Erro CRITICO ao processar webhook do pagamento. O Mercado Pago fará retentativa.", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}