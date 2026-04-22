package agendamentoDeClienteBarbearia.model;



import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "tb_expediente", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"barbeiro_id", "diaSemana"}) // Um registro por dia da semana
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek diaSemana;

    @Column(nullable = false)
    private LocalTime abertura;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime almocoInicio;

    @Column(nullable = false)
    private LocalTime almocoFim;

    @Column(nullable = false)
    private LocalTime fechamento;

    @Column(nullable = false)
    private boolean trabalha;

    @Column(nullable = false)
    private boolean ativo;
}
