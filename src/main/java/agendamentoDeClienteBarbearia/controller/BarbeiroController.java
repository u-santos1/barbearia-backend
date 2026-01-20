package agendamentoDeClienteBarbearia.controller;


import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.service.BarbeiroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/barbeiros")
public class BarbeiroController {

    private final BarbeiroService service;
    private final BarbeiroRepository repository; // Usamos o repository direto apenas para leituras (GET)

    public BarbeiroController(BarbeiroService service, BarbeiroRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Barbeiro> cadastrar(@RequestBody @Valid CadastroBarbeiroDTO dados) {
        var barbeiro = service.cadastrar(dados);
        // Retorna status 201 (Created)
        return ResponseEntity.status(HttpStatus.CREATED).body(barbeiro);
    }

    @GetMapping
    public ResponseEntity<List<Barbeiro>> listar() {
        // Para o frontend popular o <select> de barbeiros
        return ResponseEntity.ok(repository.findAll());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}