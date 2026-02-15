package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoClienteDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Cliente;
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

    // ========================================================
    // 1. UPSERT INTELIGENTE (Funciona para App e Manual)
    // ========================================================
    @Transactional
    public DetalhamentoClienteDTO salvar(CadastroClienteDTO dados, Barbeiro donoResponsavel) {

        // 1. Sanitização
        String emailInput = (dados.email() != null && !dados.email().isBlank())
                ? dados.email().trim().toLowerCase() : null;
        String telefoneInput = limparFormatacao(dados.telefone());

        if (telefoneInput.isEmpty()) {
            throw new RegraDeNegocioException("Telefone é obrigatório.");
        }

        // 2. Busca Estratégica (Evita duplicatas globais baseadas no telefone)
        // Nota: Em SaaS, decidimos se o cliente é único por LOJA ou GLOBAL.
        // Assumindo GLOBAL (um cliente pode ir em várias barbearias com o mesmo telefone):
        Optional<Cliente> clienteExistente = repository.findByTelefone(telefoneInput);

        // Se achou por telefone, usa ele. Se não, tenta por email (se houver)
        if (clienteExistente.isEmpty() && emailInput != null) {
            clienteExistente = repository.findByEmail(emailInput);
        }

        // 3. Decisão
        if (clienteExistente.isPresent()) {
            return atualizar(clienteExistente.get(), dados.nome(), emailInput, telefoneInput, donoResponsavel);
        } else {
            return criar(dados.nome(), emailInput, telefoneInput, donoResponsavel);
        }
    }

    // ========================================================
    // 2. MÉTODOS PRIVADOS (Core Logic)
    // ========================================================

    private DetalhamentoClienteDTO atualizar(Cliente cliente, String novoNome, String novoEmail, String novoTelefone, Barbeiro dono) {
        // Validação de Email Duplicado em OUTRO cliente
        if (novoEmail != null && !novoEmail.equals(cliente.getEmail())) {
            if (repository.existsByEmail(novoEmail)) {
                throw new RegraDeNegocioException("Este e-mail já está em uso por outro cliente.");
            }
            cliente.setEmail(novoEmail);
        }

        // 🚨 CORREÇÃO CRÍTICA: Validação de Telefone Duplicado em OUTRO cliente
        if (!novoTelefone.equals(cliente.getTelefone())) {
            if (repository.existsByTelefone(novoTelefone)) {
                throw new RegraDeNegocioException("Este telefone já pertence a outro cliente cadastrado.");
            }
            cliente.setTelefone(novoTelefone);
        }

        cliente.setNome(novoNome.trim());

        // Opcional: Se o cliente foi criado em outra barbearia e agora está vindo nesta,
        // você pode querer atualizar o vínculo ou manter o histórico.
        // Se o sistema for "O cliente pertence a quem cadastrou primeiro", não mexa no dono.
        // Se for "O cliente pertence à loja atual", atualize:
        if (cliente.getDono() == null && dono != null) {
            cliente.setDono(dono);
        }

        return new DetalhamentoClienteDTO(repository.save(cliente));
    }

    private DetalhamentoClienteDTO criar(String nome, String email, String telefone, Barbeiro dono) {
        // Fail-safe de concorrência
        if (repository.existsByTelefone(telefone)) {
            throw new RegraDeNegocioException("Telefone já cadastrado.");
        }

        Cliente novo = new Cliente();
        novo.setNome(nome.trim());
        novo.setEmail(email);
        novo.setTelefone(telefone);
        novo.setDono(dono); // ✅ Agora todo cliente nasce com um pai (ou null se for app público global)

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