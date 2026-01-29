package agendamentoDeClienteBarbearia.model;

import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;



import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;
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

    @Column(nullable = false, unique = true) // Garante unicidade também no banco
    private String nome;

    private String descricao;

    @Column(nullable = false)
    private BigDecimal preco;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoEmMinutos;

    // 👇 O ERRO ESTAVA AQUI: Faltava declarar este campo
    @Column(nullable = false)
    private Boolean ativo = true; // Já nasce ativo por padrão

    // Construtor Inteligente (Baseado no DTO)
    public Servico(CadastroServicoDTO dados) {
        this.nome = dados.nome().trim();
        this.descricao = dados.descricao();
        this.preco = dados.preco();
        this.duracaoEmMinutos = dados.duracaoEmMinutos();
        this.ativo = true; // Reforça que ao criar é true
    }

    // Método utilitário para "excluir" sem perder histórico
    public void excluir() {
        this.ativo = false;
    }
}