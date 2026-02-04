package agendamentoDeClienteBarbearia.service;


import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoServicoDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Servico;
import agendamentoDeClienteBarbearia.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


import java.util.List;

@Slf4j // Logs automáticos via Lombok
@Service
@RequiredArgsConstructor // Injeção de dependência via construtor (Best Practice)
public class ServicoService {

    private final ServicoRepository repository;
    private final BarbeiroService barbeiroService; // ✅ Injetado para identificar o dono logado

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
    // 4. LISTAGEM (READ ONLY) - FILTRADA POR DONO
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
        // O buscarPorId pode ser mais flexível se for usado no agendamento pelo cliente,
        // mas para gestão interna, idealmente também validaria o dono.
        var servico = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));
        return new DetalhamentoServicoDTO(servico);
    }

    // ========================================================
    // HELPER: RECUPERA O CONTEXTO SAAS
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