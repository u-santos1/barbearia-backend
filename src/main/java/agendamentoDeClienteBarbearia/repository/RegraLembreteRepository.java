package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.RegraLembrete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface RegraLembreteRepository extends JpaRepository<RegraLembrete, Long> {
    List<RegraLembrete> findAllByDonoEmail(String emailDono);
    Optional<RegraLembrete> findByIdAndDonoEmail(Long id, String emailDono);
    
    @Query("SELECT r FROM RegraLembrete r JOIN FETCH r.dono WHERE r.ativo = true")
    List<RegraLembrete> findByAtivoTrue();
}