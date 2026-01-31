package agendamentoDeClienteBarbearia.controller;


import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoClienteDTO;

import agendamentoDeClienteBarbearia.service.ClienteService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;


import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clientes")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;

    @PostMapping
    public ResponseEntity<DetalhamentoClienteDTO> cadastrar(@RequestBody @Valid CadastroClienteDTO dados) {
        var dto = service.cadastrarOuAtualizar(dados);

        var uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(dto.id()).toUri();

        return ResponseEntity.created(uri).body(dto);
    }

    @GetMapping("/recuperar-id")
    public ResponseEntity<Long> recuperarIdPorEmail(@RequestParam String email) {
        Long id = service.buscarIdPorEmail(email);
        return ResponseEntity.ok(id);
    }

    @GetMapping
    public ResponseEntity<List<DetalhamentoClienteDTO>> listarClientes() {
        return ResponseEntity.ok(service.listarTodos());
    }
}