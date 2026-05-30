package agendamentoDeClienteBarbearia.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "log_lembretes")
@Getter @Setter @NoArgsConstructor
public class LogLembrete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long agendamentoId;
    private Long regraId;
    private LocalDateTime dataEnvio;

    public LogLembrete(Long agendamentoId, Long regraId) {
        this.agendamentoId = agendamentoId;
        this.regraId = regraId;
        this.dataEnvio = LocalDateTime.now();
    }
}
