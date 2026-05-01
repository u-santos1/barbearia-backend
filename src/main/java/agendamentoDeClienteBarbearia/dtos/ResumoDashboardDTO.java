package agendamentoDeClienteBarbearia.dtos;

import java.math.BigDecimal;

public record ResumoDashboardDTO(Long agendamentosHoje,
                                 Long totalClientesCadastrados,
                                 BigDecimal faturamentoHoje,
                                 Integer taxaOcupacao) {
}
