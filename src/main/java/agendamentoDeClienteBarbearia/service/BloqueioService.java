package agendamentoDeClienteBarbearia.service;




import agendamentoDeClienteBarbearia.PerfilAcesso;
import agendamentoDeClienteBarbearia.dtos.DadosBloqueioDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Bloqueio;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.BloqueioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BloqueioService {

    private final BloqueioRepository repository;
    private final BarbeiroRepository barbeiroRepository;

    @Transactional
    public void bloquearAgenda(DadosBloqueioDTO dados, String emailLogado) {
        // 1. Quem está tentando realizar a ação?
        Barbeiro usuarioLogado = barbeiroRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário logado não encontrado"));

        // 2. Quem sofrerá o bloqueio?
        // Se o DTO vier com ID, verificamos permissão. Se não, é o próprio usuário.
        Barbeiro alvo = resolverAlvoDoBloqueio(dados.barbeiroId(), usuarioLogado);

        // 3. Validações Temporais (Regras de Ouro)
        validarDatas(dados.inicio(), dados.fim());

        // 4. Validação de Conflito (Não bloquear o que já está bloqueado)
        if (repository.existeBloqueioNoPeriodo(alvo.getId(), dados.inicio(), dados.fim())) {
            throw new RegraDeNegocioException("Já existe um bloqueio neste período para este profissional.");
        }

        // 5. Criação e Persistência
        Bloqueio bloqueio = new Bloqueio();
        bloqueio.setBarbeiro(alvo);
        bloqueio.setInicio(dados.inicio());
        bloqueio.setFim(dados.fim());
        bloqueio.setMotivo(dados.motivo() != null ? dados.motivo() : "Indisponível");

        repository.save(bloqueio);
        log.info("Bloqueio criado para o barbeiro {} de {} até {}", alvo.getNome(), dados.inicio(), dados.fim());
    }

    @Transactional
    public void desbloquear(Long idBloqueio, String emailLogado) {
        Bloqueio bloqueio = repository.findById(idBloqueio)
                .orElseThrow(() -> new RegraDeNegocioException("Bloqueio não encontrado"));

        Barbeiro usuarioLogado = barbeiroRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não autenticado"));

        // Regra de Segurança: Só o dono do bloqueio ou o Chefe dele pode remover
        boolean isDonoDoBloqueio = bloqueio.getBarbeiro().getId().equals(usuarioLogado.getId());
        boolean isChefe = usuarioLogado.getPerfil() == PerfilAcesso.ADMIN &&
                bloqueio.getBarbeiro().getDono().getId().equals(usuarioLogado.getId());

        if (!isDonoDoBloqueio && !isChefe) {
            throw new RegraDeNegocioException("Você não tem permissão para remover este bloqueio.");
        }

        repository.delete(bloqueio);
        log.info("Bloqueio ID {} removido por {}", idBloqueio, emailLogado);
    }

    @Transactional(readOnly = true)
    public List<Bloqueio> listarBloqueiosFuturos(Long barbeiroId) {
        return repository.findBloqueiosFuturos(barbeiroId, LocalDateTime.now());
    }

    // --- Métodos Auxiliares Privados ---

    private Barbeiro resolverAlvoDoBloqueio(Long idAlvo, Barbeiro usuarioLogado) {
        // Se não informou ID no DTO, o bloqueio é para si mesmo
        if (idAlvo == null || idAlvo.equals(usuarioLogado.getId())) {
            return usuarioLogado;
        }

        // Se informou ID, verificamos se o usuário logado tem permissão (É DONO/ADMIN)
        if (usuarioLogado.getPerfil() != PerfilAcesso.ADMIN) {
            throw new RegraDeNegocioException("Apenas administradores podem bloquear a agenda de outros profissionais.");
        }

        Barbeiro alvo = barbeiroRepository.findById(idAlvo)
                .orElseThrow(() -> new RegraDeNegocioException("Profissional alvo não encontrado"));

        // Verifica se o alvo realmente pertence à equipe deste dono
        if (!alvo.getDono().getId().equals(usuarioLogado.getId())) {
            throw new RegraDeNegocioException("Este profissional não pertence à sua equipe.");
        }

        return alvo;
    }

    private void validarDatas(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            throw new RegraDeNegocioException("Datas de início e fim são obrigatórias.");
        }
        if (inicio.isBefore(LocalDateTime.now())) {
            throw new RegraDeNegocioException("Não é possível criar bloqueios no passado.");
        }
        if (inicio.isAfter(fim) || inicio.isEqual(fim)) {
            throw new RegraDeNegocioException("A data final deve ser posterior à data inicial.");
        }
    }
}