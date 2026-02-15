package agendamentoDeClienteBarbearia.model;

import agendamentoDeClienteBarbearia.StatusAgendamento;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_agendamentos", indexes = {
        @Index(name = "idx_agendamento_data_inicio", columnList = "dataHoraInicio"),
        @Index(name = "idx_agendamento_barbeiro", columnList = "barbeiro_id"),
        @Index(name = "idx_agendamento_cliente", columnList = "cliente_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"barbeiro", "cliente", "servico"})
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false) // Mudado para false (Obrigatório)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id", nullable = false) // Mudado para false (Obrigatório)
    private Servico servico;

    @Column(nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(nullable = false)
    private LocalDateTime dataHoraFim;

    @Column(precision = 19, scale = 2)
    private BigDecimal valorCobrado;

    @Column(precision = 19, scale = 2)
    private BigDecimal valorTotal;

    @Column(precision = 19, scale = 2)
    private BigDecimal valorBarbeiro;

    @Column(precision = 19, scale = 2)
    private BigDecimal valorCasa;

    @Column(length = 500)
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private StatusAgendamento status;

    // Controle de concorrência JPA
    @Version
    private Long version;
}