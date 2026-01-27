package agendamentoDeClienteBarbearia.controller;


import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoClienteDTO;
import agendamentoDeClienteBarbearia.model.Cliente;
import agendamentoDeClienteBarbearia.repository.ClienteRepository;
import agendamentoDeClienteBarbearia.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    private final ClienteService service;
    private final ClienteRepository repository;

    public ClienteController(ClienteService service, ClienteRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<DetalhamentoClienteDTO> cadastrar(@RequestBody @Valid CadastroClienteDTO dados) {

        // Toda a regra de "buscar por email/telefone" e "atualizar ou criar" está aqui dentro:
        var dto = service.cadastrarOuAtualizar(dados);

        // Retornamos 200 OK porque pode ter sido uma atualização (Upsert)
        return ResponseEntity.ok(dto);
    }


    @GetMapping("/recuperar-id")
    public ResponseEntity<?> recuperarIdPorEmail(@RequestParam String email) {
        var cliente = repository.findByEmail(email);

        if (cliente.isPresent()) {
            // Retorna apenas o ID para o front
            return ResponseEntity.ok(cliente.get().getId());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Email não encontrado.");
        }
    }
    @GetMapping
    public ResponseEntity<List<Cliente>> listarClientes() {
        // Adicione @PreAuthorize("hasAuthority('Dono')") se quiser proteger
        return ResponseEntity.ok(repository.findAll());
    }
}

    // Geralmente não expomos "listar todos os clientes" publicamente por privacidade,
    // mas para estudo você pode adicionar um @GetMapping se quiser