package agendamentoDeClienteBarbearia.dtosResponse;

import agendamentoDeClienteBarbearia.model.Cliente;



public record DetalhamentoClienteDTO(Long id, String nome, String telefone, String email) {
    // Construtor que recebe a entidade Cliente
    public DetalhamentoClienteDTO(Cliente cliente) {
        this(
                cliente.getId(),
                cliente.getNome(),
                cliente.getTelefone(),
                cliente.getEmail()
        );
    }
}