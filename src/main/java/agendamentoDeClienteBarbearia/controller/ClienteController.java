package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoClienteDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.service.BarbeiroService;
import agendamentoDeClienteBarbearia.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
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

        // 1. A caixa agora é a Entidade pura que o seu Service exige!
        Barbeiro donoResponsavel;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean estaLogado = auth != null && auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getName());

        if (estaLogado) {
            // CENÁRIO A (Logado):
            // 1º Pegamos o DTO do dono logado (a ficha enxuta)
            DetalhamentoBarbeiroDTO donoDto = getDonoLogado();

            // 2º Vamos no banco e buscamos a Entidade "gorda" usando o ID do DTO.
            // Agora sim, estamos guardando um Barbeiro dentro da variável Barbeiro!
            donoResponsavel = barbeiroRepository.findById(donoDto.id())
                    .orElseThrow(() -> new RegraDeNegocioException("Dono não encontrado no banco de dados."));
        } else {
            // CENÁRIO B (Público):
            if (dados.barbeiroId() == null) {
                throw new RegraDeNegocioException("Identificação da barbearia (barbeiroId) é obrigatória para cadastro público.");
            }

            var barbeiroAlvo = barbeiroRepository.findById(dados.barbeiroId())
                    .orElseThrow(() -> new RegraDeNegocioException("Barbeiro/Barbearia não encontrado."));

            // Como a variável donoResponsavel é do tipo Barbeiro, e o getDono() também é Barbeiro,
            // a gente só pluga direto! Não precisa de 'new DTO' nenhum aqui.
            donoResponsavel = (barbeiroAlvo.getDono() != null) ? barbeiroAlvo.getDono() : barbeiroAlvo;
        }

        // 🎯 O Encaixe Perfeito: O Service pede um 'Barbeiro' e a nossa variável é um 'Barbeiro'.
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
        DetalhamentoBarbeiroDTO dono = getDonoLogado();

        // 2. Busca apenas os clientes daquela loja
        var lista = service.listarPorDono(dono.id());

        return ResponseEntity.ok(lista);
    }

    // ========================================================
    // 3. RECUPERAR ID (Útil para validações no Front)
    // ========================================================
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/recuperar-id")
    public ResponseEntity<Long> recuperarIdPorEmail(@RequestParam String email) {
        Long id = service.buscarIdPorEmail(email);
        // Retorna 200 com o ID ou null (Frontend decide como tratar se vier vazio)
        return ResponseEntity.ok(id);
    }

    // ========================================================
    // HELPER: Lógica de Segurança
    // ========================================================
    @Transactional(readOnly = true)
    private DetalhamentoBarbeiroDTO getDonoLogado() {
        try {
            // 1. Descobre quem está logado pelo Token JWT
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            DetalhamentoBarbeiroDTO usuarioLogado = barbeiroService.buscarPorEmail(email);

            if (usuarioLogado == null) {
                throw new RegraDeNegocioException("Usuário não encontrado no token.");
            }

            // 2. A MÁGICA DA RESOLUÇÃO ACONTECE AQUI
            // Verifica se o usuário logado tem um chefe (se o donoId não é nulo)
            if (usuarioLogado.donoId() != null) {
                // É FUNCIONÁRIO! Nós temos o ID do chefe, então vamos buscar o chefe no banco.
                // (Presumindo que seu barbeiroService tenha um buscarPorId que retorna o DTO)
                return barbeiroService.buscarPorId(usuarioLogado.donoId());
            }

            // 3. Se o donoId for null, significa que a pessoa logada já é o Dono!
            // Então devolvemos ela mesma.
            return usuarioLogado;

        } catch (Exception e) {
            throw new RegraDeNegocioException("Não foi possível identificar o usuário logado.");
        }
    }
}