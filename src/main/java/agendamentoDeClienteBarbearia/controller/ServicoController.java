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
@CrossOrigin(origins = "*")
public class ServicoController {

    private final ServicoService service;

    public ServicoController(ServicoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Servico> cadastrar(@RequestBody @Valid CadastroServicoDTO dados) {
        var servico = service.cadastrar(dados);
        return ResponseEntity.status(HttpStatus.CREATED).body(servico);
    }

    @GetMapping
    public ResponseEntity<List<Servico>> listar() {
        // Service deve retornar apenas serviços ATIVOS
        return ResponseEntity.ok(service.listarAtivos());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        // Soft Delete (apenas desativa)
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}