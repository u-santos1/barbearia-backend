package agendamentoDeClienteBarbearia.service;


import agendamentoDeClienteBarbearia.dtos.DadosBloqueioDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Bloqueio;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.BloqueioRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class BloqueioService {

    private final BloqueioRepository repository;
    private final BarbeiroRepository barbeiroRepository;

    public BloqueioService(BloqueioRepository repository, BarbeiroRepository barbeiroRepository) {
        this.repository = repository;
        this.barbeiroRepository = barbeiroRepository;
    }

    @Transactional
    public void bloquearAgenda(DadosBloqueioDTO dados, String emailLogado) {
        // 1. Identifica quem está bloqueando (Barbeiro logado)
        Barbeiro barbeiro = barbeiroRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new RuntimeException("Barbeiro não encontrado"));

        // 2. Cria o objeto de bloqueio
        // (Nota: Se no futuro o Dono puder bloquear agenda de outros,
        // a lógica de validação de permissão entraria aqui)

        Bloqueio bloqueio = new Bloqueio();
        bloqueio.setBarbeiro(barbeiro);
        bloqueio.setInicio(dados.inicio());
        bloqueio.setFim(dados.fim());
        bloqueio.setMotivo(dados.motivo());

        // 3. Salva
        repository.save(bloqueio);
    }
}
