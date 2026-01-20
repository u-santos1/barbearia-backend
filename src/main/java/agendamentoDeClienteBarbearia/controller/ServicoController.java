package agendamentoDeClienteBarbearia.controller;


import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;
import agendamentoDeClienteBarbearia.repository.ServicoRepository;
import agendamentoDeClienteBarbearia.model.Servico;
import agendamentoDeClienteBarbearia.service.ServicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/servicos")
public class ServicoController {

    private final ServicoService service;
    private final ServicoRepository repository;

    public ServicoController(ServicoService service, ServicoRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Servico> cadastrar(@RequestBody @Valid CadastroServicoDTO dados) {
        var servico = service.cadastrar(dados);
        return ResponseEntity.status(HttpStatus.CREATED).body(servico);
    }

    @GetMapping
    public ResponseEntity<List<Servico>> listar() {
        // O frontend precisa disso para mostrar a lista de preços e opções
        return ResponseEntity.ok(repository.findAll());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}