package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoClienteDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Cliente;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repository;
    private final BarbeiroRepository barbeiroRepository;

    // ========================================================
    // 1. UPSERT INTELIGENTE (Funciona para App e Manual)
    // ========================================================
    @Transactional
    public DetalhamentoClienteDTO salvar(CadastroClienteDTO dados, Barbeiro usuarioLogado) {

        // 1. REGRA DE NEGÓCIO: Define quem é o dono
        Barbeiro donoResponsavel = definirDonoDoCliente(dados.donoId(), usuarioLogado);

        // 2. Sanitização
        String emailInput = (dados.email() != null && !dados.email().isBlank())
                ? dados.email().trim().toLowerCase() : null;
        String telefoneInput = limparFormatacao(dados.telefone());

        if (telefoneInput.isEmpty()) {
            throw new RegraDeNegocioException("Telefone é obrigatório.");
        }

        // 3. Busca Estratégica ISOLADA POR SAAS (A Correção está aqui 👇)
        Optional<Cliente> clienteExistente = repository.findByTelefoneAndDono(telefoneInput, donoResponsavel);

        if (clienteExistente.isEmpty() && emailInput != null) {
            // Busca por email também isolada pelo dono 👇
            clienteExistente = repository.findByEmailAndDono(emailInput, donoResponsavel);
        }

        // 4. Decisão
        if (clienteExistente.isPresent()) {
            return atualizar(clienteExistente.get(), dados.nome(), emailInput, telefoneInput, donoResponsavel);
        } else {
            return criar(dados.nome(), emailInput, telefoneInput, donoResponsavel);
        }
    }

    // =============================================================
    // MÉTODO AUXILIAR: Isola a regra de negócio do vínculo da barbearia
    // =============================================================
    private Barbeiro definirDonoDoCliente(Long donoIdDTO, Barbeiro usuarioLogado) {
        // Se veio do Site Público (front-end mandou o donoId)
        if (donoIdDTO != null) {
            return barbeiroRepository.findById(donoIdDTO)
                    .orElseThrow(() -> new RegraDeNegocioException("Dono não encontrado para vínculo."));
        }
        // Se veio do Painel Admin (tem um barbeiro logado fazendo a ação)
        else if (usuarioLogado != null) {
            return usuarioLogado;
        }
        // Bloqueio de segurança
        else {
            throw new RegraDeNegocioException("Não é possível cadastrar cliente sem vincular a uma barbearia.");
        }
    }

    // ========================================================
    // 2. MÉTODOS PRIVADOS (Core Logic)
    // ========================================================

    private DetalhamentoClienteDTO atualizar(Cliente cliente, String novoNome, String novoEmail, String novoTelefone, Barbeiro dono) {
        // 1. Validação de Email Duplicado apenas dentro da barbearia deste DONO
        if (novoEmail != null && !novoEmail.equals(cliente.getEmail())) {
            // 👇 Alterado para usar AndDono
            if (repository.existsByEmailAndDono(novoEmail, dono)) {
                throw new RegraDeNegocioException("Este e-mail já está em uso por outro cliente na sua base.");
            }
            cliente.setEmail(novoEmail);
        }

        // 2. Validação de Telefone Duplicado apenas dentro da barbearia deste DONO
        if (novoTelefone != null && !novoTelefone.equals(cliente.getTelefone())) {
            // 👇 Alterado para usar AndDono
            if (repository.existsByTelefoneAndDono(novoTelefone, dono)) {
                throw new RegraDeNegocioException("Este telefone já pertence a outro cliente cadastrado na sua base.");
            }
            cliente.setTelefone(novoTelefone);
        }

        cliente.setNome(novoNome.trim());

        // Garante que o vínculo com o dono está correto no update
        if (cliente.getDono() == null) {
            cliente.setDono(dono);
        }

        return new DetalhamentoClienteDTO(repository.save(cliente));
    }

    private DetalhamentoClienteDTO criar(String nome, String email, String telefone, Barbeiro dono) {
        // 3. Fail-safe de concorrência limitado ao escopo do DONO
        // 👇 Alterado para usar AndDono
        if (repository.existsByTelefoneAndDono(telefone, dono)) {
            throw new RegraDeNegocioException("Telefone já cadastrado na sua barbearia.");
        }

        Cliente novo = new Cliente();
        novo.setNome(nome.trim());
        novo.setEmail(email);
        novo.setTelefone(telefone);
        novo.setDono(dono);

        return new DetalhamentoClienteDTO(repository.save(novo));
    }

    // ========================================================
    // 3. LEITURAS SEGURAS (SAAS)
    // ========================================================

    @Transactional(readOnly = true)
    public List<DetalhamentoClienteDTO> listarPorDono(Long idDono) {
        return repository.findAllByDonoId(idDono).stream()
                .map(DetalhamentoClienteDTO::new)
                .toList();
    }

    // 🚨 REMOVIDO: listarTodos() -> Causa vazamento de dados entre barbearias.

    @Transactional(readOnly = true)
    public Long buscarIdPorEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return repository.findByEmail(email.trim().toLowerCase())
                .map(Cliente::getId)
                .orElse(null); // Retorna null em vez de erro para o front tratar melhor se for busca opcional
    }

    // Utilitários
    private String limparFormatacao(String dado) {
        return dado == null ? "" : dado.replaceAll("\\D", "");
    }
}