package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoServicoDTO;
import agendamentoDeClienteBarbearia.model.Servico;
import agendamentoDeClienteBarbearia.repository.ServicoRepository;
import agendamentoDeClienteBarbearia.service.ServicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/servicos")
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService service;
    // O 'final' é obrigatório para o @RequiredArgsConstructor injetar o repositório
    private final ServicoRepository repository;

    @PostMapping
    public ResponseEntity<DetalhamentoServicoDTO> cadastrar(@RequestBody @Valid CadastroServicoDTO dados, UriComponentsBuilder uriBuilder) {
        var dto = service.cadastrar(dados);
        var uri = uriBuilder.path("/servicos/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DetalhamentoServicoDTO> atualizar(@PathVariable Long id, @RequestBody @Valid CadastroServicoDTO dados) {
        var dto = service.atualizar(id, dados);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    // Método Legado (Para compatibilidade com agendamento antigo)
    @GetMapping("/barbeiro/{idBarbeiro}")
    public ResponseEntity<List<DetalhamentoServicoDTO>> listarParaAgendamento(@PathVariable Long idBarbeiro) {
        var lista = service.listarPorBarbeiro(idBarbeiro);
        return ResponseEntity.ok(lista);
    }

    // ✅ ÚNICO MÉTODO LISTAR (GET /servicos)
    // Ele resolve tanto a listagem geral quanto os filtros da Home
    @GetMapping
    public ResponseEntity<List<DetalhamentoServicoDTO>> listar(
            @RequestParam(required = false) Long barbeiroId,
            @RequestParam(required = false) Long lojaId
    ) {
        List<Servico> servicos;

        if (barbeiroId != null) {
            // Filtra por barbeiro
            servicos = repository.findAllByBarbeiroId(barbeiroId);
        } else if (lojaId != null) {
            // Filtra por loja (Dono)
            servicos = repository.findAllByLojaId(lojaId);
        } else {
            // Sem filtro: retorna todos
            servicos = repository.findAll();
        }

        var lista = servicos.stream().map(DetalhamentoServicoDTO::new).toList();
        return ResponseEntity.ok(lista);
    }
}