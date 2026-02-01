package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.DadosBloqueioDTO;

import agendamentoDeClienteBarbearia.service.BloqueioService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/bloqueios")
@RequiredArgsConstructor
public class BloqueioController {

    private final BloqueioService service;

    @PostMapping
    public ResponseEntity<Void> criarBloqueio(@RequestBody @Valid DadosBloqueioDTO dados) {
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        service.bloquearAgenda(dados, emailLogado);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desbloquear(@PathVariable Long id) {
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        service.desbloquear(id, emailLogado);
        return ResponseEntity.noContent().build();
    }
}