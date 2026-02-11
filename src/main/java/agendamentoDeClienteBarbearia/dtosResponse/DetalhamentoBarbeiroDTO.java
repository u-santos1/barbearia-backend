package agendamentoDeClienteBarbearia.dtosResponse;

import agendamentoDeClienteBarbearia.model.Barbeiro;

public record DetalhamentoBarbeiroDTO(
        Long id,
        String nome,
        String email,
        String especialidade,
        Boolean ativo,
        Long donoId ,// ✅ Mudamos de Objeto para ID para evitar Erro 500 (Recursão Infinita)
        String whatsappContato
) {
    public DetalhamentoBarbeiroDTO(Barbeiro barbeiro) {
        this(
                barbeiro.getId(),
                barbeiro.getNome(),
                barbeiro.getEmail(),
                barbeiro.getEspecialidade() != null ? barbeiro.getEspecialidade() : "Barbeiro",
                barbeiro.getAtivo(),
                // Se tiver dono, pega o ID. Se não, é null (caso do próprio dono)
                barbeiro.getDono() != null ? barbeiro.getDono().getId() : null,
                barbeiro.getWhatsappContato()
        );
    }
}