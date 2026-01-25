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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;




@RestController
@RequestMapping("/agendamentos")
public class AgendamentoController {

    private final AgendamentoService service;
    private final AgendamentoRepository repository;
    private final BarbeiroRepository barbeiroRepository;

    public AgendamentoController(AgendamentoService service, AgendamentoRepository repository, BarbeiroRepository barbeiroRepository) {
        this.service = service;
        this.repository = repository;
        this.barbeiroRepository = barbeiroRepository;
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

        var inicioDia = data.atStartOfDay();
        var fimDia = data.atTime(LocalTime.MAX);
        var agendamentos = repository.findByBarbeiroIdAndDataHoraInicioBetween(idBarbeiro, inicioDia, fimDia);

        return ResponseEntity.ok(agendamentos);
    }

    // 3. ADMIN - LISTAR TODOS
    @GetMapping("/admin/todos")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarTodos() {
        var todos = repository.findAll();
        var dtos = todos.stream()
                .map(DetalhamentoAgendamentoDTO::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // 4. CLIENTE - SEUS AGENDAMENTOS
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarPorCliente(@PathVariable Long clienteId) {
        var agendamentos = repository.findByClienteIdOrderByDataHoraInicioDesc(clienteId);
        var dtos = agendamentos.stream()
                .map(DetalhamentoAgendamentoDTO::toDTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    // 5. BARBEIRO - MEUS AGENDAMENTOS
    @GetMapping("/meus")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarMeusAgendamentos() {
        var emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        var barbeiro = barbeiroRepository.findByEmail(emailLogado);

        if (barbeiro.isPresent()) {
            var agendamentos = repository.findByBarbeiroIdOrderByDataHoraInicioDesc(barbeiro.get().getId());
            var dtos = agendamentos.stream().map(DetalhamentoAgendamentoDTO::toDTO).toList();
            return ResponseEntity.ok(dtos);
        }
        return ResponseEntity.notFound().build();
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
    @Transactional
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
        List<Agendamento> todos = repository.findAll();

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal casa = BigDecimal.ZERO;
        BigDecimal repasse = BigDecimal.ZERO;
        int qtdConcluidos = 0;

        for (Agendamento a : todos) {
            // Verifica se o status é CONCLUIDO usando o ENUM
            if (a.getStatus() == StatusAgendamento.CONCLUIDO) {
                qtdConcluidos++;

                BigDecimal valor = a.getServico().getPreco();

                // Lógica de comissão com fallback para 50%
                Double comissaoDouble = a.getBarbeiro().getComissaoPorcentagem() != null
                        ? a.getBarbeiro().getComissaoPorcentagem()
                        : 50.0;

                BigDecimal porcentagemComissao = BigDecimal.valueOf(comissaoDouble)
                        .divide(BigDecimal.valueOf(100));

                BigDecimal valorBarbeiro = valor.multiply(porcentagemComissao);
                BigDecimal valorCasa = valor.subtract(valorBarbeiro);

                total = total.add(valor);
                repasse = repasse.add(valorBarbeiro);
                casa = casa.add(valorCasa);
            }
        }

        return ResponseEntity.ok(new ResumoFinanceiroDTO(
                total.doubleValue(),
                casa.doubleValue(),
                repasse.doubleValue(),
                qtdConcluidos
        ));
    }
}