package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.StatusAgendamento;
import agendamentoDeClienteBarbearia.dtos.AgendamentoDTO;
import agendamentoDeClienteBarbearia.dtos.ResumoFinanceiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;
import agendamentoDeClienteBarbearia.model.Agendamento;
import agendamentoDeClienteBarbearia.repository.AgendamentoRepository;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.service.AgendamentoService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/agendamentos")
@CrossOrigin(origins = "*") // Em produção, especifique a origem exata (ex: https://seusite.com)
public class AgendamentoController {

    private final AgendamentoService service;

    // Injeção via construtor (Melhor prática que @Autowired)
    public AgendamentoController(AgendamentoService service) {
        this.service = service;
    }

    // 1. CRIAR AGENDAMENTO
    @PostMapping
    public ResponseEntity<DetalhamentoAgendamentoDTO> agendar(@RequestBody @Valid AgendamentoDTO dados) {
        var agendamento = service.agendar(dados);
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamento);
    }

    // 2. AGENDA DO BARBEIRO
    @GetMapping("/barbeiro/{idBarbeiro}")
    public ResponseEntity<List<Agendamento>> listarAgendaDoBarbeiro(
            @PathVariable Long idBarbeiro,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        // O Controller não sabe como buscar, ele só pede ao service
        var lista = service.listarAgendaDoBarbeiro(idBarbeiro, data);
        return ResponseEntity.ok(lista);
    }

    // 3. ADMIN - LISTAR TODOS
    @GetMapping("/admin/todos")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    // 4. CLIENTE - SEUS AGENDAMENTOS
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(service.listarPorCliente(clienteId));
    }

    // 5. BARBEIRO - MEUS AGENDAMENTOS
    @GetMapping("/meus")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarMeusAgendamentos() {
        // Extraímos o usuário logado aqui (Camada de Segurança/Web)
        var emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(service.listarMeusAgendamentos(emailLogado));
    }

    // 6. BUSCAR DISPONIBILIDADE
    @GetMapping("/disponibilidade")
    public ResponseEntity<List<String>> getDisponibilidade(
            @RequestParam Long barbeiroId,
            @RequestParam Long servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {

        var lista = service.listarHorariosDisponiveis(barbeiroId, servicoId, data);
        return ResponseEntity.ok(lista);
    }

    // --- AÇÕES DE MUDANÇA DE STATUS ---

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        service.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/barbeiro")
    public ResponseEntity<Void> cancelarPeloBarbeiro(@PathVariable Long id) {
        service.cancelarPeloBarbeiro(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmar(@PathVariable Long id) {
        service.confirmar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/concluir")
    public ResponseEntity<Void> concluir(@PathVariable Long id) {
        service.concluir(id);
        return ResponseEntity.ok().build();
    }

    // --- RELATÓRIO FINANCEIRO ---
    @GetMapping("/admin/financeiro")
    public ResponseEntity<ResumoFinanceiroDTO> relatorioFinanceiro() {
        // A lógica pesada saiu daqui e foi para o service
        var relatorio = service.gerarRelatorioFinanceiro();
        return ResponseEntity.ok(relatorio);
    }
}