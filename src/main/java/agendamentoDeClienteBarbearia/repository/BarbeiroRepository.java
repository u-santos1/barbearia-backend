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

    // Busca interna genérica
    List<Barbeiro> findAllByAtivoTrue();

    // Contagem para validar plano
    long countByDonoId(Long idDono);

    // =========================================================================
    // 1. QUERY ANTIGA (USADA PELO SERVICE / listarEquipe)
    // =========================================================================
    // O erro estava reclamando da falta DESTE método:
    @Query("SELECT b FROM Barbeiro b WHERE (b.dono.id = :idDono OR b.id = :idDono) AND b.ativo = true")
    List<Barbeiro> findAllByDonoIdOrId(@Param("idDono") Long idDono);

    // =========================================================================
    // 2. QUERY NOVA (USADA PELO CONTROLLER / listarBarbeiros público)
    // =========================================================================
    // Basicamente faz a mesma coisa, mas mantivemos nomes separados para organizar
    @Query("SELECT b FROM Barbeiro b WHERE (b.id = :lojaId OR b.dono.id = :lojaId) AND b.ativo = true")
    List<Barbeiro> findAllByLoja(@Param("lojaId") Long lojaId);

    @Query("SELECT b FROM Barbeiro b WHERE b.dono.id = :donoId AND b.ativo = true")
    List<Barbeiro> findAllByDonoId(@Param("donoId") Long donoId);

}