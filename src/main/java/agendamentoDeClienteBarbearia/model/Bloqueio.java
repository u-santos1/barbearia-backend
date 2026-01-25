package agendamentoDeClienteBarbearia.model;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Table(name = "bloqueios")
@Entity(name = "Bloqueio")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class Bloqueio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "barbeiro_id")
    private Barbeiro barbeiro;

    private LocalDateTime inicio;
    private LocalDateTime fim;
    private String motivo; // Ex: "Almoço", "Médico"
}
