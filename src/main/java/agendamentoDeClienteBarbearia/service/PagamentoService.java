package agendamentoDeClienteBarbearia.service;





import agendamentoDeClienteBarbearia.TipoPlano;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.resources.payment.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PagamentoService {

    private final BarbeiroRepository barbeiroRepository;

    public PagamentoService(BarbeiroRepository barbeiroRepository,
                            @Value("${mercadopago.access_token}") String token) {
        this.barbeiroRepository = barbeiroRepository;
        MercadoPagoConfig.setAccessToken(token); // Configura o SDK
    }

    // 1. GERA O PIX
    public Payment gerarPixUpgrade(Long idBarbeiro) {
        Barbeiro barbeiro = barbeiroRepository.findById(idBarbeiro)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        try {
            PaymentClient client = new PaymentClient();

            PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                    .transactionAmount(new BigDecimal("89.90")) // Valor do Plano
                    .description("Upgrade Plano MULTI - Barbearia")
                    .paymentMethodId("pix")
                    .payer(PaymentPayerRequest.builder()
                            .email(barbeiro.getEmail())
                            .firstName(barbeiro.getNome())
                            .build())
                    // O segredo: salvamos o ID do barbeiro na referência externa
                    // Assim, quando o pagamento cair, sabemos QUEM pagar.
                    .externalReference(barbeiro.getId().toString())
                    .build();

            return client.create(createRequest);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar PIX: " + e.getMessage());
        }
    }

    // 2. RECEBE O AVISO DO MERCADO PAGO E ATUALIZA O PLANO
    public void processarWebhook(Long idPagamento) {
        try {
            PaymentClient client = new PaymentClient();
            Payment pagamento = client.get(idPagamento);

            // Verifica se está aprovado
            if ("approved".equals(pagamento.getStatus())) {
                // Pega o ID do barbeiro que salvamos lá na criação
                Long idBarbeiro = Long.parseLong(pagamento.getExternalReference());

                // Busca e Atualiza
                Barbeiro barbeiro = barbeiroRepository.findById(idBarbeiro).orElseThrow();

                if (barbeiro.getPlano() == TipoPlano.SOLO) {
                    barbeiro.setPlano(TipoPlano.MULTI);
                    barbeiroRepository.save(barbeiro);
                    System.out.println("✅ PLANO ATUALIZADO PARA MULTI: " + barbeiro.getNome());
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar pagamento: " + e.getMessage());
        }
    }
}