package agendamentoDeClienteBarbearia.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_password_reset_tokens", indexes = {
        @Index(name = "idx_token_reset", columnList = "token", unique = true),
        @Index(name = "idx_data_expiracao", columnList = "data_expiracao")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "barbeiro_id")
    private Barbeiro barbeiro;

    @Column(name = "dataExpiracao", nullable = false)
    private LocalDateTime dataExpiracao;

    public boolean isIspirado(){
        return LocalDateTime.now().isAfter(this.dataExpiracao);
    }

}
