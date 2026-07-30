package agendamentoDeClienteBarbearia.dtos;




import java.math.BigDecimal;

public record AtualizacaoBarbeiroDTO(
        String descricao,
        String barbeariaNome,
        String corPrimaria,
        String imagemFundo,
        String whatsappContato,
        String instagramUrl,
        String mensagemOla,
        BigDecimal despesa
) {}