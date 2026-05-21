package agendamentoDeClienteBarbearia.model;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_usos_assinatura")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class UsoAssinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assinatura_id", nullable = false)
    private AssinaturaCliente assinatura;

    @Column(name = "agendamento_id")
    private Long agendamentoId; // opcional — vínculo com agendamento

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;

    @Column(name = "data_uso", nullable = false)
    private LocalDateTime dataUso = LocalDateTime.now();

    @Column(length = 255)
    private String observacao;
}