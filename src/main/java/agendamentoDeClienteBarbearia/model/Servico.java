package agendamentoDeClienteBarbearia.model;


import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tb_servicos", indexes = {
        @Index(name = "idx_servico_ativo", columnList = "ativo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal preco;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoEmMinutos;

    @Column(nullable = false)
    private Boolean ativo = true;

    public Servico(CadastroServicoDTO dados) {
        this.nome = dados.nome().trim();
        this.descricao = dados.descricao();
        this.preco = dados.preco();
        this.duracaoEmMinutos = dados.duracaoEmMinutos();
        this.ativo = true;
    }

    public void excluir() {
        this.ativo = false;
    }
}