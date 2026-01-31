package agendamentoDeClienteBarbearia.service;





import agendamentoDeClienteBarbearia.TipoPlano;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j // Logs para monitorar pagamentos
@Service
public class PagamentoService {

    private final BarbeiroRepository barbeiroRepository;

    // Valor configurável no application.properties
    private final BigDecimal valorPlano;

    public PagamentoService(BarbeiroRepository barbeiroRepository,
                            @Value("${mercadopago.access_token}") String token,
                            @Value("${plano.multi.valor:89.90}") BigDecimal valorPlano) {
        this.barbeiroRepository = barbeiroRepository;
        this.valorPlano = valorPlano;

        // Inicializa o SDK uma vez
        MercadoPagoConfig.setAccessToken(token);
    }

    // ========================================================
    // 1. GERA O PIX (CHECKOUT)
    // ========================================================
    public Payment gerarPixUpgrade(Long idBarbeiro) {
        Barbeiro barbeiro = barbeiroRepository.findById(idBarbeiro)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado"));

        if (barbeiro.getEmail() == null || barbeiro.getEmail().isBlank()) {
            throw new RegraDeNegocioException("O usuário precisa de um e-mail válido para gerar o pagamento.");
        }

        try {
            log.info("Iniciando geração de PIX para barbeiro ID: {}", idBarbeiro);

            PaymentClient client = new PaymentClient();

            PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                    .transactionAmount(valorPlano)
                    .description("Assinatura Plano MULTI (Mensal)")
                    .paymentMethodId("pix")
                    .notificationUrl("https://sua-api.com/api/pagamentos/webhook") // Importante para Produção
                    .payer(PaymentPayerRequest.builder()
                            .email(barbeiro.getEmail())
                            .firstName(barbeiro.getNome())
                            .build())
                    // EXTERNAL REFERENCE: O vínculo entre o $ e o Usuário
                    .externalReference(barbeiro.getId().toString())
                    .build();

            Payment payment = client.create(createRequest);

            log.info("PIX gerado com sucesso. ID Pagamento: {}", payment.getId());
            return payment;

        } catch (MPApiException apiException) {
            log.error("Erro API Mercado Pago: {} - {}", apiException.getStatusCode(), apiException.getMessage());
            throw new RegraDeNegocioException("Erro ao comunicar com provedor de pagamento.");
        } catch (MPException e) {
            log.error("Erro interno Mercado Pago", e);
            throw new RegraDeNegocioException("Erro interno ao gerar pagamento.");
        }
    }

    // ========================================================
    // 2. PROCESSAMENTO DE WEBHOOK (ATUALIZAÇÃO DE PLANO)
    // ========================================================
    @Transactional // Garante consistência no banco
    public void processarWebhook(Long idPagamento) {
        log.info("Recebendo Webhook do Mercado Pago. ID: {}", idPagamento);

        try {
            PaymentClient client = new PaymentClient();
            Payment pagamento = client.get(idPagamento);

            // Validações de status
            if ("approved".equals(pagamento.getStatus())) {
                processarAprovacao(pagamento);
            } else {
                log.warn("Pagamento {} recebido mas status é: {}", idPagamento, pagamento.getStatus());
            }

        } catch (MPException | MPApiException e) {
            // Não lançamos exception aqui para não travar a fila de retentativa do Mercado Pago
            // Apenas logamos o erro crítico
            log.error("FALHA CRÍTICA ao processar webhook ID {}: {}", idPagamento, e.getMessage());
        }
    }

    private void processarAprovacao(Payment pagamento) {
        try {
            String externalRef = pagamento.getExternalReference();

            if (externalRef == null || externalRef.isBlank()) {
                log.error("Pagamento aprovado SEM External Reference. Impossível identificar usuário.");
                return;
            }

            Long idBarbeiro = Long.parseLong(externalRef);
            Barbeiro barbeiro = barbeiroRepository.findById(idBarbeiro)
                    .orElseThrow(() -> new RegraDeNegocioException("Barbeiro ID " + idBarbeiro + " não encontrado."));

            // Idempotência: Se já for MULTI, não faz nada (evita processamento duplo)
            if (barbeiro.getPlano() == TipoPlano.MULTI) {
                log.info("Webhook ignorado: Usuário {} já possui plano MULTI.", barbeiro.getNome());
                return;
            }

            // ATUALIZAÇÃO EFETIVA
            barbeiro.setPlano(TipoPlano.MULTI);
            barbeiroRepository.save(barbeiro);

            log.info("🚀 SUCESSO! Plano do barbeiro '{}' (ID: {}) atualizado para MULTI.",
                    barbeiro.getNome(), barbeiro.getId());

        } catch (NumberFormatException e) {
            log.error("External Reference inválida: {}", pagamento.getExternalReference());
        }
    }
}