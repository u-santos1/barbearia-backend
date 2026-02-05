package agendamentoDeClienteBarbearia.controller;


import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;

import agendamentoDeClienteBarbearia.service.ServicoService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;




import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoServicoDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/servicos")
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService service;

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
}