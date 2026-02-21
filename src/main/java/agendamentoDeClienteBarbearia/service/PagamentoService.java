package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.TipoPlano;
import agendamentoDeClienteBarbearia.dtos.RespostaPixDTO;
import agendamentoDeClienteBarbearia.dtos.UpgradeRequestDTO; // Certifique-se de ter este DTO
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest; // Importação necessária
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class PagamentoService {

    private final BarbeiroRepository barbeiroRepository;
    private final String mpAccessToken;
    private final String webhookUrl;
    private final BigDecimal valorPlano;

    public PagamentoService(BarbeiroRepository barbeiroRepository,
                            @Value("${mercadopago.access_token}") String token,
                            @Value("${mercadopago.webhook.url}") String webhookUrl,
                            @Value("${plano.multi.valor:1.00}") BigDecimal valorPlano) { // Valor teste padrão
        this.barbeiroRepository = barbeiroRepository;
        this.mpAccessToken = token;
        this.webhookUrl = webhookUrl;
        this.valorPlano = valorPlano;
    }

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(mpAccessToken);
        log.info("SDK Mercado Pago inicializado.");
    }

    // ========================================================
    // 1. GERA O PIX (AGORA RECEBENDO NOME E CPF)
    // ========================================================
    public RespostaPixDTO gerarPixUpgrade(Long idBarbeiro, UpgradeRequestDTO dados) {
        Barbeiro barbeiro = barbeiroRepository.findById(idBarbeiro)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado"));

        try {
            PaymentClient client = new PaymentClient();

            // Monta os dados do Pagador com CPF (Obrigatório em Produção)
            PaymentPayerRequest payer = PaymentPayerRequest.builder()
                    .email(barbeiro.getEmail())
                    .firstName(dados.nome()) // Nome vindo do SweetAlert
                    .identification(IdentificationRequest.builder()
                            .type("CPF")
                            .number(dados.cpf().replaceAll("\\D", "")) // Remove pontos/traços
                            .build())
                    .build();

            PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                    .transactionAmount(valorPlano)
                    .description("Assinatura Barber Pro - " + barbeiro.getNome())
                    .paymentMethodId("pix")
                    .notificationUrl(webhookUrl)
                    .payer(payer)
                    .externalReference(barbeiro.getId().toString())
                    .build();

            Payment payment = client.create(createRequest);

            // Verificação de erro do Mercado Pago (Caso retorne rejected de cara)
            if (payment.getPointOfInteraction() == null) {
                log.error("Mercado Pago não gerou ponto de interação. Status: {}", payment.getStatus());
                return new RespostaPixDTO(payment.getId(), null, null, null, payment.getStatus());
            }

            // Extrai os dados do PIX
            String qrCode = payment.getPointOfInteraction().getTransactionData().getQrCode();
            String base64 = payment.getPointOfInteraction().getTransactionData().getQrCodeBase64();
            String ticketUrl = payment.getPointOfInteraction().getTransactionData().getTicketUrl();

            return new RespostaPixDTO(
                    payment.getId(),
                    qrCode,
                    base64,
                    ticketUrl, // Link alternativo
                    payment.getStatus()
            );

        } catch (MPApiException e) {
            log.error("Erro API MP: {}", e.getApiResponse().getContent());
            throw new RegraDeNegocioException("Erro Mercado Pago: " + e.getApiResponse().getContent());
        } catch (MPException e) {
            log.error("Erro interno MP", e);
            throw new RegraDeNegocioException("Erro interno no processamento do pagamento.");
        }
    }

    // ========================================================
    // 2. WEBHOOK (SEM ALTERAÇÕES)
    // ========================================================
    @Transactional
    public void processarWebhook(Long idPagamento) {
        log.info("Processando Webhook para Pagamento ID: {}", idPagamento);
        try {
            PaymentClient client = new PaymentClient();
            Payment pagamento = client.get(idPagamento);

            if ("approved".equals(pagamento.getStatus())) {
                atualizarPlanoUsuario(pagamento);
            }
        } catch (MPException | MPApiException e) {
            log.error("Erro ao consultar pagamento no webhook: {}", idPagamento);
        }
    }

    private void atualizarPlanoUsuario(Payment pagamento) {
        String externalRef = pagamento.getExternalReference();
        if (externalRef == null) return;

        try {
            Long idBarbeiro = Long.parseLong(externalRef);
            barbeiroRepository.findById(idBarbeiro).ifPresent(barbeiro -> {
                if (barbeiro.getPlano() != TipoPlano.MULTI) {
                    barbeiro.setPlano(TipoPlano.MULTI);
                    barbeiroRepository.save(barbeiro);
                    log.info("✅ Plano MULTI ativado: {}", barbeiro.getNome());
                }
            });
        } catch (Exception e) {
            log.error("Erro ao atualizar plano: {}", e.getMessage());
        }
    }
}