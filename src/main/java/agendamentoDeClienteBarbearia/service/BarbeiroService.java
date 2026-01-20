package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import jakarta.transaction.Transactional;
import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import org.springframework.stereotype.Service;

@Service
public class BarbeiroService {

    private final BarbeiroRepository repository;

    public BarbeiroService(BarbeiroRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Barbeiro cadastrar(CadastroBarbeiroDTO dados) {
        if (repository.existsByEmail(dados.email())) {
            throw new RegraDeNegocioException("Já existe um barbeiro com este e-mail.");
        }

        var barbeiro = new Barbeiro();
        barbeiro.setNome(dados.nome());
        barbeiro.setEmail(dados.email());
        barbeiro.setEspecialidade(dados.especialidade());

        return repository.save(barbeiro);
    }
}