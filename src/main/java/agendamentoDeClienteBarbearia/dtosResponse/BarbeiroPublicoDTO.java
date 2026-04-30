package agendamentoDeClienteBarbearia.dtosResponse;

import agendamentoDeClienteBarbearia.model.Barbeiro;

public record BarbeiroPublicoDTO(
        Long id,
        String nome,
        String especialidade,
        String whatsappContato,
        Boolean ativo
) {
    // Construtor prático para mapear direto da Entidade
    public BarbeiroPublicoDTO(Barbeiro barbeiro) {
        this(
                barbeiro.getId(),
                barbeiro.getNome(),
                barbeiro.getEspecialidade(),
                barbeiro.getWhatsappContato(),
                barbeiro.getAtivo()
        );
    }}

