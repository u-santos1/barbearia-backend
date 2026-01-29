package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.TipoPlano;
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

        // Dono se cadastra como dono e como barbeiro por padrão (pode mudar depois)
        barbeiro.setTrabalhaComoBarbeiro(true);
        barbeiro.setPlano(TipoPlano.SOLO); // Padrão ao criar conta

        return repository.save(barbeiro);
    }

    @Transactional // Adicionei Transactional aqui também por segurança
    public Barbeiro cadastrarNovoFuncionario(CadastroBarbeiroDTO dados, Long idDono) {
        // 1. Busca o Dono para ver o plano dele
        Barbeiro dono = repository.findById(idDono)
                .orElseThrow(() -> new RuntimeException("Dono não encontrado"));

        // 2. Lógica do Plano SOLO
        if (dono.getPlano() == TipoPlano.SOLO) {
            // CORREÇÃO: Usa 'repository' (a variável) e não o nome da Classe
            long totalFuncionarios = repository.countByDonoId(idDono);

            if (totalFuncionarios >= 1) {
                throw new RegraDeNegocioException("Seu plano é SOLO. Faça upgrade para MULTI para contratar equipe.");
            }
        }

        // 3. Valida email duplicado para o funcionário também
        if (repository.existsByEmail(dados.email())) {
            throw new RegraDeNegocioException("Já existe um barbeiro com este e-mail.");
        }

        // 4. Cria o novo funcionário com todos os dados
        Barbeiro novo = new Barbeiro();
        novo.setNome(dados.nome());
        novo.setEmail(dados.email());
        novo.setEspecialidade(dados.especialidade());

        // Criptografa a senha do funcionário
        novo.setSenha(passwordEncoder.encode(dados.senha()));

        // Checkbox vindo do Front (Se o DTO não tiver esse campo, vai dar erro aqui)
        // Se der erro, adicione 'Boolean vaiCortarCabelo' no seu DTO record.
        novo.setTrabalhaComoBarbeiro(dados.vaiCortarCabelo());

        novo.setDono(dono); // Vincula ao dono

        return repository.save(novo);
    }
}