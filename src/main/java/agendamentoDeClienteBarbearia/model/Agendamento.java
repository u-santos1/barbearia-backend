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
@EqualsAndHashCode(of = "id") // Performance: Compara apenas ID
@ToString(exclude = {"barbeiro", "cliente", "servico"}) // Evita loop infinito no log
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Performance: FetchType.LAZY carrega o objeto apenas quando for usado (getBarbeiro)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true) // Mudou aqui
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servico_id", nullable = true) // Mudou aqui
    private Servico servico;
    @Column(nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(nullable = false)
    private LocalDateTime dataHoraFim;

    // Financeiro: Sempre BigDecimal (precisão 19,2 ou 19,4)
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
}