package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Bloqueio;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.BloqueioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/bloqueios")
@CrossOrigin(origins = "*")
public class BloqueioController {

    @Autowired
    private BloqueioRepository repository;
    @Autowired private BarbeiroRepository barbeiroRepository;

    @PostMapping
    public ResponseEntity criarBloqueio(@RequestBody DadosBloqueioDTO dados, @RequestHeader("Authorization") String token) {

        // 1. Descobrir quem está logado (Simulação rápida)
        // O jeito certo é usar o SecurityContextHolder ou decodificar o token
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro barbeiro = barbeiroRepository.findByEmail(emailLogado).orElseThrow();

        // 2. Se for Dono tentando bloquear agenda de outro (Futuro), lógica seria diferente.
        // Por enquanto, assume que quem logou está bloqueando a PRÓPRIA agenda.

        var bloqueio = new Bloqueio(null, barbeiro, dados.inicio(), dados.fim(), dados.motivo());
        repository.save(bloqueio);

        return ResponseEntity.ok().build();
    }

    // DTO auxiliar (pode criar arquivo separado)
    public record DadosBloqueioDTO(Long barbeiroId, LocalDateTime inicio, LocalDateTime fim, String motivo) {}
}