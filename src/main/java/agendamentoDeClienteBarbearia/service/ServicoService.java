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

@Slf4j // Logs automáticos via Lombok
@Service
@RequiredArgsConstructor // Injeção de dependência via construtor (Best Practice)
public class ServicoService {

    private final ServicoRepository repository;
    private final BarbeiroService barbeiroService;       // Usado para identificar usuário logado
    private final BarbeiroRepository barbeiroRepository; // ✅ Usado para buscar entidade crua (fix getDono)

    // ========================================================
    // 1. CADASTRAR (CREATE) - COM ISOLAMENTO DE DADOS
    // ========================================================
    @Transactional
    public DetalhamentoServicoDTO cadastrar(CadastroServicoDTO dados) {
        log.info("Tentando cadastrar novo serviço: {}", dados.nome());

        // 1. Identifica o Dono da Barbearia (Contexto SaaS)
        Barbeiro dono = getDonoLogado();

        String nomeNormalizado = dados.nome().trim();

        // 2. Validação de Duplicidade (Escopo: Apenas serviços deste Dono)
        if (repository.existsByNomeIgnoreCaseAndDonoId(nomeNormalizado, dono.getId())) {
            log.warn("Tentativa de cadastro duplicado rejeitada na barbearia de ID {}: {}", dono.getId(), nomeNormalizado);
            throw new RegraDeNegocioException("Você já possui um serviço cadastrado com este nome.");
        }

        // 3. Criação e Vínculo
        var servico = new Servico(dados);
        servico.setDono(dono); // ✅ VINCULA O SERVIÇO AO DONO!

        repository.save(servico);

        log.info("Serviço '{}' cadastrado com sucesso para o Dono ID: {}", servico.getNome(), dono.getId());

        return new DetalhamentoServicoDTO(servico);
    }

    // ========================================================
    // 2. ATUALIZAR (UPDATE) - BLINDADO
    // ========================================================
    @Transactional
    public DetalhamentoServicoDTO atualizar(Long id, CadastroServicoDTO dados) {
        Barbeiro dono = getDonoLogado();

        // Busca o serviço e garante que ele pertence ao dono logado
        var servico = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        // Segurança: Impede alterar serviço de outro dono (caso alguém tente forçar ID na URL)
        if (!servico.getDono().getId().equals(dono.getId())) {
            throw new RegraDeNegocioException("Acesso negado: Este serviço não pertence à sua barbearia.");
        }

        String novoNome = dados.nome().trim();

        // Validação Inteligente (No escopo do Dono)
        if (!servico.getNome().equalsIgnoreCase(novoNome) &&
                repository.existsByNomeIgnoreCaseAndDonoId(novoNome, dono.getId())) {
            throw new RegraDeNegocioException("Você já tem outro serviço com este nome.");
        }

        // Atualiza campos
        servico.setNome(novoNome);
        servico.setPreco(dados.preco());
        servico.setDescricao(dados.descricao());
        servico.setDuracaoEmMinutos(dados.duracaoEmMinutos());

        repository.save(servico);

        return new DetalhamentoServicoDTO(servico);
    }

    // ========================================================
    // 3. EXCLUIR (SOFT DELETE)
    // ========================================================
    @Transactional
    public void excluir(Long id) {
        Barbeiro dono = getDonoLogado();

        var servico = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        // Segurança
        if (!servico.getDono().getId().equals(dono.getId())) {
            throw new RegraDeNegocioException("Acesso negado.");
        }

        servico.excluir(); // Soft Delete
        log.info("Serviço ID {} inativado pelo Dono ID {}.", id, dono.getId());
    }

    // ========================================================
    // 4. LISTAGEM (READ ONLY) - FILTRADA POR DONO (PAINEL ADMIN)
    // ========================================================
    @Transactional(readOnly = true)
    public List<DetalhamentoServicoDTO> listarAtivos() {
        Barbeiro dono = getDonoLogado();

        // ✅ Busca apenas os serviços DESSA barbearia
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
    // 5. LISTAGEM PÚBLICA (AGENDAMENTO PELO CLIENTE)
    // ========================================================
    @Transactional(readOnly = true)
    public List<DetalhamentoServicoDTO> listarPorBarbeiro(Long idBarbeiro) {
        // ✅ CORREÇÃO CRÍTICA:
        // Usamos o REPOSITORY aqui, pois precisamos da Entidade Barbeiro completa
        // para acessar o método .getDono(). Se usássemos o Service, receberíamos um DTO.
        var barbeiro = barbeiroRepository.findById(idBarbeiro)
                .orElseThrow(() -> new RegraDeNegocioException("Barbeiro não encontrado"));

        // Lógica de herança do SaaS (Dono vs Funcionário)
        Long idDono = (barbeiro.getDono() != null) ? barbeiro.getDono().getId() : barbeiro.getId();

        return repository.findAllByDonoIdAndAtivoTrue(idDono).stream()
                .map(DetalhamentoServicoDTO::new)
                .toList();
    }

    // ========================================================
    // HELPER: RECUPERA O CONTEXTO SAAS
    // ========================================================
    private Barbeiro getDonoLogado() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            // Aqui usamos o service pois precisamos apenas identificar o usuário pelo email
            // (Assumindo que buscarPorEmail retorna entidade ou é tratado internamente)
            Barbeiro usuario = barbeiroService.buscarPorEmail(email);

            // Se quem logou for funcionário, retorna o patrão (Dono)
            // Se quem logou for o patrão, retorna ele mesmo
            return (usuario.getDono() != null) ? usuario.getDono() : usuario;
        } catch (Exception e) {
            log.error("Erro ao identificar usuário logado", e);
            throw new RegraDeNegocioException("Não foi possível identificar o usuário logado.");
        }
    }
}