package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.service.WhatsappAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;

@RestController
@RequestMapping("/whatsapp/admin")
@RequiredArgsConstructor
public class WhatsappAdminController {


    private final WhatsappAdminService service;

    // 1. VERIFICAR STATUS
    @GetMapping("/status/{nome}")
    public ResponseEntity<String> obterStatus(@PathVariable String nome) {
        try {
            return ResponseEntity.ok(service.obterStatus(nome));
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"instance\": \"not_found\"}");
        }
    }

    // 2. BUSCAR QR CODE
    @GetMapping("/connect/{nome}")
    public ResponseEntity<String> lerQrCode(@PathVariable String nome) {
        try {
            return ResponseEntity.ok(service.lerQrCode(nome));
        } catch (HttpStatusCodeException e) {

            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Erro interno: " + e.getMessage() + "\"}");
        }
    }

    // 3. CRIAR INSTÂNCIA CASO NÃO EXISTA
    @PostMapping("/create/{nome}")
    public ResponseEntity<String> criarInstancia(@PathVariable String nome) {
        try {
            return ResponseEntity.ok(service.criarInstancia(nome));
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Erro interno: " + e.getMessage() + "\"}");
        }
    }
}