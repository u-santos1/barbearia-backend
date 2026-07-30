package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.service.WhatsappAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.Map;

@RestController
@RequestMapping("/whatsapp/admin")
@RequiredArgsConstructor
public class WhatsappAdminController {


    private final WhatsappAdminService service;

    private String gerarNomeInstancia(UserDetails userDetails){
        return "zap-" + userDetails.getUsername().replace("[^a-zA-Z0-9]", "");
    }

    // 1. VERIFICAR STATUS
    @GetMapping("/status")
    public ResponseEntity<String> obterStatus(@AuthenticationPrincipal UserDetails usuario) {
        try {
            // BLINDAGEM: Se o usuário não estiver logado, barra na hora com Erro 401
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\": \"Usuário não autenticado\"}");
            }

            String instanciaUnica = gerarNomeInstancia(usuario);
            return ResponseEntity.ok(service.obterStatus(instanciaUnica));

        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"instance\": \"not_found\"}");
        }
    }

    // 2. BUSCAR QR CODE
    @GetMapping("/connect")
    public ResponseEntity<String> lerQrCode(@AuthenticationPrincipal UserDetails usuario){
        try {
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\": \"Usuário não autenticado\"}");
            }

            String instanciaUnica = gerarNomeInstancia(usuario);
            return ResponseEntity.ok(service.lerQrCode(instanciaUnica));

        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Erro interno: " + e.getMessage() + "\"}");
        }
    }

    // 3. CRIAR INSTÂNCIA CASO NÃO EXISTA
    @PostMapping("/create")
    public ResponseEntity<String> criarInstancia(@AuthenticationPrincipal UserDetails usuario) {
        try {
            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\": \"Usuário não autenticado\"}");
            }

            String instanciaUnica = gerarNomeInstancia(usuario);
            return ResponseEntity.ok(service.criarInstancia(instanciaUnica));

        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"Erro interno: " + e.getMessage() + "\"}");
        }
    }

}