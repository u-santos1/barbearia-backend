package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Barbeiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {

    // =========================================================================
    // 1. VALIDAÇÕES E BUSCAS BÁSICAS
    // =========================================================================
    boolean existsByEmail(String email);

    Optional<Barbeiro> findByEmail(String email);

    // ⚠️ ALERTA DE SEGURANÇA:
    // Evite usar este método no Frontend/Controller. Ele retorna barbeiros de TODAS as barbearias.
    // Use apenas para rotinas internas de Super Admin.
    List<Barbeiro> findAllByAtivoTrue();

    // =========================================================================
    // 2. CONTROLE DE PLANO E LIMITES
    // =========================================================================
    // Conta quantos funcionários um dono tem (para bloquear cadastro no plano SOLO)
    long countByDonoId(Long idDono);

    // =========================================================================
    // 3. CONSULTAS SAAS (BLINDADAS POR LOJA)
    // =========================================================================

    // ✅ QUERY PRINCIPAL PARA O FRONTEND (Agendamento)
    // Lógica: "Traga este barbeiro SE (Ele for o Dono da loja X) OU (O chefe dele for o Dono X)"
    // E garante que ele esteja ATIVO (não demitido).
    @Query("""
        SELECT b FROM Barbeiro b 
        WHERE (b.id = :lojaId OR b.dono.id = :lojaId) 
        AND b.ativo = true
    """)
    List<Barbeiro> findAllByLoja(@Param("lojaId") Long lojaId);

    // Busca APENAS a equipe (funcionários), excluindo o dono da lista se necessário
    // Útil para a tela de "Gerenciar Equipe" do painel administrativo
    @Query("SELECT b FROM Barbeiro b WHERE b.dono.id = :donoId AND b.ativo = true")
    List<Barbeiro> findAllByDonoId(@Param("donoId") Long donoId);
}