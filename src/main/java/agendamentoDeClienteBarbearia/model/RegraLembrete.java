package agendamentoDeClienteBarbearia.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "regras_lembrete")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class RegraLembrete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String tempo; // Ex: "Personalizado", "2 horas antes"

    private LocalTime hora;

    @Column(columnDefinition = "TEXT")
    private String msg;

    private Boolean ativo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dono_id")
    private Barbeiro dono;
}