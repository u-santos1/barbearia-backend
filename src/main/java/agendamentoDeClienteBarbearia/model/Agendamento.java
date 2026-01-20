package agendamentoDeClienteBarbearia.model;

import jakarta.persistence.*;
import lombok.*;
import agendamentoDeClienteBarbearia.StatusAgendamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_agendamentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne // Vários agendamentos para um barbeiro
    @JoinColumn(name = "barbeiro_id")
    private Barbeiro barbeiro;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "servico_id")
    private Servico servico;

    // A data e hora exata que começa
    @Column(nullable = false)
    private LocalDateTime dataHoraInicio;

    // A data e hora exata que termina (calculado: inicio + tempo do serviço)
    @Column(nullable = false)
    private LocalDateTime dataHoraFim;

    private BigDecimal valorCobrado;

    // Enum para status: AGENDADO, CANCELADO, FINALIZADO
    @Enumerated(EnumType.STRING)
    private StatusAgendamento status;
}
