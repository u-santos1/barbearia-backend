package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.DadosBloqueioDTO;
import agendamentoDeClienteBarbearia.model.Bloqueio;
import agendamentoDeClienteBarbearia.service.BloqueioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bloqueios")
@RequiredArgsConstructor
public class BloqueioController {

    private final BloqueioService service;

    // ========================================================
    // 1. CRIAR BLOQUEIO (POST)
    // ========================================================
    @PostMapping
    public ResponseEntity<Void> criarBloqueio(@RequestBody @Valid DadosBloqueioDTO dados) {
        // Pega quem está logado para validar se ele pode bloquear essa agenda
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();

        service.bloquearAgenda(dados, emailLogado);

        // Retorna 204 No Content (Sucesso, sem corpo) ou 201 Created
        return ResponseEntity.noContent().build();
    }

    // ========================================================
    // 2. REMOVER BLOQUEIO (DELETE)
    // ========================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desbloquear(@PathVariable Long id) {
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();

        service.desbloquear(id, emailLogado);

        return ResponseEntity.noContent().build();
    }

    // ========================================================
    // 3. LISTAR (GET) - 🚨 FALTAVA ISSO
    // ========================================================
    // O Frontend chama isso para pintar os dias bloqueados de cinza no calendário
    @GetMapping("/barbeiro/{barbeiroId}")
    public ResponseEntity<List<Bloqueio>> listarBloqueios(@PathVariable Long barbeiroId) {
        var lista = service.listarBloqueiosFuturos(barbeiroId);
        return ResponseEntity.ok(lista);
    }
}