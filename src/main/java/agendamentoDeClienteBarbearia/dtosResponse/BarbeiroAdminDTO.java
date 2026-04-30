package agendamentoDeClienteBarbearia.dtosResponse;

import agendamentoDeClienteBarbearia.model.Barbeiro;

import java.time.LocalDateTime;

public record BarbeiroAdminDTO(
        Long id,
        String nome,
        String email,
        String especialidade,
        Boolean ativo,
        String whatsappContato,
        String plano,
        LocalDateTime createdAt
) {
    public BarbeiroAdminDTO(Barbeiro barbeiro) {
        this(
                barbeiro.getId(),
                barbeiro.getNome(),
                barbeiro.getEmail(),
                barbeiro.getEspecialidade(),
                barbeiro.getAtivo(),
                barbeiro.getWhatsappContato(),
                barbeiro.getPlano().name(),
                barbeiro.getCreatedAt()
        );
    }
}
