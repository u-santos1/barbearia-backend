package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.PerfilAcesso;
import agendamentoDeClienteBarbearia.TipoPlano;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import jakarta.transaction.Transactional;
import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BarbeiroService {

    private final BarbeiroRepository repository;
    private final PasswordEncoder passwordEncoder;

    public BarbeiroService(BarbeiroRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    // ========================================================
    // 1. CADASTRAR DONO (CRIAÇÃO DE CONTA)
    // ========================================================
    @Transactional
    public Barbeiro cadastrar(CadastroBarbeiroDTO dados) {
        if (repository.existsByEmail(dados.email())) {
            throw new RegraDeNegocioException("Este e-mail já está em uso.");
        }

        var barbeiro = new Barbeiro();
        barbeiro.setNome(dados.nome());
        barbeiro.setEmail(dados.email());
        barbeiro.setSenha(passwordEncoder.encode(dados.senha()));
        barbeiro.setEspecialidade(dados.especialidade());

        // CONFIGURAÇÕES PADRÃO DE DONO
        barbeiro.setPerfil(PerfilAcesso.ADMIN); // ⚠️ IMPORTANTE: Define que ele é o Dono
        barbeiro.setTrabalhaComoBarbeiro(true);
        barbeiro.setPlano(TipoPlano.SOLO);
        barbeiro.setComissaoPorcentagem(100.0); // Dono ganha 100% (ou define lógica de lucro depois)

        return repository.save(barbeiro);
    }

    // ========================================================
    // 2. CADASTRAR FUNCIONÁRIO (EQUIPE)
    // ========================================================
    @Transactional
    public Barbeiro cadastrarNovoFuncionario(CadastroBarbeiroDTO dados, Long idDono) {
        Barbeiro dono = repository.findById(idDono)
                .orElseThrow(() -> new RegraDeNegocioException("Dono não encontrado"));

        // 🚨 VALIDAÇÃO DO PLANO (CORRIGIDA)
        if (dono.getPlano() == TipoPlano.SOLO) {
            // Se o plano é SOLO, ele NÃO pode ter funcionários.
            // O count serve para garantir, mas a regra deve ser estrita.
            long totalFuncionarios = repository.countByDonoId(idDono);

            if (totalFuncionarios > 0) {
                // Se já tiver 1 (erro de base antiga), bloqueia.
                // Mas a lógica real é: Plano Solo não adiciona ninguém.
                throw new RegraDeNegocioException("Seu plano é SOLO. Faça upgrade para MULTI para contratar equipe.");
            }

            // Se quiser ser rigoroso: SOLO não adiciona NINGUÉM, nem o primeiro.
            throw new RegraDeNegocioException("Seu plano é SOLO. O cadastro de equipe é exclusivo do plano MULTI.");
        }

        if (repository.existsByEmail(dados.email())) {
            throw new RegraDeNegocioException("Já existe um profissional com este e-mail.");
        }

        Barbeiro novo = new Barbeiro();
        novo.setNome(dados.nome());
        novo.setEmail(dados.email());
        novo.setEspecialidade("Barbeiro"); // Ou vem do DTO
        novo.setSenha(passwordEncoder.encode(dados.senha()));

        // ⚠️ VINCULA AO DONO (HIERARQUIA)
        novo.setDono(dono);

        // ⚠️ DADOS VINDOS DO FRONT (Adicione isso no seu DTO Record se não tiver)
        novo.setTrabalhaComoBarbeiro(dados.vaiCortarCabelo());
        novo.setComissaoPorcentagem(dados.comissaoPorcentagem()); // Importante para o financeiro!

        // ⚠️ SEGURANÇA: DEFINE PERFIL
        // Se ele corta cabelo, é BARBEIRO. Se não, é RECEPÇÃO (exemplo).
        // Por simplificação, vamos colocar todos como BARBEIRO ou ter um perfil FUNCIONARIO
        novo.setPerfil(PerfilAcesso.BARBEIRO);

        return repository.save(novo);
    }
    public List<DetalhamentoBarbeiroDTO> listarTodos() {
        return repository.findAllByAtivoTrue().stream() // Assumindo que Barbeiro tem campo 'ativo'
                .map(DetalhamentoBarbeiroDTO::new)
                .toList();
    }

    @Transactional
    public void inativar(Long id) {
        var barbeiro = repository.findById(id).orElseThrow();
        barbeiro.setAtivo(false); // Adicione boolean ativo na entidade Barbeiro
    }
}