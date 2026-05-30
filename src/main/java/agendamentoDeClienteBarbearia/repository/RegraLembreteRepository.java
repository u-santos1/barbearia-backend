package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.RegraLembrete;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RegraLembreteRepository extends JpaRepository<RegraLembrete, Long> {
    List<RegraLembrete> findAllByDonoEmail(String emailDono);
    Optional<RegraLembrete> findByIdAndDonoEmail(Long id, String emailDono);
    List<RegraLembrete> findByAtivoTrue();
}