package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.PerfilAcesso;
import agendamentoDeClienteBarbearia.TipoPlano;
import agendamentoDeClienteBarbearia.dtos.AtualizacaoBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.BarbeiroPublicoDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtosResponse.RelatorioBarbeiroDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Expediente;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.ExpedienteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BarbeiroService {

    private final BarbeiroRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ExpedienteRepository expedienteRepository;

    // --- CADASTRAR DONO ---
    @Transactional
    public DetalhamentoBarbeiroDTO cadastrarDono(CadastroBarbeiroDTO dados) {
        if (repository.existsByEmail(dados.email())) {
            throw new RegraDeNegocioException("E-mail já em uso.");
        }

        var barbeiro = new Barbeiro();

        barbeiro.setNome(dados.nome().trim());
        barbeiro.setEmail(dados.email().trim().toLowerCase());
        barbeiro.setSenha(passwordEncoder.encode(dados.senha()));
        barbeiro.setEspecialidade(dados.especialidade() != null ? dados.especialidade() : "Gestor");

        // Definições de Dono
        barbeiro.setPerfil(PerfilAcesso.ADMIN);
        barbeiro.setTrabalhaComoBarbeiro(true);
        barbeiro.setPlano(TipoPlano.SOLO); // O dono começa com plano SOLO (ou o que vier no DTO)
        barbeiro.setComissaoPorcentagem(new BigDecimal("100.00")); // Dono ganha tudo do próprio corte
        barbeiro.setWhatsappContato(dados.whatsappContato());
        barbeiro.setAtivo(true);
        Barbeiro salvo = repository.save(barbeiro);
        criarExpedientePadrao(salvo);

        return new DetalhamentoBarbeiroDTO(salvo);
    }

    private void criarExpedientePadrao(Barbeiro barbeiro){
        List<Expediente> expedientes = new ArrayList<>();
        for (int i = 1; i <= 7; i++){
            Expediente e = new Expediente();
            e.setBarbeiro(barbeiro);
            e.setDiaSemana(DayOfWeek.of(i));
            e.setAbertura(LocalTime.of(9,0));
            e.setFechamento(LocalTime.of(18, 0));
            e.setAlmocoInicio(LocalTime.of(12, 0));
            e.setAlmocoFim(LocalTime.of(13, 0));

            boolean diaUtil = (i < 7);
            e.setAtivo(diaUtil);
            e.setTrabalha(diaUtil);

            expedientes.add(e);
        }
        expedienteRepository.saveAll(expedientes);
    }

    // --- CADASTRAR FUNCIONÁRIO ---
    @Transactional
    public DetalhamentoBarbeiroDTO cadastrarFuncionario(CadastroBarbeiroDTO dados, Long idDono) {
        Barbeiro dono = repository.findById(idDono)
                .orElseThrow(() -> new RegraDeNegocioException("Dono não encontrado"));

        // 1. Validação de Plano (Trial ou Multi)
        validarLimitesDoPlano(dono);

        String emailFormatado = dados.email().trim().toLowerCase();

        // 2. LÓGICA INTELIGENTE (SEM QUEBRAR O CÓDIGO)
        // Busca no banco. Se achar, usa o existente. Se não achar, cria um "new Barbeiro()" vazio.
        Barbeiro novo = repository.findByEmail(emailFormatado).orElse(new Barbeiro());

        // Se ele já existe no banco (tem ID) E está ATIVO, aí sim bloqueia o cadastro.
        // Se ele estiver inativo (ativo = false), o sistema pula esse IF e sobrescreve os dados reativando-o!
        if (novo.getId() != null && novo.getAtivo()) {
            throw new RegraDeNegocioException("E-mail já cadastrado.");
        }

        // ====================================================================
        // O SEU CÓDIGO DAQUI PARA BAIXO CONTINUA EXATAMENTE IGUAL
        // Se for um cadastro novo, preenche do zero.
        // Se for um funcionário inativo, ele atualiza as informações antigas!
        // ====================================================================

        novo.setNome(dados.nome().trim());
        novo.setEmail(emailFormatado);
        novo.setSenha(passwordEncoder.encode(dados.senha())); // Funcionário precisa de senha para ver a própria agenda
        novo.setEspecialidade(dados.especialidade() != null ? dados.especialidade() : "Barbeiro");
        novo.setDono(dono); // Vínculo crucial
        novo.setTrabalhaComoBarbeiro(dados.vaiCortarCabelo() != null ? dados.vaiCortarCabelo() : true);

        // 2. Correção Financeira (Double -> BigDecimal)
        if (dados.comissaoPorcentagem() != null) {
            novo.setComissaoPorcentagem(BigDecimal.valueOf(dados.comissaoPorcentagem()));
        } else {
            novo.setComissaoPorcentagem(new BigDecimal("50.00"));
        }

        novo.setPerfil(PerfilAcesso.BARBEIRO);

        // A MÁGICA FINAL ACONTECE AQUI:
        // Se era um barbeiro inativo (false), ele volta para "true"
        novo.setAtivo(true);

        // 3. Correção: Funcionário NÃO tem plano, ele herda o acesso do dono.
        novo.setPlano(null);
        Barbeiro salva = repository.save(novo);
        return new DetalhamentoBarbeiroDTO(salva);
    }

    // --- LISTAGEM ADMIN (Ver tudo da loja) ---
    @Transactional(readOnly = true)
    public List<DetalhamentoBarbeiroDTO> listarEquipe(Long idDono) {
        return repository.findAllByLoja(idDono).stream()
                .map(DetalhamentoBarbeiroDTO::new)
                .toList();
    }

    // --- LISTAGEM PÚBLICA (FRONTEND) ---
    @Transactional(readOnly = true)
    public List<BarbeiroPublicoDTO> listarPorLoja(Long lojaId) {
        if (lojaId == null) {
            // 🚨 CORREÇÃO DE SEGURANÇA: Nunca retorne todos os usuários do banco
            // Se não tem ID da loja, retorna lista vazia ou erro.
            return Collections.emptyList();
        }

        // Busca Dono e Funcionários daquela loja específica e converte para o DTO seguro
        return repository.findAllByLoja(lojaId).stream()
                .map(BarbeiroPublicoDTO::new) // <-- Aqui é a mágica que esconde os dados sensíveis
                .toList();
    }

    @Transactional
    public void inativar(Long idFuncionario, Long idDonoLogado) {
        Barbeiro funcionario = repository.findById(idFuncionario)
                .orElseThrow(() -> new RegraDeNegocioException("Profissional não encontrado"));

        // Lógica de Permissão:
        // 1. Sou eu mesmo me inativando?
        boolean ehOProprio = funcionario.getId().equals(idDonoLogado);
        // 2. Sou o chefe dele me inativando? (Checagem de null no getDono para evitar NPE)
        boolean ehOChefe = funcionario.getDono() != null && funcionario.getDono().getId().equals(idDonoLogado);

        if (!ehOProprio && !ehOChefe) {
            throw new RegraDeNegocioException("Permissão negada.");
        }

        // Soft Delete (Apenas marca como inativo)
        funcionario.setAtivo(false);
        repository.save(funcionario);

    }

    // --- MÉTODOS AUXILIARES ---
     void validarLimitesDoPlano(Barbeiro dono) {
        // 1. O VIP passa direto (economiza CPU)
        if (dono.getPlano() == TipoPlano.MULTI) {
            return;
        }

        // 2. Proteção de fuso horário brasileiro
        ZoneId fusoBrasil = ZoneId.of("America/Sao_Paulo");
        long diasDeUso = 0;

        if (dono.getCreatedAt() != null) {
            diasDeUso = ChronoUnit.DAYS.between(dono.getCreatedAt().toLocalDate(), LocalDate.now(fusoBrasil));
        }

        boolean aindaEstaEmTeste = diasDeUso <= 15;

        // 3. Guilhotina do tempo
        if (!aindaEstaEmTeste) {
            throw new RegraDeNegocioException("Seu período de teste acabou. Faça upgrade para MULTI.");
        }

        // 4. Guilhotina da quantidade de funcionários
        long totalDeFuncionarios = repository.countByDonoIdAndAtivoTrue(dono.getId());

        // Se ele já tem 3, não pode cadastrar o 4º
        if (totalDeFuncionarios >= 3) {
            throw new RegraDeNegocioException("Limite atingido. O plano SOLO permite até 3 funcionários. Faça upgrade para MULTI.");
        }
    }

    @Transactional(readOnly = true)
    public DetalhamentoBarbeiroDTO buscarPorEmail(String email) {
        Barbeiro barbeiro = repository.findByEmail(email)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado"));
        return new DetalhamentoBarbeiroDTO(barbeiro);
    }
    @Transactional(readOnly = true)
    public DetalhamentoBarbeiroDTO buscarPorId(Long id){
        Barbeiro barbeiro = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Id de usuario nao encontrado"));
        return new DetalhamentoBarbeiroDTO(barbeiro);
    }

    @Transactional
    public DetalhamentoBarbeiroDTO atualizarPerfil(Long id, AtualizacaoBarbeiroDTO dados) {
        var barbeiro = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        barbeiro.atualizarInformacoes(dados);
        return new DetalhamentoBarbeiroDTO(barbeiro);
    }
    public List<RelatorioBarbeiroDTO> relatorioMensal(Long donoId, int mes, int ano) {
        return repository.relatorioMensal(donoId, mes, ano);}
}
