package agendamentoDeClienteBarbearia.service;


import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoClienteDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import jakarta.transaction.Transactional;
import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.repository.ClienteRepository;
import agendamentoDeClienteBarbearia.model.Cliente;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DetalhamentoClienteDTO cadastrarOuAtualizar(CadastroClienteDTO dados) {

        // Normaliza entradas (evita NullPointerException em strings vazias)
        String emailInput = (dados.email() != null && !dados.email().isBlank()) ? dados.email().trim() : null;
        String telefoneInput = dados.telefone().trim();

        Cliente clienteFinal = null;

        // 1. Tenta achar pelo E-mail (Prioridade Máxima: Identificador mais forte)
        if (emailInput != null) {
            clienteFinal = repository.findByEmail(emailInput).orElse(null);
        }

        // 2. Se não achou pelo e-mail, tenta pelo telefone
        if (clienteFinal == null) {
            clienteFinal = repository.findByTelefone(telefoneInput).orElse(null);
        }

        if (clienteFinal != null) {
            // --- ATUALIZAÇÃO (UP) ---

            // 🚨 BLINDAGEM DE CONFLITO:
            // Se encontrei o cliente pelo telefone, mas ele mandou um e-mail novo...
            // Preciso garantir que esse e-mail novo não é de OUTRA pessoa.
            if (emailInput != null && !emailInput.equals(clienteFinal.getEmail())) {
                boolean emailJaExiste = repository.existsByEmail(emailInput);
                if (emailJaExiste) {
                    throw new RegraDeNegocioException("Este e-mail já pertence a outro cliente cadastrado.");
                }
                clienteFinal.setEmail(emailInput);
            }

            clienteFinal.setNome(dados.nome());
            clienteFinal.setTelefone(telefoneInput);

            // O @Transactional salvará automaticamente (Dirty Checking),
            // mas chamar o save() não faz mal e deixa explícito.
            repository.save(clienteFinal);

        } else {
            // --- CRIAÇÃO (INSERT) ---

            // Verifica se o telefone já existe (caso raro de concorrência, mas bom validar)
            if (repository.existsByTelefone(telefoneInput)) {
                // Recupera o usuário para não duplicar (Fail-safe)
                clienteFinal = repository.findByTelefone(telefoneInput).get();
                return cadastrarOuAtualizar(dados); // Recursividade segura: tenta atualizar de novo
            }

            clienteFinal = new Cliente(dados);
            repository.save(clienteFinal);
        }

        return new DetalhamentoClienteDTO(clienteFinal);
    }
    public Long buscarIdPorEmail(String email) {
        return repository.findByEmail(email)
                .map(Cliente::getId)
                .orElseThrow(() -> new RegraDeNegocioException("Email não encontrado"));
    }
}