package agendamentoDeClienteBarbearia.controller;




import agendamentoDeClienteBarbearia.dtos.LoginDTO;
import agendamentoDeClienteBarbearia.dtosResponse.TokenJWTData;
import agendamentoDeClienteBarbearia.service.AutenticacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final AutenticacaoService service;

    @PostMapping("/login")
    public ResponseEntity<TokenJWTData> efetuarLogin(@RequestBody @Valid LoginDTO dados) {
        var tokenData = service.realizarLogin(dados);
        return ResponseEntity.ok(tokenData);
    }
}