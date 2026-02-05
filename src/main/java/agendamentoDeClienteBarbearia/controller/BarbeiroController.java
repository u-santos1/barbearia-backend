package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
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
    private final BarbeiroRepository repository; // ✅ Injeção para leitura direta (Alta Performance)

    // 1. CADASTRO DE DONO (SaaS)
    @PostMapping
    public ResponseEntity<DetalhamentoBarbeiroDTO> cadastrarDono(@RequestBody @Valid CadastroBarbeiroDTO dados, UriComponentsBuilder uriBuilder) {
        Barbeiro barbeiro = service.cadastrarDono(dados);
        var dto = new DetalhamentoBarbeiroDTO(barbeiro);
        var uri = uriBuilder.path("/barbeiros/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    // 2. CADASTRO DE EQUIPE (Apenas Dono logado)
    @PostMapping("/equipe")
    public ResponseEntity<DetalhamentoBarbeiroDTO> cadastrarFuncionario(@RequestBody @Valid CadastroBarbeiroDTO dados, UriComponentsBuilder uriBuilder) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro dono = service.buscarPorEmail(email);

        Barbeiro novoFuncionario = service.cadastrarFuncionario(dados, dono.getId());

        var dto = new DetalhamentoBarbeiroDTO(novoFuncionario);
        var uri = uriBuilder.path("/barbeiros/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    // 3. LISTAR EQUIPE (Visão Interna do Admin)
    @GetMapping("/equipe")
    public ResponseEntity<List<DetalhamentoBarbeiroDTO>> listarEquipe() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro dono = service.buscarPorEmail(email);

        return ResponseEntity.ok(service.listarEquipe(dono.getId()));
    }

    // 4. INATIVAR (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro dono = service.buscarPorEmail(email);

        service.inativar(id, dono.getId());
        return ResponseEntity.noContent().build();
    }

    // ================================================================
    // 5. LISTAGEM PÚBLICA (USADA NO AGENDAMENTO)
    // ================================================================
    // Alterado para suportar Multi-Tenancy via ?lojaId=1
    @GetMapping
    public ResponseEntity<List<DetalhamentoBarbeiroDTO>> listarBarbeiros(@RequestParam(required = false) Long lojaId) {

        List<Barbeiro> barbeiros;

        if (lojaId != null) {
            // ✅ CENÁRIO 1: Cliente Agendando (Vê apenas a equipe daquela loja)
            barbeiros = repository.findAllByLoja(lojaId);
        } else {
            // ⚠️ CENÁRIO 2: Fallback / Admin Geral
            // Se ninguém estiver logado e não mandou lojaId, retornamos vazio ou erro para proteger os dados
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName().equals("anonymousUser")) {
                // Proteção: Não listar todos os barbeiros do mundo para um anônimo sem filtro
                return ResponseEntity.badRequest().build();
            }

            // Se for um usuário logado tentando listar tudo (ex: Super Admin), mantemos o comportamento antigo
            barbeiros = repository.findAllByAtivoTrue();
        }

        var dtos = barbeiros.stream()
                .map(DetalhamentoBarbeiroDTO::new)
                .toList();

        return ResponseEntity.ok(dtos);
    }
}