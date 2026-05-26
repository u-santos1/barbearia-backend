package agendamentoDeClienteBarbearia.controller;




import agendamentoDeClienteBarbearia.dtos.LoginDTO;
import agendamentoDeClienteBarbearia.dtosResponse.TokenJWTData;
import agendamentoDeClienteBarbearia.infra.security.TokenBlacklistService;
import agendamentoDeClienteBarbearia.service.AutenticacaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final AutenticacaoService service;
    private final TokenBlacklistService blacklistService;

    @PostMapping("/login")
    public ResponseEntity<TokenJWTData> efetuarLogin(@RequestBody @Valid LoginDTO dados) {
        var tokenData = service.realizarLogin(dados);
        return ResponseEntity.ok(tokenData);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> efetuarLogout(HttpServletRequest request){
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")){
            String token = authHeader.substring(7).trim();
            blacklistService.invalidar(token);
        }
        return ResponseEntity.noContent().build();
    }
}