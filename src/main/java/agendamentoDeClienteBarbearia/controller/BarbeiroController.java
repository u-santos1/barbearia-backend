package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.AtualizacaoBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.RelatorioBarbeiroDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.service.BarbeiroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.naming.directory.BasicAttribute;
import java.util.List;

@RestController
@RequestMapping("/barbeiros")
@RequiredArgsConstructor
public class BarbeiroController {

    private final BarbeiroService service;
    private final BarbeiroRepository barbeiroRepository;

    // ================================================================
    // 1. CADASTRO DE DONO (Cria uma nova Barbearia/Conta)
    // ================================================================
    @PostMapping
    public ResponseEntity<DetalhamentoBarbeiroDTO> cadastrarDono(@RequestBody @Valid CadastroBarbeiroDTO dados, UriComponentsBuilder uriBuilder) {

        DetalhamentoBarbeiroDTO dto = service.cadastrarDono(dados);

        var uri = uriBuilder.path("/barbeiros/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    // ================================================================
    // 2. CADASTRO DE EQUIPE (Só Dono pode criar funcionário)
    // ================================================================
    @PostMapping("/equipe")
    public ResponseEntity<DetalhamentoBarbeiroDTO> cadastrarFuncionario(@RequestBody @Valid CadastroBarbeiroDTO dados, UriComponentsBuilder uriBuilder) {
        // Vamos ao cofre, tiramos o "Principal" e fazemos o Cast para (Barbeiro)
        Barbeiro chefeLogado = (Barbeiro) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        DetalhamentoBarbeiroDTO dto = service.cadastrarFuncionario(dados,chefeLogado.getId());


        var uri = uriBuilder.path("/barbeiros/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    // ================================================================
    // 3. LISTAR EQUIPE (Visão do Admin - Painel de Gestão)
    // ================================================================
    @GetMapping("/equipe")
    public ResponseEntity<List<DetalhamentoBarbeiroDTO>> listarEquipe() {
        Barbeiro donoLogado = (Barbeiro) SecurityContextHolder.getContext().getAuthentication().getPrincipal();


        return ResponseEntity.ok(service.listarEquipe(donoLogado.getId()));
    }

    // ================================================================
    // 4. INATIVAR / DEMITIR (Soft Delete)
    // ================================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        Barbeiro donoLogado = (Barbeiro) SecurityContextHolder.getContext().getAuthentication().getPrincipal();


        service.inativar(id, donoLogado.getId());

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
            @AuthenticationPrincipal Barbeiro barbeiroLogado) {


        // Passamos o ID (Long) em vez do Username (String)
        var response = service.atualizarPerfil(barbeiroLogado.getId(), dados);

        return ResponseEntity.ok(response);
    }
    // ================================================================
    // 7. QUEM SOU EU (Consulta de Perfil Atualizado)
    // ================================================================
    @GetMapping("/me")
    public ResponseEntity<DetalhamentoBarbeiroDTO> buscarMeuPerfil() {

        Barbeiro donoLogado = (Barbeiro) SecurityContextHolder.getContext().getAuthentication().getPrincipal();


        return ResponseEntity.ok(new DetalhamentoBarbeiroDTO(donoLogado));
    }
    @GetMapping("/relatorio/barbeiro")
    public List<RelatorioBarbeiroDTO> relatorio(@RequestParam int mes,
                                                @RequestParam int ano,
                                                @AuthenticationPrincipal Barbeiro dono){
        return service.relatorioMensal(dono.getId(),mes,ano);
    }
}