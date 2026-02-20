package agendamentoDeClienteBarbearia.dtos;



import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;

import java.util.List;

public record RelatorioFinanceiroCompletoDTO(
        Double totalFaturamento,
        Double totalCasa,
        Double totalComissoes,
        int quantidadeAtendimentos,
        List<DetalhamentoAgendamentoDTO> extrato
) {}
