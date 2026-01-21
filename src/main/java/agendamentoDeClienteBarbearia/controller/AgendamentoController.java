package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.AgendamentoDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;
import agendamentoDeClienteBarbearia.repository.AgendamentoRepository;
import agendamentoDeClienteBarbearia.service.AgendamentoService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;


import agendamentoDeClienteBarbearia.dtos.AgendamentoDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;
import agendamentoDeClienteBarbearia.repository.AgendamentoRepository;
import agendamentoDeClienteBarbearia.service.AgendamentoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/agendamentos")
public class AgendamentoController {

    private final AgendamentoService service;
    private final AgendamentoRepository repository;

    public AgendamentoController(AgendamentoService service, AgendamentoRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<DetalhamentoAgendamentoDTO> agendar(@RequestBody @Valid AgendamentoDTO dados) {
        var agendamento = service.agendar(dados);
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamento);
    }

    @GetMapping("/barbeiro/{idBarbeiro}")
    public ResponseEntity<List<agendamentoDeClienteBarbearia.model.Agendamento>> listarAgendaDoBarbeiro(
            @PathVariable Long idBarbeiro,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {

        var inicioDia = data.atStartOfDay();
        var fimDia = data.atTime(LocalTime.MAX);
        var agendamentos = repository.findByBarbeiroIdAndDataHoraInicioBetween(idBarbeiro, inicioDia, fimDia);

        return ResponseEntity.ok(agendamentos);
    }

    // --- AGORA ESTÁ LIMPO E PROTEGIDO PELO SECURITY CONFIG ---
    @GetMapping("/admin/todos")
    public ResponseEntity<?> listarTodos() {
        // Removemos a verificação manual.
        // Se chegou aqui, é porque o SecurityConfig já validou que é ADMIN.

        var todos = repository.findAll();
        var dtos = todos.stream()
                .map(agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO::toDTO)
                .toList();

        return ResponseEntity.ok(dtos);
    }
    // 1. CLIENTE VÊ SEUS PRÓPRIOS AGENDAMENTOS
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<?> listarPorCliente(@PathVariable Long clienteId) {
        var agendamentos = repository.findByClienteIdOrderByDataHoraInicioDesc(clienteId);
        var dtos = agendamentos.stream()
                .map(agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // 2. CLIENTE CANCELA (DELETE)
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity cancelar(@PathVariable Long id) {
        // Usa o service para garantir regra de negócio (se quiser adicionar depois)
        service.cancelar(id);
        return ResponseEntity.noContent().build();
    }@PutMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmar(@PathVariable Long id) {
        service.confirmar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/concluir")
    public ResponseEntity<Void> concluir(@PathVariable Long id) {
        service.concluir(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/barbeiro") // Rota específica para quando o barbeiro cancela
    public ResponseEntity<Void> cancelarPeloBarbeiro(@PathVariable Long id) {
        service.cancelarPeloBarbeiro(id);
        return ResponseEntity.noContent().build();
    }
}