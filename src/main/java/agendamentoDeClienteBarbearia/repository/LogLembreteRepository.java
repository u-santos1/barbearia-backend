package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.dtosResponse.HistoricoLembreteProjection;
import agendamentoDeClienteBarbearia.model.LogLembrete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogLembreteRepository extends JpaRepository<LogLembrete, Long> {
    boolean existsByAgendamentoIdAndRegraId(Long agendamentoId, Long regraId);



    @Query("SELECT COUNT(l) FROM LogLembrete l WHERE l.dataEnvio >= :inicioDia AND l.dataEnvio <= :fimDia AND l.status = 'ENVIADO' AND l.agendamentoId IN (SELECT a.id FROM Agendamento a WHERE a.barbeiro.email = :email)")
    long contarLembretesEnviadosHoje(@Param("inicioDia") LocalDateTime inicioDia,
                                     @Param("fimDia") LocalDateTime fimDia,
                                     @Param("email") String email);
    @Query(value = "SELECT " +
            "l.data_envio AS dataEnvio, " +
            "c.nome AS clienteNome, " +
            "c.telefone AS telefoneDestino, " +
            "l.status AS status, " +
            "r.nome AS regraNome " +
            "FROM log_lembretes l " +
            "INNER JOIN tb_agendamentos a ON l.agendamento_id = a.id " + // <--- O AJUSTE FOI AQUI
            "INNER JOIN tb_clientes c ON a.cliente_id = c.id " +
            "INNER JOIN regras_lembrete r ON l.regra_id = r.id " +
            "WHERE a.barbeiro_id = :donoId OR c.dono_id = :donoId " +
            "ORDER BY l.data_envio DESC", nativeQuery = true)
    List<HistoricoLembreteProjection> buscarHistoricoPorDono(@Param("donoId") Long donoId);

}
