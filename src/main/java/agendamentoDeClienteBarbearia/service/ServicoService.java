package agendamentoDeClienteBarbearia.service;


import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoServicoDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Servico;
import agendamentoDeClienteBarbearia.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j // Logs automáticos via Lombok
@Service
@RequiredArgsConstructor // Injeção de dependência via construtor (Best Practice)
public class ServicoService {

    private final ServicoRepository repository;

    // ========================================================
    // 1. CADASTRAR (CREATE)
    // ========================================================
    @Transactional
    public DetalhamentoServicoDTO cadastrar(CadastroServicoDTO dados) {
        log.info("Tentando cadastrar novo serviço: {}", dados.nome());

        // Normalização de texto (Trim remove espaços acidentais no começo/fim)
        String nomeNormalizado = dados.nome().trim();

        // 1. Validação de Duplicidade
        if (repository.existsByNomeIgnoreCase(nomeNormalizado)) {
            log.warn("Tentativa de cadastro duplicado rejeitada: {}", nomeNormalizado);
            throw new RegraDeNegocioException("Já existe um serviço cadastrado com este nome.");
        }

        // 2. Criação
        var servico = new Servico(dados); // Usa o construtor inteligente da Entity
        repository.save(servico);

        log.info("Serviço '{}' cadastrado com sucesso. ID: {}", servico.getNome(), servico.getId());

        return new DetalhamentoServicoDTO(servico);
    }

    // ========================================================
    // 2. ATUALIZAR (UPDATE) - CRÍTICO PARA PRODUÇÃO
    // ========================================================
    @Transactional
    public DetalhamentoServicoDTO atualizar(Long id, CadastroServicoDTO dados) {
        var servico = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        String novoNome = dados.nome().trim();

        // Validação Inteligente:
        // Verifica se o nome já existe em OUTRO serviço (não no mesmo que estou editando)
        if (!servico.getNome().equalsIgnoreCase(novoNome) && repository.existsByNomeIgnoreCase(novoNome)) {
            throw new RegraDeNegocioException("Já existe outro serviço com este nome.");
        }

        // Atualiza campos
        servico.setNome(novoNome);
        servico.setPreco(dados.preco());
        servico.setDescricao(dados.descricao());
        servico.setDuracaoEmMinutos(dados.duracaoEmMinutos());

        // O JPA salva automaticamente ao fim da transação, mas save() explícito é seguro
        repository.save(servico);

        return new DetalhamentoServicoDTO(servico);
    }

    // ========================================================
    // 3. EXCLUIR (SOFT DELETE)
    // ========================================================
    @Transactional
    public void excluir(Long id) {
        var servico = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));

        // Soft Delete (Apenas inativa para não quebrar histórico financeiro)
        servico.excluir();
        log.info("Serviço ID {} inativado.", id);
    }

    // ========================================================
    // 4. LISTAGEM (READ ONLY)
    // ========================================================
    @Transactional(readOnly = true)
    public List<DetalhamentoServicoDTO> listarAtivos() {
        return repository.findAllByAtivoTrue().stream()
                .map(DetalhamentoServicoDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public DetalhamentoServicoDTO buscarPorId(Long id) {
        var servico = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));
        return new DetalhamentoServicoDTO(servico);
    }
}