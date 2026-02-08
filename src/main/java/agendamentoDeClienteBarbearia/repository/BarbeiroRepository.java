package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Barbeiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {

    boolean existsByEmail(String email);
    Optional<Barbeiro> findByEmail(String email);

    // Lista genérica de ativos
    List<Barbeiro> findAllByAtivoTrue();

    // Contagem para validar plano
    long countByDonoId(Long idDono);

    // Busca apenas funcionários de um dono
    @Query("SELECT b FROM Barbeiro b WHERE b.dono.id = :donoId AND b.ativo = true")
    List<Barbeiro> findAllByDonoId(@Param("donoId") Long donoId);

    // =========================================================================
    // ✅ QUERY CORRETA PARA O FRONTEND (Busca Dono + Equipe)
    // =========================================================================
    @Query("SELECT b FROM Barbeiro b WHERE (b.id = :lojaId OR b.dono.id = :lojaId) AND b.ativo = true")
    List<Barbeiro> findAllByLoja(@Param("lojaId") Long lojaId);
}