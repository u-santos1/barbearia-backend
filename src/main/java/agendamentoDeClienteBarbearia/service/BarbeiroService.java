package agendamentoDeClienteBarbearia.service;


import agendamentoDeClienteBarbearia.PerfilAcesso;
import agendamentoDeClienteBarbearia.TipoPlano;
import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j // Logs para produção (Auditoria)
@Service
@RequiredArgsConstructor // Injeção de dependência limpa
public class BarbeiroService {

    private final BarbeiroRepository repository;
    private final PasswordEncoder passwordEncoder;

    // ========================================================
    // 1. CADASTRAR DONO (CRIAÇÃO DE CONTA / SAAS)
    // ========================================================
    @Transactional
    public Barbeiro cadastrarDono(CadastroBarbeiroDTO dados) {
        log.info("Iniciando cadastro de novo dono: {}", dados.email());

        if (repository.existsByEmail(dados.email())) {
            throw new RegraDeNegocioException("Este e-mail já está em uso.");
        }

        var barbeiro = new Barbeiro();
        barbeiro.setNome(dados.nome().trim());
        barbeiro.setEmail(dados.email().trim().toLowerCase());
        barbeiro.setSenha(passwordEncoder.encode(dados.senha()));
        barbeiro.setEspecialidade(dados.especialidade() != null ? dados.especialidade() : "Gestor");

        // CONFIGURAÇÕES DE DONO
        barbeiro.setPerfil(PerfilAcesso.ADMIN);
        barbeiro.setTrabalhaComoBarbeiro(true); // Dono geralmente corta, pode mudar depois
        barbeiro.setPlano(TipoPlano.SOLO); // Começa no grátis/solo
        barbeiro.setComissaoPorcentagem(new BigDecimal("100.00")); // Dono fica com tudo
        barbeiro.setAtivo(true);

        return repository.save(barbeiro);
    }

    // ========================================================
    // 2. CADASTRAR FUNCIONÁRIO (EQUIPE)
    // ========================================================
    @Transactional
    public Barbeiro cadastrarFuncionario(CadastroBarbeiroDTO dados, Long idDono) {
        log.info("Dono ID {} tentando cadastrar funcionário: {}", idDono, dados.email());

        Barbeiro dono = repository.findById(idDono)
                .orElseThrow(() -> new RegraDeNegocioException("Dono não encontrado"));

        // 🚨 VALIDAÇÃO DO PLANO (CRÍTICO PARA O NEGÓCIO)
        // Se for SOLO, não pode ter equipe.
        if (dono.getPlano() == TipoPlano.SOLO) {
            throw new RegraDeNegocioException("Seu plano atual (SOLO) não permite equipe. Faça o upgrade para o plano MULTI.");
        }

        if (repository.existsByEmail(dados.email())) {
            throw new RegraDeNegocioException("Já existe um profissional com este e-mail no sistema.");
        }

        Barbeiro novo = new Barbeiro();
        novo.setNome(dados.nome().trim());
        novo.setEmail(dados.email().trim().toLowerCase());
        novo.setSenha(passwordEncoder.encode(dados.senha()));
        novo.setEspecialidade(dados.especialidade() != null ? dados.especialidade() : "Barbeiro");

        // ⚠️ VINCULAÇÃO DE MULTI-TENANCY (ISOLAMENTO DE DADOS)
        novo.setDono(dono);

        // Configurações do Funcionário
        // Se o DTO não trouxer a info, assume que corta cabelo
        novo.setTrabalhaComoBarbeiro(dados.vaiCortarCabelo() != null ? dados.vaiCortarCabelo() : true);

        // Financeiro Seguro (BigDecimal)
        if (dados.comissaoPorcentagem() != null) {
            novo.setComissaoPorcentagem(BigDecimal.valueOf(dados.comissaoPorcentagem()));
        } else {
            novo.setComissaoPorcentagem(new BigDecimal("50.00")); // Padrão de mercado
        }

        novo.setPerfil(PerfilAcesso.BARBEIRO);
        novo.setAtivo(true);
        novo.setPlano(TipoPlano.SOLO); // O funcionário herda o contexto do dono, o plano dele individual é irrelevante

        return repository.save(novo);
    }

    // ========================================================
    // 3. LISTAGEM SEGURA (FILTRADA POR BARBEARIA)
    // ========================================================
    @Transactional(readOnly = true) // Otimiza performance
    public List<DetalhamentoBarbeiroDTO> listarEquipe(Long idDono) {
        // CORREÇÃO CRÍTICA:
        // Antes estava findAllByAtivoTrue() -> Isso trazia barbeiros de OUTRAS barbearias.
        // Agora busca apenas quem pertence ao Dono logado OU é o próprio Dono.

        // Regra: Traz o dono e seus funcionários
        List<Barbeiro> equipe = repository.findAllByDonoIdOrId(idDono);

        return equipe.stream()
                .filter(Barbeiro::getAtivo) // Filtra inativos em memória ou na query (melhor na query se possível)
                .map(DetalhamentoBarbeiroDTO::new)
                .toList();
    }

    @Transactional
    public void inativar(Long idFuncionario, Long idDonoLogado) {
        Barbeiro funcionario = repository.findById(idFuncionario)
                .orElseThrow(() -> new RegraDeNegocioException("Profissional não encontrado"));

        // SEGURANÇA: Garante que um dono não exclua funcionário de outro
        if (!funcionario.getId().equals(idDonoLogado)) { // Se não for ele mesmo se excluindo
            if (funcionario.getDono() == null || !funcionario.getDono().getId().equals(idDonoLogado)) {
                throw new RegraDeNegocioException("Você não tem permissão para alterar este profissional.");
            }
        }

        // Soft Delete (Não apaga do banco, só desativa para manter histórico financeiro)
        funcionario.setAtivo(false);
    }

    // Método auxiliar para buscar pelo login
    public Barbeiro buscarPorEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado"));
    }
}