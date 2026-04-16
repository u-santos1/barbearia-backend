package agendamentoDeClienteBarbearia.dtosResponse;

import agendamentoDeClienteBarbearia.dtos.AgendamentoDTO;
import agendamentoDeClienteBarbearia.model.Agendamento;
import agendamentoDeClienteBarbearia.model.Barbeiro;

import java.math.BigDecimal;

public record RelatorioBarbeiroDTO(Long id,
                                   String nomeBarbeiro,
                                   Long totalDeAgendamentos,
                                   Long totalDeCancelamentos,
                                   BigDecimal faturamentoBruto,
                                   BigDecimal valorRepasseBarbeiro) {

}
