package agendamentoDeClienteBarbearia.dtos;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AtualizacaoBarbeiroDTO(
        String descricao,
        String barbeariaNome,
        String corPrimaria,
        String imagemFundo,
        String whatsappContato,
        String instagramUrl,
        String mensagemOla
) {}