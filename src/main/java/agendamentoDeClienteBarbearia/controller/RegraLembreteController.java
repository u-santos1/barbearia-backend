package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.DadosRegraLembreteDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoRegraLembreteDTO;
import agendamentoDeClienteBarbearia.service.RegraLembreteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/lembretes")
@RequiredArgsConstructor
public class RegraLembreteController {

    private final RegraLembreteService service;

    @PostMapping
    public ResponseEntity<DetalhamentoRegraLembreteDTO> criar(@RequestBody DadosRegraLembreteDTO dados,
                                                              @AuthenticationPrincipal UserDetails usuario,
                                                              UriComponentsBuilder uriBuilder) {
        var regra = service.criar(dados, usuario.getUsername());
        var uri = uriBuilder.path("/lembretes/{id}").buildAndExpand(regra.id()).toUri();
        return ResponseEntity.created(uri).body(regra);
    }

    @GetMapping
    public ResponseEntity<List<DetalhamentoRegraLembreteDTO>> listar(@AuthenticationPrincipal UserDetails usuario) {
        return ResponseEntity.ok(service.listar(usuario.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DetalhamentoRegraLembreteDTO> atualizar(@PathVariable Long id,
                                                                  @RequestBody DadosRegraLembreteDTO dados,
                                                                  @AuthenticationPrincipal UserDetails usuario) {
        return ResponseEntity.ok(service.atualizar(id, dados, usuario.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id, @AuthenticationPrincipal UserDetails usuario) {
        service.deletar(id, usuario.getUsername());
        return ResponseEntity.noContent().build();
    }
}