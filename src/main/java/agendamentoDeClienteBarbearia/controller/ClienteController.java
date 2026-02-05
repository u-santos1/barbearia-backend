package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoClienteDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.service.BarbeiroService;
import agendamentoDeClienteBarbearia.service.ClienteService;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException; // Certifique-se de ter essa exception ou similar

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor // Lombok substitui o construtor manual (Código mais limpo)
public class ClienteController {

    private final ClienteService service;
    private final BarbeiroService barbeiroService;
    private final agendamentoDeClienteBarbearia.repository.BarbeiroRepository barbeiroRepository;


    @PostMapping
    public ResponseEntity<DetalhamentoClienteDTO> cadastrar(@RequestBody @Valid CadastroClienteDTO dados) {
        Barbeiro dono;

        var auth = SecurityContextHolder.getContext().getAuthentication();
        boolean estaLogado = auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser");

        if (estaLogado) {
            // Cenário 1: Logado (Pega do Token)
            dono = getDonoLogado();
        } else {
            // Cenário 2: Público (Pega do ID enviado no JSON)
            if (dados.barbeiroId() == null) {
                throw new RegraDeNegocioException("Erro: Identificação da barbearia é obrigatória.");
            }

            // ✅ CORREÇÃO AQUI: Usamos o Repository para pegar a ENTIDADE (que tem .getDono())
            var barbeiroSelecionado = barbeiroRepository.findById(dados.barbeiroId())
                    .orElseThrow(() -> new RegraDeNegocioException("Barbeiro não encontrado"));

            // Agora funciona porque é uma Entidade JPA
            dono = (barbeiroSelecionado.getDono() != null) ? barbeiroSelecionado.getDono() : barbeiroSelecionado;
        }

        var dto = service.cadastrarManual(dados, dono);

        var uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(dto.id()).toUri();

        return ResponseEntity.created(uri).body(dto);
    }

    @GetMapping("/recuperar-id")
    public ResponseEntity<Long> recuperarIdPorEmail(@RequestParam String email) {
        // Idealmente, validar se o email pertence a um cliente deste dono, mas para MVP ok.
        Long id = service.buscarIdPorEmail(email);
        return ResponseEntity.ok(id);
    }

    @GetMapping
    public ResponseEntity<List<DetalhamentoClienteDTO>> listar() {
        // 1. Identifica o contexto (Quem é o dono dos dados?)
        Barbeiro dono = getDonoLogado();

        // 2. Chama o serviço para buscar e converter DTOs
        // O Service que deve chamar o Repository, não o Controller
        var lista = service.listarPorDono(dono.getId());

        return ResponseEntity.ok(lista);
    }

    // ========================================================
    // HELPER: CENTRALIZA A LÓGICA DE DESCOBRIR O DONO
    // ========================================================
    private Barbeiro getDonoLogado() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Barbeiro usuario = barbeiroService.buscarPorEmail(email);

            // Se quem logou for funcionário, retorna o patrão (Dono)
            // Se quem logou for o patrão, retorna ele mesmo
            return (usuario.getDono() != null) ? usuario.getDono() : usuario;
        } catch (Exception e) {
            throw new RegraDeNegocioException("Não foi possível identificar o usuário logado.");
        }
    }
}