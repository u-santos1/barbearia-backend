package agendamentoDeClienteBarbearia.controller;


import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;

import agendamentoDeClienteBarbearia.model.Servico;
import agendamentoDeClienteBarbearia.repository.ServicoRepository;
import agendamentoDeClienteBarbearia.service.ServicoService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoServicoDTO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.util.UriComponentsBuilder;


@RestController
@RequestMapping("/servicos")
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService service;
    private ServicoRepository repository;

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

    @GetMapping
    public ResponseEntity<List<DetalhamentoServicoDTO>> listar() {
        return ResponseEntity.ok(service.listarAtivos());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/barbeiro/{idBarbeiro}")
    public ResponseEntity<List<DetalhamentoServicoDTO>> listarParaAgendamento(@PathVariable Long idBarbeiro) {
        var lista = service.listarPorBarbeiro(idBarbeiro);
        return ResponseEntity.ok(lista);
    }
    @GetMapping
    public ResponseEntity<List<DetalhamentoServicoDTO>> listar(
            @RequestParam(required = false) Long barbeiroId,
            @RequestParam(required = false) Long lojaId
    ) {
        List<Servico> servicos;

        if (barbeiroId != null) {
            // Se passar ID de barbeiro, tenta buscar por ele
            servicos = repository.findAllByBarbeiroId(barbeiroId);
        } else if (lojaId != null) {
            // ✅ CORREÇÃO: Agora busca filtrado pelo ID do Dono (Loja)
            // O repositório agora sabe que lojaId = donoId
            servicos = repository.findAllByLojaId(lojaId);
        } else {
            // Se não passar nada, traz tudo (mas idealmente deve-se evitar isso em produção)
            servicos = repository.findAll();
        }

        var lista = servicos.stream().map(DetalhamentoServicoDTO::new).toList();
        return ResponseEntity.ok(lista);
    }
}