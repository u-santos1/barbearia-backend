package agendamentoDeClienteBarbearia.model;


import agendamentoDeClienteBarbearia.StatusAssinatura;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_assinaturas_clientes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class AssinaturaCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plano_id", nullable = false)
    private PlanoAssinatura plano;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barbeiro_id", nullable = false)
    private Barbeiro barbeiro;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_expiracao", nullable = false)
    private LocalDate dataExpiracao;

    @Column(name = "cortes_disponiveis", nullable = false)
    private Integer cortesDisponiveis;

    @Column(name = "cortes_usados", nullable = false)
    private Integer cortesUsados = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAssinatura status = StatusAssinatura.ATIVA;

    @Column(name = "forma_pagamento", length = 30)
    private String formaPagamento; // MANUAL | MERCADO_PAGO

    @Column(name = "pagamento_mp_id", length = 100)
    private String pagamentoMpId;

    @Column(name = "pagamento_mp_status", length = 30)
    private String pagamentoMpStatus;

    @Column(length = 500)
    private String observacao;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // -----------------------------------------------
    // Regras de negócio
    // -----------------------------------------------

    public int getCortesRestantes() {
        return cortesDisponiveis - cortesUsados;
    }

    public boolean temCortesDisponiveis() {
        return getCortesRestantes() > 0 && status == StatusAssinatura.ATIVA;
    }

    public boolean estaVigente() {
        return !LocalDate.now().isAfter(dataExpiracao) && status == StatusAssinatura.ATIVA;
    }

    public void usarCorte() {
        if (!temCortesDisponiveis()) {
            throw new IllegalStateException("Sem cortes disponíveis nesta assinatura.");
        }
        if (!estaVigente()) {
            throw new IllegalStateException("Assinatura expirada ou inativa.");
        }
        this.cortesUsados++;
        if (this.cortesUsados >= this.cortesDisponiveis) {
            this.status = StatusAssinatura.EXPIRADA;
        }
    }

    public void verificarExpiracao() {
        if (LocalDate.now().isAfter(dataExpiracao) && status == StatusAssinatura.ATIVA) {
            this.status = StatusAssinatura.EXPIRADA;
        }
    }
}
