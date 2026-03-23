package agendamentoDeClienteBarbearia.dtos;



import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;

import java.math.BigDecimal;
import java.util.List;

public record RelatorioFinanceiroCompletoDTO(
        BigDecimal totalFaturamento,
        BigDecimal totalCasa,
        BigDecimal totalComissoes,
        int quantidadeAtendimentos,
        List<DetalhamentoAgendamentoDTO> extrato
) {}
