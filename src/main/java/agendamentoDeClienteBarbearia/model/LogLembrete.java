package agendamentoDeClienteBarbearia.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "log_lembretes")
@Getter
@Setter
@NoArgsConstructor
public class LogLembrete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agendamento_id", nullable = false)
    private Long agendamentoId;

    @Column(name = "regra_id", nullable = false)
    private Long regraId;

    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio;


    @Column(nullable = false)
    private String status;

    public LogLembrete(Long agendamentoId, Long regraId) {
        this.agendamentoId = agendamentoId;
        this.regraId = regraId;
        this.dataEnvio = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
        this.status = "ENVIADO";
    }
}
