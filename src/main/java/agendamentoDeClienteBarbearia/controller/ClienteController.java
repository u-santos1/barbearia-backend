package agendamentoDeClienteBarbearia.controller;


import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoClienteDTO;
import agendamentoDeClienteBarbearia.model.Cliente;
import agendamentoDeClienteBarbearia.repository.ClienteRepository;
import agendamentoDeClienteBarbearia.service.ClienteService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DetalhamentoClienteDTO> cadastrar(@RequestBody @Valid CadastroClienteDTO dados) {
        var dto = service.cadastrarOuAtualizar(dados);

        var uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(dto.id()).toUri();

        return ResponseEntity.created(uri).body(dto);
    }

    @GetMapping("/recuperar-id")
    public ResponseEntity<Long> recuperarIdPorEmail(@RequestParam String email) {
        // Lógica movida para o service
        Long id = service.buscarIdPorEmail(email);
        return ResponseEntity.ok(id);
    }

    @GetMapping
    public ResponseEntity<List<DetalhamentoClienteDTO>> listarClientes() {
        return ResponseEntity.ok(service.listarTodos());
    }
}