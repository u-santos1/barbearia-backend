package agendamentoDeClienteBarbearia.dtosResponse;


import agendamentoDeClienteBarbearia.model.Barbeiro;

public record DetalhamentoBarbeiroDTO(
        Long id,
        String nome,
        String email,
        String especialidade
) {
    // Construtor auxiliar: Recebe a Entidade e extrai os dados
    public DetalhamentoBarbeiroDTO(Barbeiro barbeiro) {
        this(
                barbeiro.getId(),
                barbeiro.getNome(),
                barbeiro.getEmail(),
                barbeiro.getEspecialidade()
        );
    }
}