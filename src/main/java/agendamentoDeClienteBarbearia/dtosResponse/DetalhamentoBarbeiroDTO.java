package agendamentoDeClienteBarbearia.dtosResponse;


import agendamentoDeClienteBarbearia.model.Barbeiro;
import java.time.LocalDateTime;

public record DetalhamentoBarbeiroDTO(
        Long id,
        String nome,
        String email,
        String especialidade,
        Boolean ativo,
        Long donoId,
        String whatsappContato,
        String plano,
        LocalDateTime createdAt,
        boolean acessoBloqueado
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
                barbeiro.getWhatsappContato(),
                barbeiro.getPlano() != null ? barbeiro.getPlano().name() : "SOLO",
                barbeiro.getCreatedAt(),
                barbeiro.isAcessoBloqueado()
        );
    }
}