package agendamentoDeClienteBarbearia.dtosResponse;



import java.math.BigDecimal;

public record RelatorioBarbeiroDTO(Long id,
                                   String nomeBarbeiro,
                                   Long totalDeAgendamentos,
                                   Long totalDeCancelamentos,
                                   BigDecimal faturamentoBruto,
                                   BigDecimal valorRepasseBarbeiro) {

}
