package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.service.BarbeiroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/barbeiros")
@RequiredArgsConstructor
public class BarbeiroController {

    private final BarbeiroService service;

    // 1. CADASTRO DE DONO
    @PostMapping
    public ResponseEntity<DetalhamentoBarbeiroDTO> cadastrarDono(@RequestBody @Valid CadastroBarbeiroDTO dados, UriComponentsBuilder uriBuilder) {
        Barbeiro barbeiro = service.cadastrarDono(dados);
        var dto = new DetalhamentoBarbeiroDTO(barbeiro);
        var uri = uriBuilder.path("/barbeiros/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    // 2. CADASTRO DE EQUIPE
    @PostMapping("/equipe")
    public ResponseEntity<DetalhamentoBarbeiroDTO> cadastrarFuncionario(@RequestBody @Valid CadastroBarbeiroDTO dados, UriComponentsBuilder uriBuilder) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro dono = service.buscarPorEmail(email);

        Barbeiro novoFuncionario = service.cadastrarFuncionario(dados, dono.getId());
        var dto = new DetalhamentoBarbeiroDTO(novoFuncionario);
        var uri = uriBuilder.path("/barbeiros/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    // 3. LISTAR EQUIPE (ADMIN)
    @GetMapping("/equipe")
    public ResponseEntity<List<DetalhamentoBarbeiroDTO>> listarEquipe() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro dono = service.buscarPorEmail(email);
        return ResponseEntity.ok(service.listarEquipe(dono.getId()));
    }

    // 4. INATIVAR
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro dono = service.buscarPorEmail(email);
        service.inativar(id, dono.getId());
        return ResponseEntity.noContent().build();
    }

    // ================================================================
    // 5. LISTAGEM PÚBLICA (USADA NO AGENDAMENTO DO FRONT)
    // ================================================================
    // ✅ CORREÇÃO: Agora usa o Service.listarPorLoja que trata tudo corretamente
    @GetMapping
    public ResponseEntity<List<DetalhamentoBarbeiroDTO>> listarBarbeiros(@RequestParam(required = false) Long lojaId) {
        var lista = service.listarPorLoja(lojaId);
        return ResponseEntity.ok(lista);
    }
}