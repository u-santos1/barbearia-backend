package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.StatusAgendamento;
import agendamentoDeClienteBarbearia.model.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // ========================================================================
    // 1. CORE: VERIFICAÇÃO DE CONFLITOS (Está OK, pois não faz JOIN)
    // ========================================================================

    @Query("""
        SELECT COUNT(a) > 0 
        FROM Agendamento a 
        WHERE a.barbeiro.id = :barbeiroId 
        AND a.status NOT IN (
            agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO,
            agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO_PELO_CLIENTE, 
            agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO_PELO_BARBEIRO
        )
        AND (
            (a.dataHoraInicio < :fimSolicitado) AND 
            (a.dataHoraFim > :inicioSolicitado)
        )
    """)
    boolean existeConflitoDeHorario(@Param("barbeiroId") Long barbeiroId,
                                    @Param("inicioSolicitado") LocalDateTime inicioSolicitado,
                                    @Param("fimSolicitado") LocalDateTime fimSolicitado);

    @Query("""
        SELECT COUNT(a) > 0 
        FROM Agendamento a 
        WHERE a.barbeiro.email = :email 
        AND a.status NOT IN (
             agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO,
             agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO_PELO_CLIENTE, 
             agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO_PELO_BARBEIRO
        )
        AND (a.dataHoraInicio < :fim AND a.dataHoraFim > :inicio)
    """)
    boolean existeConflitoDeHorario(
            @Param("email") String emailBarbeiro,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    // ========================================================================
    // 2. DISPONIBILIDADE E AGENDA DIÁRIA (🚨 CORRIGIDO AQUI)
    // ========================================================================

    // Mudei para LEFT JOIN FETCH no servico para trazer os bloqueios também
    @Query("""
        SELECT a FROM Agendamento a 
        LEFT JOIN FETCH a.cliente 
        LEFT JOIN FETCH a.servico  
        WHERE a.barbeiro.id = :barbeiroId 
        AND a.dataHoraInicio BETWEEN :inicio AND :fim
        AND a.status NOT IN (
            agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO,
            agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO_PELO_CLIENTE, 
            agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO_PELO_BARBEIRO
        )
    """)
    List<Agendamento> findAgendaDoDia(@Param("barbeiroId") Long barbeiroId,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);

    @Query("""
    SELECT a FROM Agendamento a
    JOIN FETCH a.barbeiro
    LEFT JOIN FETCH a.cliente
    LEFT JOIN FETCH a.servico
    WHERE a.barbeiro.id = :barbeiroId
    AND a.barbeiro.dono.email = :emailLogado
    AND a.dataHoraInicio BETWEEN :inicio AND :fim
    ORDER BY a.dataHoraInicio ASC
""")
    List<Agendamento> findByBarbeiroIdAndDataHoraInicioBetween(
            @Param("barbeiroId") Long barbeiroId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("emailLogado") String emailLogado
    );

    // ========================================================================
    // 3. CONSULTAS OTIMIZADAS PARA O FRONTEND
    // ========================================================================

    @Query("""
    SELECT a FROM Agendamento a 
    JOIN FETCH a.barbeiro 
    JOIN FETCH a.servico 
    JOIN FETCH a.cliente
    WHERE a.cliente.telefone LIKE %:telefone% 
    AND a.dataHoraInicio > :agora 
    AND a.barbeiro.dono.email = :emailLogado
    AND a.status NOT IN (
        agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO, 
        agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO_PELO_CLIENTE, 
        agendamentoDeClienteBarbearia.StatusAgendamento.BLOQUEADO
    )
    ORDER BY a.dataHoraInicio ASC
""")
    List<Agendamento> buscarAgendamentosAtivosPorTelefone(
            @Param("telefone") String telefone,
            @Param("agora") LocalDateTime agora,
            @Param("emailLogado") String emailLogado
    );

    @Query("""
    SELECT a FROM Agendamento a
    JOIN FETCH a.barbeiro
    JOIN FETCH a.servico
    JOIN FETCH a.cliente
    WHERE a.cliente.id = :clienteId
    AND a.barbeiro.dono.email = :emailLogado
    ORDER BY a.dataHoraInicio DESC
""")
    List<Agendamento> findByClienteIdOrderByDataHoraInicioDesc(
            @Param("clienteId") Long clienteId,
            @Param("emailLogado") String emailLogado
    );

    // ========================================================================
    // 4. MÉTODOS SAAS E SEGURANÇA (🚨 CORRIGIDO AQUI TAMBÉM)
    // ========================================================================

    @Query("""
        SELECT a FROM Agendamento a 
        LEFT JOIN FETCH a.cliente c
        LEFT JOIN FETCH a.servico s
        WHERE a.barbeiro.email = :emailBarbeiro
        ORDER BY a.dataHoraInicio DESC
    """)
    List<Agendamento> findByBarbeiroEmailOrderByDataHoraInicioDesc(@Param("emailBarbeiro") String emailBarbeiro);

    @Query("""
        SELECT a FROM Agendamento a 
        JOIN FETCH a.barbeiro b
        LEFT JOIN b.dono d
        LEFT JOIN FETCH a.servico s
        WHERE (d.email = :emailDono OR b.email = :emailDono)
        AND a.dataHoraInicio BETWEEN :inicio AND :fim 
        AND a.status = :status
    """)
    List<Agendamento> buscarFinanceiroPorDono(
            @Param("emailDono") String emailDono,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("status") StatusAgendamento status
    );

    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.barbeiro
        LEFT JOIN FETCH a.servico  
        LEFT JOIN FETCH a.cliente
        WHERE a.dataHoraInicio BETWEEN :inicio AND :fim
        AND a.status = :status
        ORDER BY a.dataHoraInicio DESC
    """)
    List<Agendamento> findByDataHoraInicioBetweenAndStatus(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("status") StatusAgendamento status
    );

    @Query("""
    SELECT a FROM Agendamento a
    LEFT JOIN FETCH a.cliente
    LEFT JOIN FETCH a.servico 
    WHERE a.barbeiro.id = :barbeiroId
    AND a.barbeiro.dono.email = :emailLogado
    ORDER BY a.dataHoraInicio DESC
""")
    List<Agendamento> findByBarbeiroIdOrderByDataHoraInicioDesc(
            @Param("barbeiroId") Long barbeiroId,
            @Param("emailLogado") String emailLogado
    );

    @Query("""
        SELECT a FROM Agendamento a 
        LEFT JOIN FETCH a.cliente
        WHERE a.barbeiro.dono.id = :donoId 
        OR a.barbeiro.id = :donoId
        ORDER BY a.dataHoraInicio DESC
    """)
    List<Agendamento> findAllByBarbeiroDonoId(@Param("donoId") Long donoId);

    // 3. DASHBOARD DONO (🚨 CORRIGIDO: LEFT JOIN no Servico e Cliente)
    @Query("""
    SELECT a FROM Agendamento a 
    LEFT JOIN FETCH a.cliente
    LEFT JOIN FETCH a.servico
    LEFT JOIN FETCH a.barbeiro
    WHERE a.barbeiro.dono.email = :emailLogado
    ORDER BY a.dataHoraInicio DESC
""")
    List<Agendamento> findAllByDonoEmail(@Param("emailLogado") String emailLogado);

    @Query("SELECT a FROM Agendamento a WHERE a.id = :id AND a.barbeiro.dono.email = :emailDono")
    Optional<Agendamento> findByIdAndDonoEmail(Long id, String emailDono);
}

