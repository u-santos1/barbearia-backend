package agendamentoDeClienteBarbearia.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO; // Importe o DTO correto
import jakarta.persistence.*;
import lombok.*;



import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tb_clientes", indexes = {
        @Index(name = "idx_cliente_email", columnList = "email"),
        @Index(name = "idx_cliente_telefone", columnList = "telefone")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(unique = true, length = 150)
    private String email;

    @Column(length = 20) // Telefone não precisa ser TEXT
    private String telefone;

    public Cliente(CadastroClienteDTO dados) {
        this.nome = dados.nome();
        this.email = dados.email();
        this.telefone = dados.telefone();
    }
}