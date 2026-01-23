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

    @Autowired
    private AutenticacaoService service; // Injetamos o Service, não o Manager

    @PostMapping("/login")
    public ResponseEntity<TokenJWTData> efetuarLogin(@RequestBody @Valid LoginDTO dados) {
        // O Controller não sabe como loga, ele só pede pro Service
        var tokenData = service.realizarLogin(dados);

        return ResponseEntity.ok(tokenData);
    }
}
