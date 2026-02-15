package agendamentoDeClienteBarbearia.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tb_servicos",
        indexes = { @Index(name = "idx_servico_ativo", columnList = "ativo") },
        // Garante que o nome é único APENAS dentro da mesma barbearia
        uniqueConstraints = { @UniqueConstraint(name = "uk_servico_nome_dono", columnNames = {"dono_id", "nome"}) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100) // Removido unique=true global
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal preco;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoEmMinutos;

    @Column(nullable = false)
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY) // Lazy é melhor aqui
    @JoinColumn(name = "dono_id", nullable = false) // Serviço sempre tem dono
    private Barbeiro dono;

    public void excluir() {
        this.ativo = false;
    }
}