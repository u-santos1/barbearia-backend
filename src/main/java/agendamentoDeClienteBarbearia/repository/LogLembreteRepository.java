package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.LogLembrete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LogLembreteRepository extends JpaRepository<LogLembrete, Long> {
    boolean existsByAgendamentoIdAndRegraId(Long agendamentoId, Long regraId);

    @Query("SELECT COUNT(l) FROM LogLembrete l WHERE l.dataEnvio >= :inicioDia AND l.dataEnvio <= :fimDia AND l.status = 'ENVIADO' AND l.agendamentoId IN (SELECT a.id FROM Agendamento a WHERE a.barbeiro.usuario.email = :email)")
    long contarLembretesEnviadosHoje(@Param("inicioDia") LocalDateTime inicioDia,
                                     @Param("fimDia") LocalDateTime fimDia,
                                     @Param("email") String email);
}
