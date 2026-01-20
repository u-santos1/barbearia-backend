package agendamentoDeClienteBarbearia.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_servicos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome; // Ex: "Corte Degradê"

    private String descricao;

    private BigDecimal preco; // Use BigDecimal para dinheiro, nunca Double!

    @Column(name = "duracao_minutos")
    private Integer duracaoEmMinutos; // Ex: 30, 45, 60
}
