package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.TipoPlano;

import agendamentoDeClienteBarbearia.dtos.RespostaPixDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import com.mercadopago.MercadoPagoConfig;

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
                            @Value("${plano.multi.valor:89.90}") BigDecimal valorPlano) {
        this.barbeiroRepository = barbeiroRepository;
        this.mpAccessToken = token;
        this.webhookUrl = webhookUrl;
        this.valorPlano = valorPlano;
    }

    // Inicializa o SDK após a injeção de dependências
    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(mpAccessToken);
        log.info("SDK Mercado Pago inicializado.");
    }

    // ========================================================
    // 1. GERA O PIX (CHECKOUT TRANSPARENTE)
    // ========================================================
    public RespostaPixDTO gerarPixUpgrade(Long idBarbeiro) {
        Barbeiro barbeiro = barbeiroRepository.findById(idBarbeiro)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado"));

        if (barbeiro.getEmail() == null) {
            throw new RegraDeNegocioException("E-mail obrigatório para pagamento.");
        }

        try {
            PaymentClient client = new PaymentClient();

            // Monta os dados do Pagador
            // DICA: Em produção, adicione CPF (Identification) para evitar recusa do banco
            PaymentPayerRequest payer = PaymentPayerRequest.builder()
                    .email(barbeiro.getEmail())
                    .firstName(barbeiro.getNome())
                    .build();

            // Cria a requisição de pagamento
            PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                    .transactionAmount(valorPlano)
                    .description("Upgrade Plano MULTI - Barbearia")
                    .paymentMethodId("pix")
                    .notificationUrl(webhookUrl) // URL injetada do properties
                    .payer(payer)
                    .externalReference(barbeiro.getId().toString()) // Vínculo ID Barbeiro
                    .build();

            Payment payment = client.create(createRequest);

            // Extrai os dados do PIX para o Frontend
            String qrCode = payment.getPointOfInteraction().getTransactionData().getQrCode();
            String base64 = payment.getPointOfInteraction().getTransactionData().getQrCodeBase64();

            return new RespostaPixDTO(
                    payment.getId(),
                    qrCode,
                    base64,
                    payment.getStatus()
            );

        } catch (MPApiException e) {
            log.error("Erro API MP: {}", e.getApiResponse().getContent());
            throw new RegraDeNegocioException("Erro ao gerar PIX: " + e.getMessage());
        } catch (MPException e) {
            log.error("Erro interno MP", e);
            throw new RegraDeNegocioException("Erro interno no pagamento.");
        }
    }

    // ========================================================
    // 2. WEBHOOK (CALLBACK)
    // ========================================================
    @Transactional
    public void processarWebhook(Long idPagamento) {
        log.info("Webhook recebido. Pagamento ID: {}", idPagamento);

        try {
            PaymentClient client = new PaymentClient();
            Payment pagamento = client.get(idPagamento);

            if ("approved".equals(pagamento.getStatus())) {
                atualizarPlanoUsuario(pagamento);
            } else {
                log.info("Pagamento {} processado, mas status é {}", idPagamento, pagamento.getStatus());
            }

        } catch (MPException | MPApiException e) {
            // Loga o erro mas não lança exception para não travar a fila do webhook
            // Se lançar erro 500 aqui, o Mercado Pago vai reenviar a cada hora.
            log.error("Erro ao processar webhook ID {}", idPagamento, e);
        }
    }

    private void atualizarPlanoUsuario(Payment pagamento) {
        String externalRef = pagamento.getExternalReference();

        if (externalRef == null) {
            log.warn("Pagamento {} sem External Reference. Ignorando.", pagamento.getId());
            return;
        }

        try {
            Long idBarbeiro = Long.parseLong(externalRef);
            Barbeiro barbeiro = barbeiroRepository.findById(idBarbeiro).orElse(null);

            if (barbeiro == null) {
                log.error("Barbeiro ID {} não encontrado para o pagamento {}.", idBarbeiro, pagamento.getId());
                return;
            }

            // Lógica de Ativação
            if (barbeiro.getPlano() != TipoPlano.MULTI) {
                barbeiro.setPlano(TipoPlano.MULTI);
                barbeiroRepository.save(barbeiro);
                log.info("✅ Plano MULTI ativado para {}", barbeiro.getNome());
            } else {
                log.info("Usuário {} já é MULTI. Ignorando duplicidade.", barbeiro.getNome());
            }

        } catch (NumberFormatException e) {
            log.error("External Reference inválida: {}", externalRef);
        }
    }
}