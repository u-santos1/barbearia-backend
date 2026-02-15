package agendamentoDeClienteBarbearia.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_clientes",
        indexes = {
                @Index(name = "idx_cliente_telefone", columnList = "telefone")
        },
        // REGRA DE OURO DO SAAS:
        // O telefone pode repetir no banco, MAS NÃO para o mesmo Dono.
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cliente_telefone_por_loja", columnNames = {"dono_id", "telefone"}),
                @UniqueConstraint(name = "uk_cliente_email_por_loja", columnNames = {"dono_id", "email"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 150)
    private String email;

    @Column(length = 20, nullable = false)
    private String telefone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dono_id", nullable = false) // Cliente SEMPRE pertence a alguém
    private Barbeiro dono;

    // Construtor Limpo (Sem DTO) - Quem chama converte os dados
    public Cliente(String nome, String email, String telefone, Barbeiro dono) {
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.dono = dono;
    }

    // HashCode e Equals manuais para segurança com Proxy e Sets
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cliente)) return false;
        Cliente cliente = (Cliente) o;
        return getId() != null && getId().equals(cliente.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}