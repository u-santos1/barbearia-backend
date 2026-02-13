package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.*;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;
import agendamentoDeClienteBarbearia.service.AgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService service;

    // =========================================================
    // 🟢 ÁREA PÚBLICA (CLIENTE)
    // =========================================================

    @PostMapping
    public ResponseEntity<DetalhamentoAgendamentoDTO> agendar(
            @RequestBody @Valid AgendamentoDTO dados,
            UriComponentsBuilder uriBuilder) {

        var dto = service.agendar(dados);
        var uri = uriBuilder.path("/agendamentos/{id}").buildAndExpand(dto.id()).toUri();

        return ResponseEntity.created(uri).body(dto);
    }

    /**
     * Busca agendamentos pelo telefone.
     * Rota essencial para a funcionalidade "Meus Horários" do cliente.
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> buscarPorTelefone(
            @RequestParam String telefone) {
        // Limpeza básica antes de enviar pro service (opcional, pois o service já trata)
        String telLimpo = telefone.replaceAll("\\D", "");
        return ResponseEntity.ok(service.buscarPorTelefoneCliente(telLimpo));
    }

    /**
     * Consulta disponibilidade de horários.
     * Usada pelo calendário do frontend para pintar os slots livres.
     */
    @GetMapping("/disponibilidade")
    public ResponseEntity<List<String>> getDisponibilidade(
            @RequestParam Long barbeiroId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam Long servicoId) {

        var horarios = service.consultarDisponibilidade(barbeiroId, data, servicoId);
        return ResponseEntity.ok(horarios);
    }

    /**
     * Cancelamento feito pelo próprio cliente via "Meus Horários".
     */
    @DeleteMapping("/cliente/{id}")
    public ResponseEntity<Void> cancelarPeloCliente(@PathVariable Long id) {
        service.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // 🟠 ÁREA RESTRITA (BARBEIRO / ADMIN)
    // =========================================================

    /**
     * Lista todos os agendamentos da barbearia do dono logado (Visão SaaS).
     */
    @GetMapping("/admin/todos")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarTodos(
            @AuthenticationPrincipal UserDetails user) {
        // O Service usa o email para filtrar apenas a loja desse dono
        return ResponseEntity.ok(service.listarTodosDoDono(user.getUsername()));
    }

    /**
     * Lista agendamentos onde o usuário logado é o prestador de serviço.
     */
    @GetMapping("/meus")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarMeusAgendamentos(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(service.listarMeusAgendamentos(user.getUsername()));
    }

    /**
     * Relatório Financeiro seguro (SaaS).
     * O controller extrai o usuário, o service aplica a segurança e as datas padrão.
     */
    @GetMapping("/admin/financeiro")
    public ResponseEntity<ResumoFinanceiroDTO> relatorioFinanceiro(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        var relatorio = service.gerarRelatorioFinanceiro(user.getUsername(), inicio, fim);
        return ResponseEntity.ok(relatorio);
    }

    /**
     * Cria um bloqueio na agenda (almoço, folga, etc).
     */
    @PostMapping("/bloqueio")
    public ResponseEntity<Void> criarBloqueio(
            @RequestBody @Valid BloqueioDTO dados,
            @AuthenticationPrincipal UserDetails user) {

        service.bloquearHorario(user.getUsername(), dados);
        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // 🔴 AÇÕES DE GERENCIAMENTO (BARBEIRO)
    // =========================================================

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
}