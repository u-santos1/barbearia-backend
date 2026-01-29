package agendamentoDeClienteBarbearia.controller;


import agendamentoDeClienteBarbearia.dtos.LoginDTO;
import agendamentoDeClienteBarbearia.dtosResponse.TokenJWTData;
import agendamentoDeClienteBarbearia.service.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AutenticacaoController {

    private final AutenticacaoService service;

    public AutenticacaoController(AutenticacaoService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenJWTData> efetuarLogin(@RequestBody @Valid LoginDTO dados) {
        var tokenData = service.realizarLogin(dados);
        return ResponseEntity.ok(tokenData);
    }
}