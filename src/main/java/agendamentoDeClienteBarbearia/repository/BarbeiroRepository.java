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

    // Busca interna para validações
    List<Barbeiro> findAllByAtivoTrue();

    // Contagem para limitar planos (SaaS)
    long countByDonoId(Long idDono);

    // ✅ QUERY CORRIGIDA (SaaS Public View)
    // Busca:
    // 1. O próprio Dono (b.id = :lojaId)
    // 2. OU Funcionários desse Dono (b.dono.id = :lojaId)
    // 3. E garante que apenas os ATIVOS apareçam
    @Query("SELECT b FROM Barbeiro b WHERE (b.id = :lojaId OR b.dono.id = :lojaId) AND b.ativo = true")
    List<Barbeiro> findAllByLoja(@Param("lojaId") Long lojaId);
}