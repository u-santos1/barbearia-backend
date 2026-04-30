package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.AtualizacaoBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.BarbeiroPublicoDTO;
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
    public ResponseEntity<DetalhamentoBarbeiroDTO> cadastrarFuncionario(
            @RequestBody @Valid CadastroBarbeiroDTO dados,
            UriComponentsBuilder uriBuilder,
            @AuthenticationPrincipal Barbeiro chefeLogado) { // Padrão Limpo

        DetalhamentoBarbeiroDTO dto = service.cadastrarFuncionario(dados, chefeLogado.getId());
        var uri = uriBuilder.path("/barbeiros/{id}").buildAndExpand(dto.id()).toUri();
        return ResponseEntity.created(uri).body(dto);
    }

    // ================================================================
    // 3. LISTAR EQUIPE (Visão do Admin - Painel de Gestão)
    // ================================================================
    @GetMapping("/equipe")
    public ResponseEntity<List<DetalhamentoBarbeiroDTO>> listarEquipe(@AuthenticationPrincipal Barbeiro donoLogado) {
        return ResponseEntity.ok(service.listarEquipe(donoLogado.getId()));
    }

    // ================================================================
    // 4. INATIVAR / DEMITIR (Soft Delete)
    // ================================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(
            @PathVariable Long id,
            @AuthenticationPrincipal Barbeiro donoLogado) {

        service.inativar(id, donoLogado.getId());
        return ResponseEntity.noContent().build();
    }

    // ================================================================
    // 5. LISTAGEM PÚBLICA (APP CLIENTE - AGENDAMENTO)
    // ================================================================
    // ✅ SEGURANÇA: Agora retorna BarbeiroPublicoDTO.
    // Planos, e-mails e datas de criação não vazam mais na internet!
    @GetMapping
    public ResponseEntity<List<BarbeiroPublicoDTO>> listarBarbeiros(@RequestParam(required = false) Long lojaId) {
        // Se o seu service ainda estiver retornando DetalhamentoBarbeiroDTO,
        // você precisa atualizar o método listarPorLoja() no BarbeiroService
        // para retornar List<BarbeiroPublicoDTO>.
        List<BarbeiroPublicoDTO> lista = service.listarPorLoja(lojaId);
        return ResponseEntity.ok(lista);
    }

    // ================================================================
    // 6. ATUALIZAR MEUS DADOS (Perfil)
    // ================================================================
    @PutMapping("/meus-dados")
    public ResponseEntity<DetalhamentoBarbeiroDTO> atualizarPerfil(
            @RequestBody @Valid AtualizacaoBarbeiroDTO dados,
            @AuthenticationPrincipal Barbeiro barbeiroLogado) {

        var response = service.atualizarPerfil(barbeiroLogado.getId(), dados);
        return ResponseEntity.ok(response);
    }

    // ================================================================
    // 7. QUEM SOU EU (Consulta de Perfil Atualizado)
    // ================================================================
    @GetMapping("/me")
    public ResponseEntity<DetalhamentoBarbeiroDTO> buscarMeuPerfil(@AuthenticationPrincipal Barbeiro donoLogado) {
        return ResponseEntity.ok(new DetalhamentoBarbeiroDTO(donoLogado));
    }

    // ================================================================
    // 8. RELATÓRIOS
    // ================================================================
    @GetMapping("/relatorio/barbeiro")
    public ResponseEntity<List<RelatorioBarbeiroDTO>> relatorio(@RequestParam int mes,
                                                                @RequestParam int ano,
                                                                @AuthenticationPrincipal Barbeiro dono){
        // Envolvi no ResponseEntity.ok() para manter o padrão de retorno da API
        return ResponseEntity.ok(service.relatorioMensal(dono.getId(),mes,ano));
    }
}