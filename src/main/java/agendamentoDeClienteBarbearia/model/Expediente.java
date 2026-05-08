package agendamentoDeClienteBarbearia.model;



import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private Barbeiro barbeiro;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek diaSemana;

    @Column
    private LocalTime abertura;



    @Column(name = "almoco_inicio")
    private LocalTime almocoInicio;

    @Column(name = "almoco_fim")
    private LocalTime almocoFim;

    @Column
    private LocalTime fechamento;

    @Column(nullable = false)
    private boolean trabalha;

    @Column(nullable = false)
    private boolean ativo;
}
