package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoClienteDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.service.BarbeiroService;
import agendamentoDeClienteBarbearia.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;
    private final BarbeiroService barbeiroService;
    private final BarbeiroRepository barbeiroRepository;

    // ========================================================
    // 1. CADASTRAR (Híbrido: Funciona Logado ou Público)
    // ========================================================
    @PostMapping
    public ResponseEntity<DetalhamentoClienteDTO> cadastrar(@RequestBody @Valid CadastroClienteDTO dados) {
        Barbeiro donoResponsavel;

        // 1. Verifica se há alguém logado (Token JWT válido)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean estaLogado = auth != null && auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getName());

        if (estaLogado) {
            // CENÁRIO A: Painel Administrativo (SaaS)
            // O dono é o usuário logado (ou o chefe do barbeiro logado)
            donoResponsavel = getDonoLogado();
        } else {
            // CENÁRIO B: App Público (Anônimo)
            // O JSON precisa dizer para qual barbearia é este cliente
            if (dados.barbeiroId() == null) {
                throw new RegraDeNegocioException("Identificação da barbearia (barbeiroId) é obrigatória para cadastro público.");
            }

            // Busca o barbeiro pelo ID informado no JSON
            var barbeiroAlvo = barbeiroRepository.findById(dados.barbeiroId())
                    .orElseThrow(() -> new RegraDeNegocioException("Barbeiro/Barbearia não encontrado."));

            // Define quem é o Dono daquela barbearia
            donoResponsavel = (barbeiroAlvo.getDono() != null) ? barbeiroAlvo.getDono() : barbeiroAlvo;
        }

        // Chama o Service unificado (Salvar/Atualizar)
        var dto = service.salvar(dados, donoResponsavel);

        var uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(dto.id()).toUri();

        return ResponseEntity.created(uri).body(dto);
    }

    // ========================================================
    // 2. LISTAR (Blindado por Dono - SaaS)
    // ========================================================
    @GetMapping
    public ResponseEntity<List<DetalhamentoClienteDTO>> listar() {
        // 1. Identifica quem está pedindo (Barbeiro ou Dono)
        Barbeiro dono = getDonoLogado();

        // 2. Busca apenas os clientes daquela loja
        var lista = service.listarPorDono(dono.getId());

        return ResponseEntity.ok(lista);
    }

    // ========================================================
    // 3. RECUPERAR ID (Útil para validações no Front)
    // ========================================================
    @GetMapping("/recuperar-id")
    public ResponseEntity<Long> recuperarIdPorEmail(@RequestParam String email) {
        Long id = service.buscarIdPorEmail(email);
        // Retorna 200 com o ID ou null (Frontend decide como tratar se vier vazio)
        return ResponseEntity.ok(id);
    }

    // ========================================================
    // HELPER: Lógica de Segurança
    // ========================================================
    private Barbeiro getDonoLogado() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Barbeiro usuario = barbeiroService.buscarPorEmail(email);

            // Se o usuário for null (token inválido), o service já deve ter tratado,
            // mas por segurança lançamos erro aqui.
            if (usuario == null) throw new RegraDeNegocioException("Usuário não encontrado.");

            // Retorna o Dono da conta (se for funcionário, retorna o chefe)
            return (usuario.getDono() != null) ? usuario.getDono() : usuario;
        } catch (Exception e) {
            throw new RegraDeNegocioException("Não foi possível identificar o usuário logado.");
        }
    }
}