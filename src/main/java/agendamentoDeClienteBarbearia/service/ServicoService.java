package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoServicoDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Servico;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository repository;
    private final BarbeiroService barbeiroService;
    private final BarbeiroRepository barbeiroRepository;

    // ========================================================
    // 1. CADASTRAR (CREATE)
    // ========================================================
    @Transactional
    public DetalhamentoServicoDTO cadastrar(CadastroServicoDTO dados) {
        log.info("Tentando cadastrar novo serviço: {}", dados.nome());

        Barbeiro dono = getDonoLogado();
        String nomeNormalizado = dados.nome().trim();

        if (repository.existsByNomeIgnoreCaseAndDonoId(nomeNormalizado, dono.getId())) {
            log.warn("Tentativa duplicada: {}", nomeNormalizado);
            throw new RegraDeNegocioException("Você já possui um serviço cadastrado com este nome.");
        }

        var servico = new Servico(dados);
        servico.setDono(dono);
        servico.setAtivo(true); // ✅ Garante que o serviço nasce ativo

        repository.save(servico);
        log.info("Serviço '{}' cadastrado para Dono ID: {}", servico.getNome(), dono.getId());

        return new DetalhamentoServicoDTO(servico);
    }

    // ========================================================
    // 2. ATUALIZAR (UPDATE)
    // ========================================================
    @Transactional
    public DetalhamentoServicoDTO atualizar(Long id, CadastroServicoDTO dados) {
        Barbeiro dono = getDonoLogado();

        var servico = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        if (!servico.getDono().getId().equals(dono.getId())) {
            throw new RegraDeNegocioException("Acesso negado: Este serviço não pertence à sua barbearia.");
        }

        String novoNome = dados.nome().trim();

        // Verifica se mudou o nome E se o novo nome já existe
        if (!servico.getNome().equalsIgnoreCase(novoNome) &&
                repository.existsByNomeIgnoreCaseAndDonoId(novoNome, dono.getId())) {
            throw new RegraDeNegocioException("Você já tem outro serviço com este nome.");
        }

        servico.setNome(novoNome);
        servico.setPreco(dados.preco());
        servico.setDescricao(dados.descricao());
        servico.setDuracaoEmMinutos(dados.duracaoEmMinutos());

        return new DetalhamentoServicoDTO(servico);
    }

    // ========================================================
    // 3. EXCLUIR (SOFT DELETE - CORRIGIDO)
    // ========================================================
    @Transactional
    public void excluir(Long id) {
        Barbeiro dono = getDonoLogado();

        var servico = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        if (!servico.getDono().getId().equals(dono.getId())) {
            throw new RegraDeNegocioException("Acesso negado.");
        }

        // ✅ FIX CRÍTICO: Soft Delete
        // Não apaga o registro, apenas esconde. Isso evita o erro de chave estrangeira (Erro 500).
        servico.setAtivo(false);

        log.info("Serviço ID {} inativado (Soft Delete) pelo Dono ID {}.", id, dono.getId());
    }

    // ========================================================
    // 4. LISTAGEM (READ ONLY) - FILTRADA POR DONO
    // ========================================================
    @Transactional(readOnly = true)
    // ⚠️ Renomeei de 'listarAtivos' para 'listarMeusServicos' para bater com seu Controller
    public List<DetalhamentoServicoDTO> listarMeusServicos() {
        Barbeiro dono = getDonoLogado();
        // Só retorna os ativos, então o item excluído some da lista
        return repository.findAllByDonoIdAndAtivoTrue(dono.getId()).stream()
                .map(DetalhamentoServicoDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public DetalhamentoServicoDTO buscarPorId(Long id) {
        var servico = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));
        return new DetalhamentoServicoDTO(servico);
    }

    // ========================================================
    // 5. LISTAGEM PÚBLICA
    // ========================================================
    @Transactional(readOnly = true)
    public List<DetalhamentoServicoDTO> listarPorBarbeiro(Long idBarbeiro) {
        var barbeiro = barbeiroRepository.findById(idBarbeiro)
                .orElseThrow(() -> new RegraDeNegocioException("Barbeiro não encontrado"));

        Long idDono = (barbeiro.getDono() != null) ? barbeiro.getDono().getId() : barbeiro.getId();

        return repository.findAllByDonoIdAndAtivoTrue(idDono).stream()
                .map(DetalhamentoServicoDTO::new)
                .toList();
    }

    // ========================================================
    // HELPER
    // ========================================================
    private Barbeiro getDonoLogado() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Barbeiro usuario = barbeiroService.buscarPorEmail(email);
            return (usuario.getDono() != null) ? usuario.getDono() : usuario;
        } catch (Exception e) {
            log.error("Erro ao identificar usuário logado", e);
            throw new RegraDeNegocioException("Não foi possível identificar o usuário logado.");
        }
    }
}