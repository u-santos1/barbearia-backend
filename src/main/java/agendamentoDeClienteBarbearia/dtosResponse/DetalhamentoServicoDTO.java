package agendamentoDeClienteBarbearia.dtosResponse;

// Importe a classe Servico do pacote correto (verifique se é .model ou .domain.servico)
import agendamentoDeClienteBarbearia.model.Servico;
import java.math.BigDecimal;

public record DetalhamentoServicoDTO(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer duracaoEmMinutos
) {
    // Construtor auxiliar para converter Entidade -> DTO
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