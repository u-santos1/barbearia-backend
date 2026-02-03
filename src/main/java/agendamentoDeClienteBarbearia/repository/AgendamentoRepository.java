package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;



import agendamentoDeClienteBarbearia.StatusAgendamento;
import agendamentoDeClienteBarbearia.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // ========================================================================
    // VALIDAÇÃO DE CONFLITO (CRÍTICO)
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

    // ========================================================================
    // CONSULTAS OTIMIZADAS (COM JOIN FETCH PARA EVITAR N+1)
    // ========================================================================

    // Busca histórico do cliente (Traz Barbeiro e Serviço junto)
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.barbeiro
        JOIN FETCH a.servico
        WHERE a.cliente.id = :clienteId
        ORDER BY a.dataHoraInicio DESC
    """)
    List<Agendamento> findByClienteIdOrderByDataHoraInicioDesc(@Param("clienteId") Long clienteId);

    // Busca histórico do barbeiro "Meus Agendamentos" (Traz Cliente e Serviço junto)
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.cliente
        JOIN FETCH a.servico
        WHERE a.barbeiro.id = :barbeiroId
        ORDER BY a.dataHoraInicio DESC
    """)
    List<Agendamento> findByBarbeiroIdOrderByDataHoraInicioDesc(@Param("barbeiroId") Long barbeiroId);

    // Agenda do dia (Visualização Calendário) - Otimizada
    @Query("""
        SELECT a FROM Agendamento a 
        JOIN FETCH a.cliente 
        JOIN FETCH a.servico 
        WHERE a.barbeiro.id = :id 
        AND a.dataHoraInicio BETWEEN :inicio AND :fim
        AND a.status NOT IN ('CANCELADO_PELO_CLIENTE', 'CANCELADO_PELO_BARBEIRO')
    """)
    List<Agendamento> findAgendaDoDia(@Param("id") Long id,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);

    // Método simples para uso interno (sem fetch pesado se não precisar)
    List<Agendamento> findByBarbeiroIdAndDataHoraInicioBetween(Long idBarbeiro, LocalDateTime inicio, LocalDateTime fim);

    // ========================================================================
    // RELATÓRIOS
    // ========================================================================

    // Usado no Relatório Financeiro (AgendamentoService)
    List<Agendamento> findByDataHoraInicioBetweenAndStatus(LocalDateTime inicio,
                                                           LocalDateTime fim,
                                                           StatusAgendamento status);

    @Query("""
    SELECT a FROM Agendamento a 
    WHERE a.barbeiro.id = :barbeiroId 
    AND a.dataHoraInicio >= :inicio 
    AND a.dataHoraInicio < :fim
    AND a.status NOT IN ('CANCELADO_PELO_CLIENTE', 'CANCELADO_PELO_BARBEIRO')
""")
    List<Agendamento> findAgendamentosDoDia(
            @Param("barbeiroId") Long barbeiroId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim
    );
}