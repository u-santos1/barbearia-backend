package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.AgendamentoDTO;
import agendamentoDeClienteBarbearia.dtos.BloqueioDTO;
import agendamentoDeClienteBarbearia.dtos.RelatorioFinanceiroCompletoDTO;
import agendamentoDeClienteBarbearia.dtos.ResumoFinanceiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.service.AgendamentoService;
import agendamentoDeClienteBarbearia.service.GeradorPdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final GeradorPdfService geradorPdfService;

    // =========================================================
    // ROTAS PÚBLICAS (Acessadas pelo Site do Cliente)
    // =========================================================

    @PostMapping
    public ResponseEntity<DetalhamentoAgendamentoDTO> agendar(@RequestBody @Valid AgendamentoDTO dados, UriComponentsBuilder uriBuilder) {
        var dto = service.agendar(dados);
        var uri = uriBuilder.path("/agendamentos/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    @GetMapping("/disponibilidade")
    public ResponseEntity<List<String>> getDisponibilidade(
            @RequestParam Long barbeiroId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam Long servicoId
    ) {
        var horarios = service.consultarDisponibilidade(barbeiroId, data, servicoId);
        return ResponseEntity.ok(horarios);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> buscarPorTelefone(
            @RequestParam String telefone,
            @AuthenticationPrincipal UserDetails userDetails) {

        String telLimpo = telefone.replaceAll("\\D", "");
        // Tolera chamadas do site público (sem login) passando null para o e-mail
        String emailContexto = (userDetails != null) ? userDetails.getUsername() : null;

        var lista = service.buscarPorTelefoneCliente(telLimpo, emailContexto);
        return ResponseEntity.ok(lista);
    }

    @DeleteMapping("/cliente/{id}")
    public ResponseEntity<Void> cancelarPeloCliente(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String emailContexto = (userDetails != null) ? userDetails.getUsername() : null;
        service.cancelar(id, emailContexto);

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // ROTAS PRIVADAS (Acessadas pelo Painel Administrativo)
    // =========================================================

    @GetMapping("/admin/todos")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarTodos(@AuthenticationPrincipal Barbeiro barbeiroLogado) {

        var lista = service.listarTodosDoDono(barbeiroLogado.getId());

        return ResponseEntity.ok(lista);
    }

    @GetMapping("/admin/dono")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarPorDonoId(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.listarTodosPorDonoId(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetalhamentoAgendamentoDTO> buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Barbeiro barbeiroLogado) {
        var dto = service.buscarPorId(id, barbeiroLogado.getId()); // Passa o ID do dono!
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/meus")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarMeusAgendamentos(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.listarMeusAgendamentos(userDetails.getUsername()));
    }

    @GetMapping("/cliente/{clienteId}/historico")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarPorCliente(@PathVariable Long clienteId, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.listarPorCliente(clienteId, userDetails.getUsername()));
    }

    @GetMapping("/barbeiro/{barbeiroId}/agenda")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> buscarAgendaPorPeriodo(
            @PathVariable Long barbeiroId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @AuthenticationPrincipal UserDetails userDetails) {

        var lista = service.listarPorBarbeiroEPeriodo(
                barbeiroId,
                inicio.atStartOfDay(),
                fim.atTime(23, 59, 59),
                userDetails.getUsername()
        );
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/historico/barbeiro/{barbeiroId}")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarPorBarbeiro(@PathVariable Long barbeiroId, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(service.listarPorBarbeiroId(barbeiroId, userDetails.getUsername()));
    }

    // --- AÇÕES ---

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        service.cancelar(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/barbeiro")
    public ResponseEntity<Void> cancelarPeloBarbeiro(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        service.cancelarPeloBarbeiro(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmar(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        service.confirmar(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/concluir")
    public ResponseEntity<Void> concluir(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        service.concluir(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bloqueio")
    public ResponseEntity<Void> criarBloqueio(@RequestBody @Valid BloqueioDTO dados, @AuthenticationPrincipal UserDetails userDetails) {
        service.bloquearHorario(userDetails.getUsername(), dados);
        return ResponseEntity.noContent().build();
    }

    // --- RELATÓRIOS ---

    @GetMapping("/admin/financeiro")
    public ResponseEntity<ResumoFinanceiroDTO> relatorioFinanceiro(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @AuthenticationPrincipal UserDetails userDetails) {

        var relatorio = service.gerarRelatorioFinanceiro(userDetails.getUsername(), inicio, fim);
        return ResponseEntity.ok(relatorio);
    }

    @GetMapping("/financeiro/extrato")
    public ResponseEntity<RelatorioFinanceiroCompletoDTO> getExtratoFinanceiro(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @AuthenticationPrincipal UserDetails userDetails) {

        var relatorio = service.gerarExtratoFinanceiro(userDetails.getUsername(), inicio, fim);
        return ResponseEntity.ok(relatorio);
    }

    @GetMapping("/financeiro/extrato/pdf")
    public ResponseEntity<byte[]> baixarRelatorioPdf(
            @RequestParam String inicio,
            @RequestParam String fim,
            org.springframework.security.core.Authentication authentication) { // <--- Pega quem está logado

        // 1. Pega o email do dono que está logado no momento
        String emailDono = authentication.getName();

        // 2. Converte as datas de String ("2026-05-01") para LocalDate do Java
        java.time.LocalDate dataInicio = java.time.LocalDate.parse(inicio);
        java.time.LocalDate dataFim = java.time.LocalDate.parse(fim);


        RelatorioFinanceiroCompletoDTO dados = service.gerarExtratoFinanceiro(emailDono, dataInicio, dataFim);

        // 4. Gera o PDF
        byte[] relatorioPdf = geradorPdfService.gerarRelatorioFinanceiroPdf(dados, inicio, fim);

        // 5. Retorna forçando o Download no navegador
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-barber-pro.pdf\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(relatorioPdf);
    }
}
