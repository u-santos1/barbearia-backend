package agendamentoDeClienteBarbearia.dtosResponse;

import agendamentoDeClienteBarbearia.TipoPlano;
import agendamentoDeClienteBarbearia.model.Barbeiro;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
        Boolean acessoBloqueado,
        Integer diasRestantesTrial,
        java.math.BigDecimal despesa
) {
    private static final int DIAS_TRIAL = 7;

    public DetalhamentoBarbeiroDTO(Barbeiro barbeiro) {
        this(
                barbeiro.getId(),
                barbeiro.getNome(),
                barbeiro.getEmail(),
                barbeiro.getEspecialidade() != null ? barbeiro.getEspecialidade() : "Barbeiro",
                barbeiro.getAtivo(),
                barbeiro.getDono() != null ? barbeiro.getDono().getId() : null,
                barbeiro.getWhatsappContato(),
                barbeiro.getPlano() != null ? barbeiro.getPlano().name() : "SOLO",
                barbeiro.getCreatedAt(),
                calcularBloqueio(barbeiro),
                calcularDiasRestantes(barbeiro),
                barbeiro.getDespesa()
        );
    }

    // Bloqueado = plano SOLO com trial expirado (>= 7 dias desde criação)
    private static Boolean calcularBloqueio(Barbeiro barbeiro) {
        if (barbeiro.getPlano() != TipoPlano.SOLO) return false;
        if (barbeiro.getCreatedAt() == null) return false;
        long diasPassados = ChronoUnit.DAYS.between(barbeiro.getCreatedAt(), LocalDateTime.now());
        return diasPassados >= DIAS_TRIAL;
    }

    // -1 = plano pago (não está em trial), 0-7 = dias restantes do trial
    private static Integer calcularDiasRestantes(Barbeiro barbeiro) {
        if (barbeiro.getPlano() != TipoPlano.SOLO) return -1;
        if (barbeiro.getCreatedAt() == null) return 0;
        long diasPassados = ChronoUnit.DAYS.between(barbeiro.getCreatedAt(), LocalDateTime.now());
        return (int) Math.max(0, DIAS_TRIAL - diasPassados);
    }
}
