package agendamentoDeClienteBarbearia.dtosResponse;


import agendamentoDeClienteBarbearia.model.Servico;

import java.math.BigDecimal;

public record DetalhamentoServicoDTO(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer duracaoEmMinutos
) {
    public DetalhamentoServicoDTO(Servico servico) {
        this(
                servico.getId(),
                servico.getNome(),
                servico.getDescricao(),
                servico.getPreco(),
                servico.getDuracaoEmMinutos()
        );
    }
}