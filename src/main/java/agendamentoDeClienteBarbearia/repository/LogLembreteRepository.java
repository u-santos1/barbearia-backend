package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.LogLembrete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogLembreteRepository extends JpaRepository<LogLembrete, Long> {
    boolean existsByAgendamentoIdAndRegraId(Long agendamentoId, Long regraId);
}
