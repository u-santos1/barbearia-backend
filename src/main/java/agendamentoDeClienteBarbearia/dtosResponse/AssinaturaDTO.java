package agendamentoDeClienteBarbearia.dtosResponse;


import agendamentoDeClienteBarbearia.StatusAssinatura;
import agendamentoDeClienteBarbearia.model.AssinaturaCliente;
import agendamentoDeClienteBarbearia.model.PlanoAssinatura;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

// -----------------------------------------------
// PLANO
// -----------------------------------------------
public class AssinaturaDTO {

    public record CriarPlanoDTO(
            @NotBlank String nome,
            String descricao,
            @NotNull @Positive BigDecimal preco,
            @NotNull @Min(1) Integer quantidadeCortes,
            @NotNull @Min(1) Integer vigenciaDias
    ) {}

    public record PlanoResponseDTO(
            Long id,
            String nome,
            String descricao,
            BigDecimal preco,
            Integer quantidadeCortes,
            Integer vigenciaDias,
            Boolean ativo
    ) {
        public static PlanoResponseDTO from(PlanoAssinatura p) {
            return new PlanoResponseDTO(
                    p.getId(), p.getNome(), p.getDescricao(),
                    p.getPreco(), p.getQuantidadeCortes(),
                    p.getVigenciaDias(), p.getAtivo()
            );
        }
    }

    // -----------------------------------------------
    // ASSINATURA DO CLIENTE
    // -----------------------------------------------
    public record AssinarDTO(
            @NotNull Long clienteId,
            @NotNull Long planoId,
            @NotBlank String formaPagamento, // MANUAL | MERCADO_PAGO
            String observacao
    ) {}

    public record AssinaturaResponseDTO(
            Long id,
            String clienteNome,
            String planNome,
            BigDecimal planoPreco,
            LocalDate dataInicio,
            LocalDate dataExpiracao,
            Integer cortesDisponiveis,
            Integer cortesUsados,
            Integer cortesRestantes,
            StatusAssinatura status,
            String formaPagamento
    ) {
        public static AssinaturaResponseDTO from(AssinaturaCliente a) {
            return new AssinaturaResponseDTO(
                    a.getId(),
                    a.getCliente().getNome(),
                    a.getPlano().getNome(),
                    a.getPlano().getPreco(),
                    a.getDataInicio(),
                    a.getDataExpiracao(),
                    a.getCortesDisponiveis(),
                    a.getCortesUsados(),
                    a.getCortesRestantes(),
                    a.getStatus(),
                    a.getFormaPagamento()
            );
        }
    }

    // -----------------------------------------------
    // USO DE CORTE
    // -----------------------------------------------
    public record UsarCorteDTO(
            @NotNull Long assinaturaId,
            Long agendamentoId, // opcional
            String observacao
    ) {}

    // -----------------------------------------------
    // WEBHOOK MERCADO PAGO
    // -----------------------------------------------
    public record MpWebhookDTO(
            String type,
            MpDataDTO data
    ) {}

    public record MpDataDTO(String id) {}
}