package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.PerfilAcesso;
import agendamentoDeClienteBarbearia.TipoPlano;
import agendamentoDeClienteBarbearia.dtos.AtualizacaoBarbeiroDTO;
import agendamentoDeClienteBarbearia.dtos.AtualizacaoPerfilDTO;
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
import java.time.*;
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


        if (dono.getPlano() == TipoPlano.SOLO) {
            long totalEquipe = repository.countByDonoIdAndAtivoTrue(dono.getId());

            // O plano SOLO só permite o dono (0 funcionários extras).
            // Se ele já tiver 0 funcionários cadastrados, qualquer tentativa de cadastrar o 1º vai falhar.
            if (totalEquipe >= 0) {
                throw new RegraDeNegocioException("O seu plano atual (SOLO) não permite adicionar equipa. Faça upgrade para o plano MULTI.");
            }
        }
        // ====================================================================

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
            //  CORREÇÃO DE SEGURANÇA: Nunca retorne todos os usuários do banco
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
    public void validarAcessoRecurso(Barbeiro dono, String recurso) {
        // 1. O MULTI tem acesso total
        if (dono.getPlano() == TipoPlano.MULTI) return;

        // 2. Cálculo do Trial (7 dias a partir da data de criação)
        long diasDeUso = 0;
        if (dono.getCreatedAt() != null) {
            diasDeUso = ChronoUnit.DAYS.between(dono.getCreatedAt().toLocalDate(), LocalDate.now());
        }
        boolean emTrial = diasDeUso <= 7;

        // 3. Se estiver em trial, liberamos tudo
        if (emTrial) return;

        // 4. Se o trial acabou e é SOLO, bloqueamos recursos avançados
        List<String> recursosPremium = List.of("FINANCEIRO", "EQUIPE", "EXPEDIENTE", "LEMBRETES", "PDF");

        if (recursosPremium.contains(recurso.toUpperCase())) {
            throw new RegraDeNegocioException("Seu período de testes acabou. Faça o upgrade para o plano MULTI para continuar usando " + recurso);
        }
    }

    public void validarLimiteDeBarbeiros(Barbeiro dono) {
        // Regra: Solo só pode ter 1 barbeiro
        if (dono.getPlano() == TipoPlano.SOLO) {
            long total = repository.countByDonoIdAndAtivoTrue(dono.getId());
            if (total >= 1) { // Nota: O dono conta como 1, então se já existir 1, não pode adicionar outro
                throw new RegraDeNegocioException("O plano SOLO permite apenas 1 barbeiro. Faça o upgrade para o plano MULTI.");
            }
        }
    }
    public void validarAcessoPremium(Barbeiro dono) {
        // Se já é MULTI, passa direto
        if (dono.getPlano() == TipoPlano.MULTI) {
            return;
        }

        // Verifica se o trial (7 dias) ainda está valendo
        boolean emTrial = dono.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7));

        if (!emTrial) {
            // Se o trial passou e ele não é MULTI, bloqueia tudo!
            throw new RegraDeNegocioException("Seu período de testes expirou. Faça upgrade para o plano MULTI para acessar este recurso.");
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
    @Transactional
    public List<RelatorioBarbeiroDTO> relatorioMensal(Long donoId, int mes, int ano) {
        return repository.relatorioMensal(donoId, mes, ano);}

    @Transactional
    public Barbeiro atualizarPerfil(String emailLogado, AtualizacaoPerfilDTO dados) {
        Barbeiro barbeiro = repository.findByEmail(emailLogado)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado."));

        // Atualiza os dados apenas se eles foram enviados na requisição
        if (dados.barbeariaNome() != null) barbeiro.setBarbeariaNome(dados.barbeariaNome());
        if (dados.whatsappContato() != null) barbeiro.setWhatsappContato(dados.whatsappContato());
        if (dados.mensagemOla() != null) barbeiro.setMensagemOla(dados.mensagemOla());
        if (dados.imagemFundo() != null) barbeiro.setImagemFundo(dados.imagemFundo());
        if (dados.corPrimaria() != null) barbeiro.setCorPrimaria(dados.corPrimaria());

        return barbeiro;
}

}
