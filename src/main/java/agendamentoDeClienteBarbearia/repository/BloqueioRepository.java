package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Bloqueio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface BloqueioRepository extends JpaRepository<Bloqueio, Long> {
    // Busca bloqueios do dia para validar agenda
    @Query("SELECT b FROM Bloqueio b WHERE b.barbeiro.id = :id AND b.inicio BETWEEN :inicioDia AND :fimDia")
    List<Bloqueio> findBloqueiosDoDia(Long id, LocalDateTime inicioDia, LocalDateTime fimDia);
}