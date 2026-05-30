package agendamentoDeClienteBarbearia.dtosResponse;

import agendamentoDeClienteBarbearia.model.RegraLembrete;
import java.time.LocalTime;

public record DetalhamentoRegraLembreteDTO(
        Long id,
        String nome,
        String tempo,
        LocalTime hora,
        String msg,
        Boolean ativo
) {
    public DetalhamentoRegraLembreteDTO(RegraLembrete regra) {
        this(regra.getId(), regra.getNome(), regra.getTempo(), regra.getHora(), regra.getMsg(), regra.getAtivo());
    }
}
