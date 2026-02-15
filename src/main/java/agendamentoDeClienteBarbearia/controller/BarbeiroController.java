package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.AtualizacaoBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.service.BarbeiroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/barbeiros")
@RequiredArgsConstructor
public class BarbeiroController {

    private final BarbeiroService service;

    // ================================================================
    // 1. CADASTRO DE DONO (Cria uma nova Barbearia/Conta)
    // ================================================================
    @PostMapping
    public ResponseEntity<DetalhamentoBarbeiroDTO> cadastrarDono(@RequestBody @Valid CadastroBarbeiroDTO dados, UriComponentsBuilder uriBuilder) {
        // O Service retorna a Entidade Barbeiro (com ID gerado e senha hash)
        Barbeiro barbeiro = service.cadastrarDono(dados);

        // Convertemos para DTO para não expor a senha no JSON de resposta
        var dto = new DetalhamentoBarbeiroDTO(barbeiro);

        var uri = uriBuilder.path("/barbeiros/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    // ================================================================
    // 2. CADASTRO DE EQUIPE (Só Dono pode criar funcionário)
    // ================================================================
    @PostMapping("/equipe")
    public ResponseEntity<DetalhamentoBarbeiroDTO> cadastrarFuncionario(@RequestBody @Valid CadastroBarbeiroDTO dados, UriComponentsBuilder uriBuilder) {
        // Recupera quem está logado (o Chefe)
        String emailChefe = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro dono = service.buscarPorEmail(emailChefe);

        // O Service cuida da validação de plano e vínculo
        Barbeiro novoFuncionario = service.cadastrarFuncionario(dados, dono.getId());

        var dto = new DetalhamentoBarbeiroDTO(novoFuncionario);
        var uri = uriBuilder.path("/barbeiros/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    // ================================================================
    // 3. LISTAR EQUIPE (Visão do Admin - Painel de Gestão)
    // ================================================================
    @GetMapping("/equipe")
    public ResponseEntity<List<DetalhamentoBarbeiroDTO>> listarEquipe() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro dono = service.buscarPorEmail(email);

        // O Service já retorna a lista de DTOs pronta
        return ResponseEntity.ok(service.listarEquipe(dono.getId()));
    }

    // ================================================================
    // 4. INATIVAR / DEMITIR (Soft Delete)
    // ================================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro usuarioLogado = service.buscarPorEmail(email);

        service.inativar(id, usuarioLogado.getId());

        return ResponseEntity.noContent().build();
    }

    // ================================================================
    // 5. LISTAGEM PÚBLICA (APP CLIENTE - AGENDAMENTO)
    // ================================================================
    // ✅ CORREÇÃO: Usa o Service blindado. Se não mandar lojaId, retorna vazio.
    @GetMapping
    public ResponseEntity<List<DetalhamentoBarbeiroDTO>> listarBarbeiros(@RequestParam(required = false) Long lojaId) {
        var lista = service.listarPorLoja(lojaId);
        return ResponseEntity.ok(lista);
    }

    // ================================================================
    // 6. ATUALIZAR MEUS DADOS (Perfil)
    // ================================================================
    @PutMapping("/meus-dados")
    public ResponseEntity<DetalhamentoBarbeiroDTO> atualizarPerfil(
            @RequestBody @Valid AtualizacaoBarbeiroDTO dados,
            @AuthenticationPrincipal UserDetails userDetails) {

        // O Service cuida da atualização e já devolve o DTO atualizado
        var response = service.atualizarPerfil(userDetails.getUsername(), dados);

        return ResponseEntity.ok(response);
    }
}