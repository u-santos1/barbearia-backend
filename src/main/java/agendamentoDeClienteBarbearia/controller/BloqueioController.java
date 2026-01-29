package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Bloqueio;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.BloqueioRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/bloqueios")
@CrossOrigin(origins = "*")
public class BloqueioController {

    private final BloqueioService service; // Crie este Service se não tiver

    public BloqueioController(BloqueioService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> criarBloqueio(@RequestBody @Valid DadosBloqueioDTO dados) {
        // Pega o usuário logado de forma limpa
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();

        service.bloquearAgenda(dados, emailLogado);

        return ResponseEntity.ok().build();
    }
}