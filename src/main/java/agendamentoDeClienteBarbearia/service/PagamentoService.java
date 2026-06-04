package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.TipoPlano;
import agendamentoDeClienteBarbearia.dtos.RespostaPixDTO;
import agendamentoDeClienteBarbearia.dtos.UpgradeRequestDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.HistoricoPagamento;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.HistoricoPagamentoRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
public class PagamentoService {

    private final HistoricoPagamentoRepository pagamentoRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final String mpAccessToken;
    private final String webhookUrl;
    private final BigDecimal valorPlano;

    public PagamentoService(BarbeiroRepository barbeiroRepository,
                            @Value("${mercadopago.access_token}") String token,
                            @Value("${mercadopago.webhook.url}") String webhookUrl,
                            @Value("${plano.multi.valor:1.00}") BigDecimal valorPlano,
                            HistoricoPagamentoRepository pagamentoRepository) {
        this.barbeiroRepository = barbeiroRepository;
        this.mpAccessToken = token;
        this.webhookUrl = webhookUrl;
        this.valorPlano = valorPlano;
        this.pagamentoRepository = pagamentoRepository;
    }

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(mpAccessToken);
        log.info("SDK Mercado Pago inicializado.");
    }

    private boolean verificarSePagamentoJaFoiProcessado(Long idPagamento){
        return pagamentoRepository.existsByPagamentoIdMp(idPagamento);
    }

    // ========================================================
    // 1. GERA O PIX (COM BLINDAGEM ANTI-ERRO 500)
    // ========================================================
    public RespostaPixDTO gerarPixUpgrade(Long idBarbeiro, UpgradeRequestDTO dados) {
        Barbeiro barbeiro = barbeiroRepository.findById(idBarbeiro)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado no sistema."));

        // 🛡️ 1. PROTEÇÃO ANTI-NULL: Evita o crash se o CPF vier vazio do Frontend
        String cpfLimpo = (dados.cpf() != null) ? dados.cpf().replaceAll("\\D", "") : "";
        if (cpfLimpo.length() != 11) {
            throw new RegraDeNegocioException("CPF inválido. Certifique-se de preencher 11 dígitos.");
        }

        // 🛡️ 2. PROTEÇÃO DE NOME: O Mercado Pago recusa se o nome for vazio
        String nomePagador = (dados.nome() != null && !dados.nome().isBlank()) ? dados.nome() : barbeiro.getNome();

        // 🛡️ 3. REGRA DE NEGÓCIO: Impede gerar PIX se a conta já está paga e válida
        if (barbeiro.getPlano() == TipoPlano.MULTI && barbeiro.getDataExpiracaoSaas() != null && barbeiro.getDataExpiracaoSaas().isAfter(LocalDate.now())) {
            throw new RegraDeNegocioException("A sua barbearia já possui uma assinatura MULTI ativa e válida.");
        }

        try {
            PaymentClient client = new PaymentClient();

            PaymentPayerRequest payer = PaymentPayerRequest.builder()
                    .email(barbeiro.getEmail())
                    .firstName(nomePagador)
                    .identification(IdentificationRequest.builder()
                            .type("CPF")
                            .number(cpfLimpo)
                            .build())
                    .build();

            PaymentCreateRequest createRequest = PaymentCreateRequest.builder()
                    .transactionAmount(valorPlano)
                    .description("Assinatura Kliper MULTI - 30 Dias")
                    .paymentMethodId("pix")
                    .notificationUrl(webhookUrl)
                    .payer(payer)
                    .externalReference(barbeiro.getId().toString())
                    .build();

            Payment payment = client.create(createRequest);

            if (payment.getPointOfInteraction() == null) {
                log.error("Mercado Pago recusou o PIX. Status: {} - Detalhe: {}", payment.getStatus(), payment.getStatusDetail());
                throw new RegraDeNegocioException("O Mercado Pago recusou a geração do pagamento. Tente novamente.");
            }

            String qrCode = payment.getPointOfInteraction().getTransactionData().getQrCode();
            String base64 = payment.getPointOfInteraction().getTransactionData().getQrCodeBase64();
            String ticketUrl = payment.getPointOfInteraction().getTransactionData().getTicketUrl();

            return new RespostaPixDTO(payment.getId(), qrCode, base64, ticketUrl, payment.getStatus());

        } catch (MPApiException e) {
            log.error("Erro API MP: {}", e.getApiResponse().getContent());
            throw new RegraDeNegocioException("Erro no Mercado Pago: " + e.getApiResponse().getContent());
        } catch (MPException e) {
            log.error("Erro interno MP", e);
            throw new RegraDeNegocioException("Erro de comunicação com o Mercado Pago.");
        }
    }

    // ========================================================
    // 2. WEBHOOK (AGORA ATUALIZA OS 30 DIAS DO SAAS)
    // ========================================================
    @Transactional
    public void processarWebhook(Long idPagamento) {
        log.info("Processando Webhook para Pagamento ID: {}", idPagamento);
        try {
            PaymentClient client = new PaymentClient();
            Payment pagamento = client.get(idPagamento);

            if ("approved".equals(pagamento.getStatus())){
                if(verificarSePagamentoJaFoiProcessado(idPagamento)){
                    log.info("Pagamento ID: {} já processado anteriormente. Ignorando.", idPagamento);
                    return;
                }
                atualizarPlanoUsuario(pagamento);
            }
        } catch (MPException | MPApiException e) {
            log.error("Erro ao consultar pagamento no webhook: {}", idPagamento);
            throw new RuntimeException("Falha na validação do pagamento", e);
        }
    }

    private void atualizarPlanoUsuario(Payment pagamento) {
        String externalRef = pagamento.getExternalReference();
        if (externalRef == null) return;

        try {
            Long idBarbeiro = Long.parseLong(externalRef);
            barbeiroRepository.findById(idBarbeiro).ifPresent(barbeiro -> {

                // 1. Seta o plano para MULTI
                if (barbeiro.getPlano() != TipoPlano.MULTI) {
                    barbeiro.setPlano(TipoPlano.MULTI);
                }

                // 2. 🚀 MÁGICA DO SAAS: Adiciona 30 dias de acesso ao sistema
                LocalDate dataBase = barbeiro.getDataExpiracaoSaas();
                if (dataBase == null || dataBase.isBefore(LocalDate.now())) {
                    barbeiro.setDataExpiracaoSaas(LocalDate.now().plusDays(30)); // Estava vencido, conta de hoje
                } else {
                    barbeiro.setDataExpiracaoSaas(dataBase.plusDays(30)); // Pagou antes de vencer, acumula os dias!
                }

                barbeiroRepository.save(barbeiro);
                log.info("✅ Plano MULTI ativado! O SaaS de {} foi renovado até: {}", barbeiro.getNome(), barbeiro.getDataExpiracaoSaas());

                // 3. Salva o Recibo na Base de Dados
                HistoricoPagamento historico = new HistoricoPagamento();
                historico.setPagamentoIdMp(pagamento.getId());
                historico.setStatus(pagamento.getStatus());
                historico.setDataProcessamento(LocalDateTime.now());
                pagamentoRepository.save(historico);
            });
        } catch (Exception e) {
            log.error("Erro Crítico ao atualizar o plano: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void cancelarAssinatura(String emailBarbeiro) {
        Barbeiro barbeiro = barbeiroRepository.findByEmail(emailBarbeiro)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado."));

        // Se ele já for SOLO, não faz nada
        if (barbeiro.getPlano() == TipoPlano.SOLO) {
            throw new RegraDeNegocioException("Você já está no plano gratuito (SOLO).");
        }

        // Faz o downgrade imediato
        barbeiro.setPlano(TipoPlano.SOLO);

        // Opcional: Você pode zerar a data de expiração ou deixá-la rolar até o fim.
        // Como ele pediu para cancelar, vamos zerar para cortar o acesso MULTI na hora.
        barbeiro.setDataExpiracaoSaas(LocalDate.now().minusDays(1));

        barbeiroRepository.save(barbeiro);
        log.info("Assinatura cancelada (Downgrade para SOLO) pelo usuário: {}", emailBarbeiro);
    }
}