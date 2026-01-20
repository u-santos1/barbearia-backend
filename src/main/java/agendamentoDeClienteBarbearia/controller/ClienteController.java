package agendamentoDeClienteBarbearia.controller;


import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.model.Cliente;
import agendamentoDeClienteBarbearia.repository.ClienteRepository;
import agendamentoDeClienteBarbearia.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService service;
    private final ClienteRepository repository;

    public ClienteController(ClienteService service, ClienteRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Cliente> cadastrar(@RequestBody @Valid CadastroClienteDTO dados) {
        var cliente = service.cadastrar(dados);
        return ResponseEntity.status(HttpStatus.CREATED).body(cliente);
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
}

    // Geralmente não expomos "listar todos os clientes" publicamente por privacidade,
    // mas para estudo você pode adicionar um @GetMapping se quiser