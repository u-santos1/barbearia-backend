package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Bloqueio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;




import org.springframework.data.repository.query.Param;

;

public interface BloqueioRepository extends JpaRepository<Bloqueio, Long> {

    // Usado para verificar slots livres na agenda (AgendamentoService)
    // Traz bloqueios que tocam o dia solicitado
    @Query("SELECT b FROM Bloqueio b WHERE b.barbeiro.id = :id AND b.inicio < :fimDia AND b.fim > :inicioDia")
    List<Bloqueio> findBloqueiosDoDia(@Param("id") Long id,
                                      @Param("inicioDia") LocalDateTime inicioDia,
                                      @Param("fimDia") LocalDateTime fimDia);

    // Usado na validação ao CRIAR um bloqueio (BloqueioService)
    // Verifica Overlap (Colisão): (NovoInicio < FimAntigo) E (NovoFim > InicioAntigo)
    @Query("""
        SELECT COUNT(b) > 0 FROM Bloqueio b
        WHERE b.barbeiro.id = :barbeiroId
        AND (b.inicio < :fim AND b.fim > :inicio)
    """)
    boolean existeBloqueioNoPeriodo(@Param("barbeiroId") Long barbeiroId,
                                    @Param("inicio") LocalDateTime inicio,
                                    @Param("fim") LocalDateTime fim);

    // Lista bloqueios futuros (Para o barbeiro ver suas folgas)
    @Query("SELECT b FROM Bloqueio b WHERE b.barbeiro.id = :barbeiroId AND b.fim >= :agora ORDER BY b.inicio ASC")
    List<Bloqueio> findBloqueiosFuturos(@Param("barbeiroId") Long barbeiroId,
                                        @Param("agora") LocalDateTime agora);
}