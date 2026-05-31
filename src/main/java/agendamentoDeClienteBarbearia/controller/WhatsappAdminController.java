package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.service.WhatsappAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whatsapp/admin")
public class WhatsappAdminController {

    @Autowired
    private WhatsappAdminService service;

    // 1. VERIFICAR STATUS
    @GetMapping("/status/{nome}")
    public ResponseEntity<String> obterStatus(@PathVariable String nome) {
        try {
            return ResponseEntity.ok(service.obterStatus(nome));
        } catch (Exception e) {
            // Se a API der erro, sabemos que a instância está desconectada ou não existe
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"instance\": \"not_found\"}");
        }
    }

    // 2. BUSCAR QR CODE
    @GetMapping("/connect/{nome}")
    public ResponseEntity<String> lerQrCode(@PathVariable String nome) {
        try {
            return ResponseEntity.ok(service.lerQrCode(nome));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Erro ao gerar QR Code\"}");
        }
    }

    // 3. CRIAR INSTÂNCIA CASO NÃO EXISTA
    @PostMapping("/create/{nome}")
    public ResponseEntity<String> criarInstancia(@PathVariable String nome) {
        try {
            return ResponseEntity.ok(service.criarInstancia(nome));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Erro ao criar instancia\"}");
        }
    }
}