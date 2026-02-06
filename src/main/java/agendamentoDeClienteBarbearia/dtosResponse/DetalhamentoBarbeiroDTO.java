package agendamentoDeClienteBarbearia.dtosResponse;


import agendamentoDeClienteBarbearia.model.Barbeiro;



import agendamentoDeClienteBarbearia.model.Barbeiro;

public record DetalhamentoBarbeiroDTO(
        Long id,
        String nome,
        String email,
        String especialidade,
        DetalhamentoBarbeiroDTO dono
) {
    public DetalhamentoBarbeiroDTO(Barbeiro barbeiro) {
        this(
                barbeiro.getId(),
                barbeiro.getNome(),
                barbeiro.getEmail(),
                // "Geral" soa mais profissional que "Barbeiro" como default
                barbeiro.getEspecialidade() != null ? barbeiro.getEspecialidade() : "Barbeiro",
                barbeiro.getDono() != null ? new DetalhamentoBarbeiroDTO(barbeiro.getDono()) : null
        );
    }
}