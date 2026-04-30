package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoServicoDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.service.BarbeiroService;
import agendamentoDeClienteBarbearia.service.ServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/servicos")
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService service;

    @PostMapping
    public ResponseEntity<DetalhamentoServicoDTO> cadastrar(@RequestBody @Valid CadastroServicoDTO dados, UriComponentsBuilder uriBuilder) {

        // 1. Pega apenas a "identidade" de quem fez a requisição
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Passa a bola pro Service resolver a regra de negócio
        var dto = service.cadastrar(dados, emailLogado);

        // 3. Devolve a resposta HTTP
        var uri = uriBuilder.path("/servicos/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DetalhamentoServicoDTO> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid CadastroServicoDTO dados,
            @AuthenticationPrincipal Barbeiro barbeiroLogado) {

        // Passamos tanto o ID do serviço quanto o ID de quem está tentando alterar
        var dto = service.atualizar(id, dados, barbeiroLogado.getId());

        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    // Método Legado (Compatibilidade mantida)
    @GetMapping("/barbeiro/{idBarbeiro}")
    public ResponseEntity<List<DetalhamentoServicoDTO>> listarParaAgendamento(@PathVariable Long idBarbeiro) {
        return ResponseEntity.ok(service.listarPorBarbeiro(idBarbeiro));
    }

    // ✅ MÉTODO BLINDADO (GET /servicos)
    // Resolve o vazamento de dados.
    @GetMapping
    public ResponseEntity<List<DetalhamentoServicoDTO>> listar(
            @RequestParam(required = false) Long barbeiroId,
            @RequestParam(required = false) Long lojaId
    ) {
        // Pega o usuário do Token JWT para segurança caso não venha parâmetro
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();

        // O Service decide: se tem ID, busca do ID. Se não tem, busca do emailLogado.
        // NUNCA mais retorna findAll() global.
        var lista = service.listarComFiltros(barbeiroId, lojaId, emailLogado);

        return ResponseEntity.ok(lista);
    }
}