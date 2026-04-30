package agendamentoDeClienteBarbearia.controller;


import agendamentoDeClienteBarbearia.dtos.RespostaPixDTO;

import agendamentoDeClienteBarbearia.dtos.UpgradeRequestDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.service.BarbeiroService;
import agendamentoDeClienteBarbearia.service.PagamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;


import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService service;
    private final BarbeiroService barbeiroService;



    // ========================================================
    // 2. WEBHOOK (MERCADO PAGO)
    // ========================================================
    @PostMapping("/webhook")
    public ResponseEntity<Void> receberNotificacao(@RequestBody Map<String, Object> payload) {
        log.info("Webhook recebido do Mercado Pago. Payload parcial: {}", payload);

        try {
            if(payload.containsKey("data") && payload.get("data") instanceof Map){
                @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) payload.get("data");

                if (data.containsKey("id")){
                    String idStr = String.valueOf(data.get("id"));
                    Long idPagamento = Long.parseLong(idStr);

                    service.processarWebhook(idPagamento);
                }
            }
            return ResponseEntity.ok().build();
        }
        catch (Exception e){
            log.error("Erro CRITICO ao processar webhook do pagamento. O mercado Pago fara retentativa,", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @PostMapping("/upgrade")
    public ResponseEntity<RespostaPixDTO> criarPagamento(@RequestBody UpgradeRequestDTO dados) {
        // 1. Identifica o barbeiro logado pelo Token JWT
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        DetalhamentoBarbeiroDTO barbeiro = barbeiroService.buscarPorEmail(email);

        // 2. Chama o service passando o ID do usuário e os dados coletados (Nome/CPF)
        RespostaPixDTO resposta = service.gerarPixUpgrade(barbeiro.id(), dados);

        return ResponseEntity.ok(resposta);
    }
}