package agendamentoDeClienteBarbearia.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO; // Importe o DTO correto
import jakarta.persistence.*;
import lombok.*;

@Table(name = "tb_clientes")
@Entity(name = "Cliente")
@Getter
@Setter
@NoArgsConstructor // Obrigatório para o JPA/Hibernate
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true) // Aceita null, mas se tiver valor, deve ser único
    private String email;

    private String telefone;

    // --- ADICIONE ESTE CONSTRUTOR ---
    public Cliente(CadastroClienteDTO dados) {
        this.nome = dados.nome();
        this.email = dados.email();
        this.telefone = dados.telefone();
    }
}