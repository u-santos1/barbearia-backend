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
    // 1. CORE: VERIFICAÇÃO DE CONFLITOS (Necessário para o Agendar)
    // ========================================================================

    @Query("""
        SELECT COUNT(a) > 0 
        FROM Agendamento a 
        WHERE a.barbeiro.id = :barbeiroId 
        AND a.status NOT IN (
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

    // Sobrecarga para Bloqueios via E-mail (Service usa este)
    @Query("""
        SELECT COUNT(a) > 0 
        FROM Agendamento a 
        WHERE a.barbeiro.email = :email 
        AND a.status NOT IN (
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
    // 2. DISPONIBILIDADE E AGENDA DIÁRIA
    // ========================================================================

    @Query("""
        SELECT a FROM Agendamento a 
        JOIN FETCH a.cliente 
        JOIN FETCH a.servico 
        WHERE a.barbeiro.id = :barbeiroId 
        AND a.dataHoraInicio BETWEEN :inicio AND :fim
        AND a.status NOT IN (
            agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO_PELO_CLIENTE, 
            agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO_PELO_BARBEIRO
        )
    """)
    List<Agendamento> findAgendaDoDia(@Param("barbeiroId") Long barbeiroId,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);

    // Mantido para compatibilidade (mas recomenda-se usar o findAgendaDoDia acima)
    List<Agendamento> findByBarbeiroIdAndDataHoraInicioBetween(Long barbeiroId, LocalDateTime inicio, LocalDateTime fim);

    // ========================================================================
    // 3. CONSULTAS OTIMIZADAS PARA O FRONTEND (JOIN FETCH)
    // ========================================================================

    // Busca do Cliente (Pelo Telefone - Blindada contra Timezone e Lazy)
    @Query("""
        SELECT a FROM Agendamento a 
        JOIN FETCH a.barbeiro 
        JOIN FETCH a.servico 
        JOIN FETCH a.cliente
        WHERE a.cliente.telefone LIKE %:telefone% 
        AND a.dataHoraInicio > :agora 
        AND a.status NOT IN (
            agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO, 
            agendamentoDeClienteBarbearia.StatusAgendamento.CANCELADO_PELO_CLIENTE, 
            agendamentoDeClienteBarbearia.StatusAgendamento.BLOQUEADO
        )
        ORDER BY a.dataHoraInicio ASC
    """)
    List<Agendamento> buscarAgendamentosAtivosPorTelefone(
            @Param("telefone") String telefone,
            @Param("agora") LocalDateTime agora
    );

    // Histórico do Cliente (Perfil)
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.barbeiro
        JOIN FETCH a.servico
        WHERE a.cliente.id = :clienteId
        ORDER BY a.dataHoraInicio DESC
    """)
    List<Agendamento> findByClienteIdOrderByDataHoraInicioDesc(@Param("clienteId") Long clienteId);

    // ========================================================================
    // 4. MÉTODOS SAAS E SEGURANÇA (Estes estavam faltando para o seu Service)
    // ========================================================================

    // LISTAR TODOS DO DONO (Painel Admin - SaaS)
    // O Service chama 'findAllByDonoEmail' passando String
    @Query("""
        SELECT a FROM Agendamento a 
        JOIN FETCH a.barbeiro b
        JOIN FETCH a.servico s
        JOIN FETCH a.cliente c
        WHERE b.dono.email = :emailDono OR b.email = :emailDono
        ORDER BY a.dataHoraInicio DESC
    """)
    List<Agendamento> findAllByDonoEmail(@Param("emailDono") String emailDono);

    // LISTAR MEUS AGENDAMENTOS (Painel Barbeiro)
    // O Service chama 'findByBarbeiroEmailOrderByDataHoraInicioDesc' passando String
    @Query("""
        SELECT a FROM Agendamento a 
        JOIN FETCH a.cliente c
        JOIN FETCH a.servico s
        WHERE a.barbeiro.email = :emailBarbeiro
        ORDER BY a.dataHoraInicio DESC
    """)
    List<Agendamento> findByBarbeiroEmailOrderByDataHoraInicioDesc(@Param("emailBarbeiro") String emailBarbeiro);

    // RELATÓRIO FINANCEIRO (SaaS)
    // O Service chama 'buscarFinanceiroPorDono'
    @Query("""
        SELECT a FROM Agendamento a 
        JOIN FETCH a.barbeiro b
        WHERE (b.dono.email = :emailDono OR b.email = :emailDono)
        AND a.dataHoraInicio BETWEEN :inicio AND :fim 
        AND a.status = :status
    """)
    List<Agendamento> buscarFinanceiroPorDono(
            @Param("emailDono") String emailDono,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("status") StatusAgendamento status
    );

    // Método legado (Mantido para não quebrar códigos antigos, se houver)
    List<Agendamento> findByDataHoraInicioBetweenAndStatus(LocalDateTime inicio, LocalDateTime fim, StatusAgendamento status);

    // Histórico do Barbeiro por ID (Legado ou uso interno)
    @Query("""
        SELECT a FROM Agendamento a
        JOIN FETCH a.cliente
        JOIN FETCH a.servico
        WHERE a.barbeiro.id = :barbeiroId
        ORDER BY a.dataHoraInicio DESC
    """)
    List<Agendamento> findByBarbeiroIdOrderByDataHoraInicioDesc(@Param("barbeiroId") Long barbeiroId);

    // Visão do dono por ID (Legado)
    @Query("""
        SELECT a FROM Agendamento a 
        WHERE a.barbeiro.dono.id = :donoId 
        OR a.barbeiro.id = :donoId
        ORDER BY a.dataHoraInicio DESC
    """)
    List<Agendamento> findAllByBarbeiroDonoId(@Param("donoId") Long donoId);
}