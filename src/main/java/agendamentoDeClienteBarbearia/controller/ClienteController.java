package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.dtos.ResumoDashboardDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoClienteDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.service.BarbeiroService;
import agendamentoDeClienteBarbearia.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;

    // ========================================================
    // 1. CADASTRAR (Híbrido: Funciona Logado ou Público)
    // ========================================================
    @PostMapping
    public ResponseEntity<DetalhamentoClienteDTO> cadastrar(
            @RequestBody @Valid CadastroClienteDTO dados,
            @AuthenticationPrincipal Barbeiro usuarioLogado) { // Spring injeta o logado ou null automaticamente


        var dto = service.salvar(dados, usuarioLogado);

        var uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(dto.id()).toUri();

        return ResponseEntity.created(uri).body(dto);
    }

    // ========================================================
    // 2. LISTAR (Blindado por Dono - SaaS)
    // ========================================================
    @GetMapping
    public ResponseEntity<List<DetalhamentoClienteDTO>> listar(@AuthenticationPrincipal Barbeiro usuarioLogado) {

        Long donoId = usuarioLogado.getDono() != null ? usuarioLogado.getDono().getId() : usuarioLogado.getId();

        var lista = service.listarPorDono(donoId);
        return ResponseEntity.ok(lista);
    }

    // ========================================================
    // 3. RECUPERAR ID (Blindado e RESTful)
    // ========================================================
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/recuperar-id")
    public ResponseEntity<Long> recuperarIdPorEmail(@RequestParam String email) {
        Long id = service.buscarIdPorEmail(email);

        if (id == null) {
            return ResponseEntity.notFound().build(); // 404 limpo
        }

        return ResponseEntity.ok(id);
    }
}