package agendamentoDeClienteBarbearia.controller;


import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
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
@CrossOrigin(origins = "*")
public class BarbeiroController {

    private final BarbeiroService service;

    public BarbeiroController(BarbeiroService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DetalhamentoBarbeiroDTO> cadastrar(@RequestBody @Valid CadastroBarbeiroDTO dados) {
        var barbeiro = service.cadastrar(dados); // Service deve retornar DTO, ou converta aqui
        return ResponseEntity.status(HttpStatus.CREATED).body(new DetalhamentoBarbeiroDTO(barbeiro));
    }

    @GetMapping
    public ResponseEntity<List<DetalhamentoBarbeiroDTO>> listar() {
        // O Service que busca, filtra os ativos e converte para DTO
        return ResponseEntity.ok(service.listarTodos());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        // Mudamos de "excluir" para "inativar" (Soft Delete)
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }
}