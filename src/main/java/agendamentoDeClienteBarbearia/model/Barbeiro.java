package agendamentoDeClienteBarbearia.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_barbeiros")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Barbeiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true)
    private String email;

    private String especialidade; // Ex: "Barba", "Corte Clássico"

    // Opcional: Lista de agendamentos desse barbeiro (bidirecional)
    // @OneToMany(mappedBy = "barbeiro")
    // private List<Agendamento> agendamentos;
}
