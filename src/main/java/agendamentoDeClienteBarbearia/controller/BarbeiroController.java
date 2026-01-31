package agendamentoDeClienteBarbearia.controller;


import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;

import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.service.BarbeiroService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;




import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.web.util.UriComponentsBuilder;



@RestController
@RequestMapping("/barbeiros")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BarbeiroController {

    private final BarbeiroService service;

    // 1. REGISTRO PÚBLICO (Cria a conta do Dono)
    @PostMapping("/registro")
    public ResponseEntity<DetalhamentoBarbeiroDTO> cadastrarDono(@RequestBody @Valid CadastroBarbeiroDTO dados, UriComponentsBuilder uriBuilder) {
        Barbeiro barbeiro = service.cadastrarDono(dados);
        var dto = new DetalhamentoBarbeiroDTO(barbeiro);
        var uri = uriBuilder.path("/barbeiros/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    // 2. CADASTRO DE EQUIPE (Apenas Dono logado pode fazer)
    @PostMapping("/equipe")
    public ResponseEntity<DetalhamentoBarbeiroDTO> cadastrarFuncionario(@RequestBody @Valid CadastroBarbeiroDTO dados, UriComponentsBuilder uriBuilder) {
        // Recupera ID do Dono logado
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro dono = service.buscarPorEmail(email);

        Barbeiro novoFuncionario = service.cadastrarFuncionario(dados, dono.getId());

        var dto = new DetalhamentoBarbeiroDTO(novoFuncionario);
        var uri = uriBuilder.path("/barbeiros/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    // 3. LISTAR MINHA EQUIPE
    @GetMapping("/equipe")
    public ResponseEntity<List<DetalhamentoBarbeiroDTO>> listarEquipe() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro dono = service.buscarPorEmail(email);

        return ResponseEntity.ok(service.listarEquipe(dono.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro dono = service.buscarPorEmail(email);

        service.inativar(id, dono.getId());
        return ResponseEntity.noContent().build();
    }
}