package agendamentoDeClienteBarbearia.controller;


import agendamentoDeClienteBarbearia.dtosResponse.AssinaturaDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.service.AssinaturaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/assinaturas")
@RequiredArgsConstructor
public class AssinaturaController {

    private final AssinaturaService service;

    // -----------------------------------------------
    // PLANOS
    // -----------------------------------------------

    @PostMapping("/planos")
    public ResponseEntity<AssinaturaDTO.PlanoResponseDTO> criarPlano(
            @Valid @RequestBody AssinaturaDTO.CriarPlanoDTO dto,
            @AuthenticationPrincipal Barbeiro barbeiro) {
        return ResponseEntity.ok(service.criarPlano(dto, barbeiro.getId()));
    }

    @GetMapping("/planos")
    public ResponseEntity<List<AssinaturaDTO.PlanoResponseDTO>> listarPlanos(
            @AuthenticationPrincipal Barbeiro barbeiro) {
        return ResponseEntity.ok(service.listarPlanos(barbeiro.getId()));
    }

    @DeleteMapping("/planos/{id}")
    public ResponseEntity<Void> excluirPlano(
            @PathVariable Long id,
            @AuthenticationPrincipal Barbeiro barbeiro) {
        service.excluirPlano(id, barbeiro.getId());
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------
    // ASSINATURAS
    // -----------------------------------------------

    // Barbeiro assina manualmente um cliente
    @PostMapping
    public ResponseEntity<AssinaturaDTO.AssinaturaResponseDTO> assinarManual(
            @Valid @RequestBody AssinaturaDTO.AssinarDTO dto,
            @AuthenticationPrincipal Barbeiro barbeiro) {
        return ResponseEntity.ok(service.assinarManual(dto, barbeiro.getId()));
    }

    // Lista todas as assinaturas do barbeiro
    @GetMapping
    public ResponseEntity<List<AssinaturaDTO.AssinaturaResponseDTO>> listar(
            @AuthenticationPrincipal Barbeiro barbeiro) {
        return ResponseEntity.ok(service.listarAssinaturas(barbeiro.getId()));
    }

    // Busca assinatura ativa de um cliente
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<AssinaturaDTO.AssinaturaResponseDTO> buscarAtiva(@PathVariable Long clienteId) {
        return ResponseEntity.ok(service.buscarAssinaturaAtiva(clienteId));
    }

    // -----------------------------------------------
    // USAR CORTE
    // -----------------------------------------------

    @PostMapping("/usar-corte")
    public ResponseEntity<AssinaturaDTO.AssinaturaResponseDTO> usarCorte(
            @Valid @RequestBody AssinaturaDTO.UsarCorteDTO dto,
            @AuthenticationPrincipal Barbeiro barbeiro) {
        return ResponseEntity.ok(service.usarCorte(dto, barbeiro.getId()));
    }

    // Cancelar assinatura
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal Barbeiro barbeiro) {
        service.cancelarAssinatura(id, barbeiro.getId());
        return ResponseEntity.noContent().build();
    }

    // -----------------------------------------------
    // WEBHOOK MERCADO PAGO
    // -----------------------------------------------

    @PostMapping("/webhook/mercadopago")
    public ResponseEntity<Void> webhookMp(@RequestBody AssinaturaDTO.MpWebhookDTO webhook) {
        if ("payment".equals(webhook.type()) && webhook.data() != null) {
            // Aqui você consulta o MP para pegar o status real
            // Por ora, registra o ID para processamento
            service.processarPagamentoMp(webhook.data().id(), "approved");
        }
        return ResponseEntity.ok().build();
    }

    // Endpoint para gerar link de pagamento MP (para assinatura)
    @PostMapping("/mercadopago/criar-preferencia")
    public ResponseEntity<Map<String, String>> criarPreferenciaMp(
            @RequestBody AssinaturaDTO.AssinarDTO dto,
            @AuthenticationPrincipal Barbeiro barbeiro) {

        return ResponseEntity.ok(Map.of(
                "mensagem", "Integração MP em implementação",
                // Corrigido: Usar o telefone ou nome, já que o clienteId não existe mais no DTO
                "clienteReferencia", dto.clienteTelefone(),
                "planoId", String.valueOf(dto.planoId())
        ));
    }
}
