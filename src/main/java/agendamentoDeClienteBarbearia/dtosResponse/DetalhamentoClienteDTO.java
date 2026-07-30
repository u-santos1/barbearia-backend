package agendamentoDeClienteBarbearia.dtosResponse;

import agendamentoDeClienteBarbearia.model.Cliente;





import agendamentoDeClienteBarbearia.model.Cliente;



import agendamentoDeClienteBarbearia.model.Cliente;

public record DetalhamentoClienteDTO(
        Long id,
        String nome,
        String email,
        String telefone,
        Long donoId
) {
    public DetalhamentoClienteDTO(Cliente cliente) {
        this(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getTelefone(),
                cliente.getDono().getId()
        );
    }
}