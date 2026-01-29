package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Barbeiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {
    boolean existsByEmail(String email);

    @Query("""
    SELECT SUM(a.valorCobrado) 
    FROM Agendamento a 
    WHERE a.barbeiro.id = :idBarbeiro 
    AND a.dataHoraInicio BETWEEN :inicio AND :fim
    AND a.status = 'CONCLUIDO'
""")
    BigDecimal faturamentoTotal(Long idBarbeiro, LocalDateTime inicio, LocalDateTime fim);
    Optional<Barbeiro> findByEmail(String email);
    // BarbeiroRepository.java
    List<Barbeiro> findAllByTrabalhaComoBarbeiroTrue();

    long countByDonoId(Long donoId);
    }