package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Modifying
    @Transactional
    @Query(
            "DELETE FROM PasswordResetToken t WHERE t.dataExpiracao < :agora"
    )
    void deletarTokenExpirados(@Param("agora")LocalDateTime agora);
}
