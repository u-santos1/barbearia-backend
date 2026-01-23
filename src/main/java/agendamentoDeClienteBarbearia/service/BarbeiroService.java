package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import jakarta.transaction.Transactional;
import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class BarbeiroService {

    private final BarbeiroRepository repository;
    private final PasswordEncoder passwordEncoder;

    public BarbeiroService(BarbeiroRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Barbeiro cadastrar(CadastroBarbeiroDTO dados) {
        if (repository.existsByEmail(dados.email())) {
            throw new RegraDeNegocioException("Já existe um barbeiro com este e-mail.");
        }
        String senhaCriptografada = passwordEncoder.encode(dados.senha());

        var barbeiro = new Barbeiro();
        barbeiro.setNome(dados.nome());
        barbeiro.setEmail(dados.email());
        barbeiro.setSenha(senhaCriptografada);
        barbeiro.setEspecialidade(dados.especialidade());

        return repository.save(barbeiro);
    }
}