package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.StatusAgendamento;
import agendamentoDeClienteBarbearia.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // ========================================================================
    // 1. MÉTODOS ESSENCIAIS PARA O AGENDAMENTO SERVICE
    // ========================================================================

    // Usado para verificar disponibilidade no Service
    List<Agendamento> findByBarbeiroIdAndDataHoraInicioBetween(Long barbeiroId, LocalDateTime inicio, LocalDateTime fim);

    // Relatório Financeiro
    List<Agendamento> findByDataHoraInicioBetweenAndStatus(LocalDateTime inicio, LocalDateTime fim, StatusAgendamento status);

    // ========================================================================
    // 2. CONSULTAS OTIMIZADAS (JOIN FETCH) - PARA EVITAR ERRO DE N+1
    // ========================================================================

    // Histórico do Cliente
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.barbeiro
        JOIN FETCH a.servico
        WHERE a.cliente.id = :clienteId
        ORDER BY a.dataHoraInicio DESC
    """)
    List<Agendamento> findByClienteIdOrderByDataHoraInicioDesc(@Param("clienteId") Long clienteId);

    // Histórico do Barbeiro
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.cliente
        JOIN FETCH a.servico
        WHERE a.barbeiro.id = :barbeiroId
        ORDER BY a.dataHoraInicio DESC
    """)
    List<Agendamento> findByBarbeiroIdOrderByDataHoraInicioDesc(@Param("barbeiroId") Long barbeiroId);

    // ========================================================================
    // 3. VISÃO DO DONO (SAAS)
    // ========================================================================
    @Query("""
        SELECT a FROM Agendamento a 
        WHERE a.barbeiro.dono.id = :donoId 
        OR a.barbeiro.id = :donoId
        ORDER BY a.dataHoraInicio DESC
    """)
    List<Agendamento> findAllByBarbeiroDonoId(@Param("donoId") Long donoId);

    // ========================================================================
    // 4. MÉTODOS LEGADOS/EXTRAS (Mantidos para compatibilidade se necessário)
    // ========================================================================

    @Query("""
        SELECT COUNT(a) > 0 
        FROM Agendamento a 
        WHERE a.barbeiro.id = :barbeiroId 
        AND a.status NOT IN ('CANCELADO_PELO_CLIENTE', 'CANCELADO_PELO_BARBEIRO')
        AND (
            (a.dataHoraInicio < :fimSolicitado) AND 
            (a.dataHoraFim > :inicioSolicitado)
        )
    """)
    boolean existeConflitoDeHorario(@Param("barbeiroId") Long barbeiroId,
                                    @Param("inicioSolicitado") LocalDateTime inicioSolicitado,
                                    @Param("fimSolicitado") LocalDateTime fimSolicitado);
}