package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.PerfilAcesso;
import agendamentoDeClienteBarbearia.dtos.DadosBloqueioDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Bloqueio;
import agendamentoDeClienteBarbearia.repository.AgendamentoRepository; // 🚨 Importante
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.BloqueioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BloqueioService {

    private final BloqueioRepository repository;
    private final BarbeiroRepository barbeiroRepository;

    // 🚨 INJEÇÃO OBRIGATÓRIA: Para evitar colisão com clientes
    private final AgendamentoRepository agendamentoRepository;

    // ========================================================
    // 1. BLOQUEAR (CRIAR)
    // ========================================================
    @Transactional
    public void bloquearAgenda(DadosBloqueioDTO dados, String emailLogado) {
        // 1. Identificação
        Barbeiro usuarioLogado = barbeiroRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário logado não encontrado"));

        // 2. Define quem será bloqueado (Proteção contra bloquear barbeiro de outro dono)
        Barbeiro alvo = resolverAlvoDoBloqueio(dados.barbeiroId(), usuarioLogado);

        // 3. Validações de Data
        validarDatas(dados.inicio(), dados.fim());

        // 4. Validação: Já existe outro BLOQUEIO nesse horário?
        if (repository.existeBloqueioNoPeriodo(alvo.getId(), dados.inicio(), dados.fim())) {
            throw new RegraDeNegocioException("Já existe um bloqueio administrativo neste período.");
        }

        // 5. 🚨 VALIDAÇÃO CRÍTICA: Já existe um CLIENTE nesse horário?
        // Sem isso, você bloqueia o horário do almoço em cima de um cliente marcado!
        boolean temCliente = agendamentoRepository.existeConflitoDeHorario(
                alvo.getId(),
                dados.inicio(),
                dados.fim()
        );

        if (temCliente) {
            throw new RegraDeNegocioException("Não é possível bloquear: Existem clientes agendados neste intervalo. Cancele os agendamentos primeiro.");
        }

        // 6. Salvar
        Bloqueio bloqueio = new Bloqueio();
        bloqueio.setBarbeiro(alvo);
        bloqueio.setInicio(dados.inicio());
        bloqueio.setFim(dados.fim());
        bloqueio.setMotivo(dados.motivo() != null ? dados.motivo() : "Indisponível");

        repository.save(bloqueio);
        log.info("Bloqueio criado: Barbeiro ID {} | {} - {}", alvo.getId(), dados.inicio(), dados.fim());
    }

    // ========================================================
    // 2. DESBLOQUEAR (REMOVER)
    // ========================================================
    @Transactional
    public void desbloquear(Long idBloqueio, String emailLogado) {
        Bloqueio bloqueio = repository.findById(idBloqueio)
                .orElseThrow(() -> new RegraDeNegocioException("Bloqueio não encontrado"));

        Barbeiro usuarioLogado = barbeiroRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não autenticado"));

        // Validação de Permissão (Dono do bloqueio ou Chefe)
        validarPermissao(bloqueio, usuarioLogado);

        repository.delete(bloqueio);
        log.info("Bloqueio ID {} removido por {}", idBloqueio, emailLogado);
    }

    // ========================================================
    // 3. LISTAR (LEITURA)
    // ========================================================
    @Transactional(readOnly = true)
    public List<Bloqueio> listarBloqueiosFuturos(Long barbeiroId) {
        // Usa a query otimizada do seu repository
        return repository.findBloqueiosFuturos(barbeiroId, LocalDateTime.now());
    }

    // ========================================================
    // MÉTODOS PRIVADOS (HELPER)
    // ========================================================

    private Barbeiro resolverAlvoDoBloqueio(Long idAlvo, Barbeiro usuarioLogado) {
        // Se não mandou ID, ou mandou o próprio ID, o alvo é o logado
        if (idAlvo == null || idAlvo.equals(usuarioLogado.getId())) {
            return usuarioLogado;
        }

        // Se mandou ID de outro, precisa ser ADMIN/DONO para bloquear
        // Ajuste PerfilAcesso.ADMIN conforme seu Enum real
        if (usuarioLogado.getPerfil() != PerfilAcesso.ADMIN) {
            throw new RegraDeNegocioException("Apenas administradores podem bloquear a agenda de terceiros.");
        }

        Barbeiro alvo = barbeiroRepository.findById(idAlvo)
                .orElseThrow(() -> new RegraDeNegocioException("Profissional alvo não encontrado"));

        // Segurança SaaS: O alvo pertence à minha loja?
        // Se o alvo tem dono, o ID do dono deve ser igual ao meu ID
        if (alvo.getDono() != null && !alvo.getDono().getId().equals(usuarioLogado.getId())) {
            throw new RegraDeNegocioException("Este profissional não pertence à sua equipe.");
        }

        return alvo;
    }

    private void validarPermissao(Bloqueio bloqueio, Barbeiro usuarioLogado) {
        boolean isProprioBarbeiro = bloqueio.getBarbeiro().getId().equals(usuarioLogado.getId());

        // Verifica se é o chefe (Proteção contra NullPointer)
        boolean isChefe = false;
        Barbeiro donoDoAlvo = bloqueio.getBarbeiro().getDono();

        if (usuarioLogado.getPerfil() == PerfilAcesso.ADMIN && donoDoAlvo != null) {
            isChefe = donoDoAlvo.getId().equals(usuarioLogado.getId());
        } else if (usuarioLogado.getPerfil() == PerfilAcesso.ADMIN && donoDoAlvo == null) {
            // Se o alvo não tem dono e quem tá logado é admin, assume permissão
            // (Cenário onde o Admin bloqueia a si mesmo ou sistema global)
            isChefe = true;
        }

        if (!isProprioBarbeiro && !isChefe) {
            throw new RegraDeNegocioException("Sem permissão para remover este bloqueio.");
        }
    }

    private void validarDatas(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) throw new RegraDeNegocioException("Datas são obrigatórias.");
        if (inicio.isBefore(LocalDateTime.now())) throw new RegraDeNegocioException("Bloqueio no passado não permitido.");
        if (inicio.isAfter(fim) || inicio.isEqual(fim)) throw new RegraDeNegocioException("Data final deve ser maior que a inicial.");
    }
}