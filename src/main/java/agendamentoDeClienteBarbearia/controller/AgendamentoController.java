package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.AgendamentoDTO;
import agendamentoDeClienteBarbearia.dtos.BloqueioDTO;
import agendamentoDeClienteBarbearia.dtos.RelatorioFinanceiroCompletoDTO;
import agendamentoDeClienteBarbearia.dtos.ResumoFinanceiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.service.AgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @PostMapping
    public ResponseEntity<DetalhamentoAgendamentoDTO> agendar(@RequestBody @Valid AgendamentoDTO dados, UriComponentsBuilder uriBuilder) {
        var dto = service.agendar(dados);
        var uri = uriBuilder.path("/agendamentos/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    // LISTAR AGENDA (Visualização do calendário)
    @GetMapping("/barbeiro/{idBarbeiro}")
    public ResponseEntity<List<String>> listarHorariosDisponiveis(
            @PathVariable Long idBarbeiro,
            @RequestParam Long servicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        var lista = service.listarHorariosDisponiveis(idBarbeiro, servicoId, data);
        return ResponseEntity.ok(lista);
    }


    @GetMapping("/{id}")
    public ResponseEntity<DetalhamentoAgendamentoDTO> buscarPorId(@PathVariable Long id, Authentication authentication) {
        // Passamos o e-mail do usuário logado para garantir que ele só veja se for o dono
        var dto = service.buscarPorId(id, authentication.getName());
        return ResponseEntity.ok(dto);
    }

    // ADMIN - LISTAR TODOS (Filtrado por Dono - SaaS)
    @GetMapping("/admin/todos")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarTodos(@PathVariable Long id) {
        var lista = service.listarTodosDoDono(id);
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarPorCliente(@PathVariable Long clienteId, Authentication authentication) {
        return ResponseEntity.ok(service.listarPorCliente(clienteId, authentication.getName()));
    }

    @GetMapping("/meus")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarMeusAgendamentos() {
        var emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(service.listarMeusAgendamentos(emailLogado));
    }

    // --- AÇÕES ---

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, Authentication authentication) {
        service.cancelar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/barbeiro")
    public ResponseEntity<Void> cancelarPeloBarbeiro(@PathVariable Long id, Authentication authentication) {
        service.cancelarPeloBarbeiro(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmar(@PathVariable Long id, Authentication authentication) {
        service.confirmar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/concluir")
    public ResponseEntity<Void> concluir(@PathVariable Long id, Authentication authentication) {
        service.concluir(id, authentication.getName());
        return ResponseEntity.ok().build();
    }

    // --- RELATÓRIOS ---

    @GetMapping("/admin/financeiro")
    public ResponseEntity<ResumoFinanceiroDTO> relatorioFinanceiro(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
    ) {
        // Seguindo sua risca: buscando o email logado para segurança e passando datas para o service
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        var relatorio = service.gerarRelatorioFinanceiro(emailLogado, inicio, fim);
        return ResponseEntity.ok(relatorio);
    }

    // 🚨 DISPONIBILIDADE PARA O FRONTEND
    @GetMapping("/disponibilidade")
    public ResponseEntity<List<String>> getDisponibilidade(
            @RequestParam Long barbeiroId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam Long servicoId
    ) {
        var horarios = service.consultarDisponibilidade(barbeiroId, data, servicoId);
        return ResponseEntity.ok(horarios);
    }

    @PostMapping("/bloqueio")
    public ResponseEntity<Void> criarBloqueio(
            @RequestBody @Valid BloqueioDTO dados,
            @AuthenticationPrincipal UserDetails userDetails) {
        service.bloquearHorario(userDetails.getUsername(), dados);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> buscarPorTelefone(@RequestParam String telefone, Authentication authentication) {
        String telLimpo = telefone.replaceAll("\\D", "");
        var lista = service.buscarPorTelefoneCliente(telLimpo, authentication.getName());
        return ResponseEntity.ok(lista);
    }

    @DeleteMapping("/cliente/{id}")
    public ResponseEntity<Void> cancelarPeloCliente(@PathVariable Long id, Authentication authentication) {
        service.cancelar(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // Novos métodos de histórico que incluímos na revisão anterior para cobertura total
    @GetMapping("/historico/barbeiro/{barbeiroId}")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarPorBarbeiro(@PathVariable Long barbeiroId, Authentication authentication) {
        return ResponseEntity.ok(service.listarPorBarbeiroId(barbeiroId, authentication.getName()));
    }

    @GetMapping("/admin/dono/{donoId}")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> listarPorDonoId(Authentication authentication) {
        return ResponseEntity.ok(service.listarTodosPorDonoId(authentication.getName()));
    }
    @GetMapping("/barbeiro/{barbeiroId}/agenda")
    public ResponseEntity<List<DetalhamentoAgendamentoDTO>> buscarAgendaPorPeriodo(
            @PathVariable Long barbeiroId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Authentication authentication) {

        // Converte LocalDate (dia) para LocalDateTime (dia com hora)
        // Inicio: 00:00:00 do dia inicial
        // Fim: 23:59:59 do dia final
        var lista = service.listarPorBarbeiroEPeriodo(
                barbeiroId,
                inicio.atStartOfDay(),
                fim.atTime(23, 59, 59),
                        authentication.getName());

        return ResponseEntity.ok(lista);
    }
    @GetMapping("/financeiro/extrato")
    public ResponseEntity<RelatorioFinanceiroCompletoDTO> getExtratoFinanceiro(
            @AuthenticationPrincipal Barbeiro barbeiroLogado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {

        // Supondo que o usuário logado seja o Dono
        var relatorio = service.gerarExtratoFinanceiro(barbeiroLogado.getEmail(), inicio, fim);
        return ResponseEntity.ok(relatorio);
    }
}