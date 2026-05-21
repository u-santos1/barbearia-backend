package agendamentoDeClienteBarbearia.model;




import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_planos_assinatura")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class PlanoAssinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal preco;

    @Column(name = "quantidade_cortes", nullable = false)
    private Integer quantidadeCortes;

    @Column(name = "vigencia_dias", nullable = false)
    private Integer vigenciaDias = 30;

    @Column(nullable = false)
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dono_id", nullable = false)
    private Barbeiro dono;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void desativar() {
        this.ativo = false;
    }
}
