package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Agendamento;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// No arquivo BarbeiroRepository.java
public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {

    boolean existsByEmail(String email);

    Optional<Barbeiro> findByEmail(String email);

    // Busca customizada para o Multi-Tenancy
    // Traz todos onde (dono_id = X) OU (id = X) -> Traz a equipe e o chefe
    @Query("SELECT b FROM Barbeiro b WHERE (b.dono.id = :idDono OR b.id = :idDono) AND b.ativo = true")
    List<Barbeiro> findAllByDonoIdOrId(Long idDono);

    // Contagem para validar plano
    long countByDonoId(Long idDono);

    List<Barbeiro> findAllByAtivoTrue();

}