package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoClienteDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Cliente;
import agendamentoDeClienteBarbearia.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j // Logs para monitoramento
@Service
@RequiredArgsConstructor // Injeção limpa via construtor
public class ClienteService {

    private final ClienteRepository repository;

    // ========================================================
    // CADASTRAR OU ATUALIZAR (UPSERT INTELIGENTE)
    // ========================================================
    @Transactional
    public DetalhamentoClienteDTO cadastrarOuAtualizar(CadastroClienteDTO dados) {

        // 1. Sanitização (Limpeza de Dados)
        // Remove espaços do email e coloca em minúsculo
        String emailInput = (dados.email() != null && !dados.email().isBlank())
                ? dados.email().trim().toLowerCase()
                : null;

        // Remove tudo que não for número do telefone (ex: (11) 9... vira 119...)
        String telefoneInput = limparFormatacao(dados.telefone());

        if (telefoneInput.isEmpty()) {
            throw new RegraDeNegocioException("Telefone é obrigatório.");
        }

        // 2. Estratégia de Busca (Tentativa de Match)
        Cliente clienteExistente = null;

        // Prioridade A: Busca por Email (Identificador Forte)
        if (emailInput != null) {
            clienteExistente = repository.findByEmail(emailInput).orElse(null);
        }

        // Prioridade B: Busca por Telefone (Se não achou por email)
        if (clienteExistente == null) {
            clienteExistente = repository.findByTelefone(telefoneInput).orElse(null);
        }

        // 3. Decisão: Criar ou Atualizar?
        if (clienteExistente != null) {
            log.info("Cliente existente encontrado (ID: {}). Atualizando dados...", clienteExistente.getId());
            return atualizarCliente(clienteExistente, dados.nome(), emailInput, telefoneInput);
        } else {
            log.info("Novo cliente identificado. Criando cadastro...");
            return criarCliente(dados.nome(), emailInput, telefoneInput);
        }
    }

    // --- Métodos Privados para Organização ---

    private DetalhamentoClienteDTO atualizarCliente(Cliente cliente, String novoNome, String novoEmail, String novoTelefone) {
        // Validação de Conflito de Email:
        // Se o cliente mudou o email, verifica se esse novo email já não é de OUTRA pessoa.
        if (novoEmail != null && !novoEmail.equals(cliente.getEmail())) {
            boolean emailEmUso = repository.existsByEmail(novoEmail);
            if (emailEmUso) {
                throw new RegraDeNegocioException("Este e-mail já pertence a outro cliente.");
            }
            cliente.setEmail(novoEmail);
        }

        // Atualiza dados básicos
        cliente.setNome(novoNome.trim());

        // Se mudou o telefone, atualiza (Cuidado: validar se telefone já existe em outro ID seria bom aqui também)
        cliente.setTelefone(novoTelefone);

        // O JPA faz o update automático (Dirty Checking), mas save explícito é boa prática
        repository.save(cliente);

        return new DetalhamentoClienteDTO(cliente);
    }

    private DetalhamentoClienteDTO criarCliente(String nome, String email, String telefone) {
        // Validação Final (Fail-safe): Garante que o telefone não existe mesmo
        // (Pode ter sido criado milissegundos atrás por outra request concorrente)
        if (repository.existsByTelefone(telefone)) {
            throw new RegraDeNegocioException("Telefone já cadastrado. Tente buscar o cliente novamente.");
        }

        Cliente novo = new Cliente();
        novo.setNome(nome.trim());
        novo.setEmail(email);
        novo.setTelefone(telefone);

        repository.save(novo);
        return new DetalhamentoClienteDTO(novo);
    }

    // ========================================================
    // LEITURAS (READ-ONLY PARA PERFORMANCE)
    // ========================================================

    @Transactional(readOnly = true)
    public Long buscarIdPorEmail(String email) {
        if (email == null || email.isBlank()) return null;

        return repository.findByEmail(email.trim().toLowerCase())
                .map(Cliente::getId)
                .orElseThrow(() -> new RegraDeNegocioException("Cliente não encontrado com o email: " + email));
    }

    @Transactional(readOnly = true)
    public List<DetalhamentoClienteDTO> listarTodos() {
        // ⚠️ ALERTA DE PRODUÇÃO:
        // Se tiver 50.000 clientes, isso trava o sistema.
        // O ideal aqui seria usar paginação (Pageable).
        // Mantive a lista para compatibilidade, mas considere mudar para findAll(Pageable).
        return repository.findAll().stream()
                .map(DetalhamentoClienteDTO::new)
                .toList();
    }

    // Utilitário de Limpeza
    private String limparFormatacao(String dado) {
        if (dado == null) return "";
        // Regex: Substitui tudo que NÃO for dígito (0-9) por vazio
        return dado.replaceAll("\\D", "");
    }
}