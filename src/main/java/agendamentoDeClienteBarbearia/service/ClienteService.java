package agendamentoDeClienteBarbearia.service;


import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import jakarta.transaction.Transactional;
import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.repository.ClienteRepository;
import agendamentoDeClienteBarbearia.model.Cliente;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Cliente cadastrar(CadastroClienteDTO dados) {
        // Regra: E-mail único
        if (repository.existsByEmail(dados.email())) {
            throw new RegraDeNegocioException("Já existe um cliente cadastrado com este e-mail.");
        }

        var cliente = new Cliente();
        cliente.setNome(dados.nome());
        cliente.setEmail(dados.email());
        cliente.setTelefone(dados.telefone());

        return repository.save(cliente);
    }
}