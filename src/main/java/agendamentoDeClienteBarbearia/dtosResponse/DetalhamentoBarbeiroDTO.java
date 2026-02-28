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
        String plano,              // ✅ O Frontend precisa disso para liberar a tela
        LocalDateTime createdAt    // ✅ O Frontend precisa disso para contar os 15 dias
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

                // ✅ Transforma o Enum TipoPlano em String ("SOLO" ou "MULTI").
                // Se por algum motivo estiver nulo no banco, enviamos "SOLO" por segurança.
                barbeiro.getPlano() != null ? barbeiro.getPlano().name() : "SOLO",

                // ✅ Pega a data de criação.
                // Atenção: Se na sua entidade Barbeiro o campo se chamar "dataCriacao", mude aqui para barbeiro.getDataCriacao()
                barbeiro.getCreatedAt()
        );
    }
}