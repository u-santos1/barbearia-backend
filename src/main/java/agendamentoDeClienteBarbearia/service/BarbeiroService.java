package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.PerfilAcesso;
import agendamentoDeClienteBarbearia.TipoPlano;
import agendamentoDeClienteBarbearia.dtos.AtualizacaoBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BarbeiroService {

    private final BarbeiroRepository repository;
    private final PasswordEncoder passwordEncoder;

    // --- CADASTRAR DONO ---
    @Transactional
    public Barbeiro cadastrarDono(CadastroBarbeiroDTO dados) {
        if (repository.existsByEmail(dados.email())) throw new RegraDeNegocioException("E-mail já em uso.");

        var barbeiro = new Barbeiro();
        barbeiro.setNome(dados.nome().trim());
        barbeiro.setEmail(dados.email().trim().toLowerCase());
        barbeiro.setSenha(passwordEncoder.encode(dados.senha()));
        barbeiro.setEspecialidade(dados.especialidade() != null ? dados.especialidade() : "Gestor");
        barbeiro.setPerfil(PerfilAcesso.ADMIN);
        barbeiro.setTrabalhaComoBarbeiro(true);
        barbeiro.setPlano(TipoPlano.SOLO);
        barbeiro.setComissaoPorcentagem(new BigDecimal("100.00"));
        barbeiro.setAtivo(true);

        return repository.save(barbeiro);
    }

    // --- CADASTRAR FUNCIONÁRIO ---
    @Transactional
    public Barbeiro cadastrarFuncionario(CadastroBarbeiroDTO dados, Long idDono) {
        Barbeiro dono = repository.findById(idDono)
                .orElseThrow(() -> new RegraDeNegocioException("Dono não encontrado"));

        long diasDeUso = 0;
        if (dono.getCreatedAt() != null) {
            diasDeUso = ChronoUnit.DAYS.between(dono.getCreatedAt().toLocalDate(), LocalDate.now());
        }

        boolean aindaEstaEmTeste = diasDeUso <= 15;
        boolean ehPlanoMulti = (dono.getPlano() == TipoPlano.MULTI);

        if (!ehPlanoMulti && !aindaEstaEmTeste) {
            throw new RegraDeNegocioException("Plano SOLO não permite equipe. Faça upgrade.");
        }

        if (repository.existsByEmail(dados.email())) throw new RegraDeNegocioException("E-mail já cadastrado.");

        Barbeiro novo = new Barbeiro();
        novo.setNome(dados.nome().trim());
        novo.setEmail(dados.email().trim().toLowerCase());
        novo.setSenha(passwordEncoder.encode(dados.senha()));
        novo.setEspecialidade(dados.especialidade() != null ? dados.especialidade() : "Barbeiro");
        novo.setDono(dono);
        novo.setTrabalhaComoBarbeiro(dados.vaiCortarCabelo() != null ? dados.vaiCortarCabelo() : true);
        novo.setComissaoPorcentagem(dados.comissaoPorcentagem() != null ? BigDecimal.valueOf(dados.comissaoPorcentagem()) : new BigDecimal("50.00"));
        novo.setPerfil(PerfilAcesso.BARBEIRO);
        novo.setAtivo(true);
        novo.setPlano(TipoPlano.SOLO);

        return repository.save(novo);
    }

    // --- LISTAGEM ADMIN ---
    @Transactional(readOnly = true)
    public List<DetalhamentoBarbeiroDTO> listarEquipe(Long idDono) {
        // Usa o método que busca Dono + Funcionários
        return repository.findAllByLoja(idDono).stream()
                .map(DetalhamentoBarbeiroDTO::new)
                .toList();
    }

    // --- LISTAGEM PÚBLICA (FRONTEND) ---
    @Transactional(readOnly = true)
    public List<DetalhamentoBarbeiroDTO> listarPorLoja(Long lojaId) {
        List<Barbeiro> barbeiros;

        if (lojaId != null) {
            // Busca Dono e Funcionários daquela loja
            barbeiros = repository.findAllByLoja(lojaId);
        } else {
            // Fallback: Busca todos ativos (Cuidado em produção com muitos dados)
            barbeiros = repository.findAllByAtivoTrue();
        }

        return barbeiros.stream()
                .map(DetalhamentoBarbeiroDTO::new)
                .toList();
    }

    @Transactional
    public void inativar(Long idFuncionario, Long idDonoLogado) {
        Barbeiro funcionario = repository.findById(idFuncionario)
                .orElseThrow(() -> new RegraDeNegocioException("Profissional não encontrado"));

        if (!funcionario.getId().equals(idDonoLogado)) {
            if (funcionario.getDono() == null || !funcionario.getDono().getId().equals(idDonoLogado)) {
                throw new RegraDeNegocioException("Permissão negada.");
            }
        }
        funcionario.setAtivo(false);
    }

    public Barbeiro buscarPorEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado"));
    }
    @Transactional
    public DetalhamentoBarbeiroDTO atualizarPerfil(String email, AtualizacaoBarbeiroDTO dados) {
        // Busca a entidade e a coloca no estado "Managed" do JPA
        var barbeiro = repository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        // Delegação da lógica de atualização para a própria Entidade (Encapsulamento)
        barbeiro.atualizarInformacoes(dados);

        // Não é necessário chamar repository.save() explicitamente devido ao @Transactional
        return new DetalhamentoBarbeiroDTO(barbeiro);
    }
}
