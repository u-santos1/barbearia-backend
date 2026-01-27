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

        Optional<Cliente> clienteExistente = Optional.empty();
        Cliente clienteFinal;

        // 1. Lógica de Busca (Prioridade: E-mail -> Telefone)
        if (dados.email() != null && !dados.email().isBlank()) {
            clienteExistente = repository.findByEmail(dados.email());
        }

        if (clienteExistente.isEmpty()) {
            clienteExistente = repository.findByTelefone(dados.telefone());
        }

        // 2. Decisão: Atualizar ou Criar
        if (clienteExistente.isPresent()) {
            clienteFinal = clienteExistente.get();

            // --- ATUALIZAÇÕES ---
            clienteFinal.setNome(dados.nome());
            clienteFinal.setTelefone(dados.telefone()); // <--- CORREÇÃO: Faltava atualizar o telefone!

            // Só atualiza o email se ele foi informado e não está em branco
            if (dados.email() != null && !dados.email().isBlank()) {
                // (Opcional) Poderia verificar se esse email novo já não pertence a OUTRA pessoa
                clienteFinal.setEmail(dados.email());
            }

            // O @Transactional faz o "Dirty Checking" e salva sozinho ao fim do método.
        } else {
            // --- CRIAÇÃO ---
            clienteFinal = new Cliente(dados);
            repository.save(clienteFinal);
        }

        // 3. Retorno
        return new DetalhamentoClienteDTO(clienteFinal);
    }
}