package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @Query("""
        SELECT COUNT(a) > 0 
        FROM Agendamento a 
        WHERE a.barbeiro.id = :barbeiroId 
        AND a.status = 'AGENDADO'
        AND (
            (a.dataHoraInicio < :fimSolicitado) AND 
            (a.dataHoraFim > :inicioSolicitado)
        )
    """)
    boolean existeConflitoDeHorario(Long barbeiroId, LocalDateTime inicioSolicitado, LocalDateTime fimSolicitado);

    List<Agendamento> findByBarbeiroIdAndDataHoraInicioBetween(Long idBarbeiro, LocalDateTime inicio, LocalDateTime fim);
    // Busca agendamentos do cliente, ordenados por data
    List<Agendamento> findByClienteIdOrderByDataHoraInicioDesc(Long clienteId);
}