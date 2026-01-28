package agendamentoDeClienteBarbearia.dtosResponse;


import agendamentoDeClienteBarbearia.model.Barbeiro;

public record DetalhamentoBarbeiroDTO(Long id, String nome, String email, String especialidade) {
    public DetalhamentoBarbeiroDTO(Barbeiro barbeiro) {
        this(
                barbeiro.getId(),
                barbeiro.getNome(),
                barbeiro.getEmail(),
                // Proteção contra null na especialidade
                barbeiro.getEspecialidade() != null ? barbeiro.getEspecialidade() : "Barbeiro"
        );
    }
}