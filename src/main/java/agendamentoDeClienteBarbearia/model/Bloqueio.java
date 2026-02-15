package agendamentoDeClienteBarbearia.model;





import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_bloqueios", indexes = {
        @Index(name = "idx_bloqueio_periodo", columnList = "inicio, fim")
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Bloqueio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Column(length = 100)
    private String motivo;

    @AssertTrue(message = "A data final deve ser posterior à inicial")
    public boolean isPeriodoValido() {
        if (inicio == null || fim == null) return false;
        return fim.isAfter(inicio);
}
}