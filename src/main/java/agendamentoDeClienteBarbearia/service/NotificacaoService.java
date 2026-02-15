package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.model.Agendamento;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotificacaoService {

    private final RestTemplate restTemplate;

    // Constantes para evitar Strings mágicas espalhadas
    private static final String ONESIGNAL_URL = "https://onesignal.com/api/v1/notifications";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    @Value("${onesignal.app.id}")
    private String oneSignalAppId;

    @Value("${onesignal.api.key}")
    private String oneSignalApiKey;

    public NotificacaoService(RestTemplateBuilder restTemplateBuilder) {
        // Timeout é CRUCIAL em chamadas externas.
        // Se o OneSignal cair, sua aplicação desiste em 5 segundos e não trava.
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Envia notificação em background (Fire-and-forget).
     * O @Async faz isso rodar numa thread separada.
     */
    @Async
    public void notificarBarbeiro(Barbeiro barbeiro, Agendamento agendamento) {
        // 1. Fail-fast: Se não tem token, nem tenta.
        if (barbeiro.getTokenPushNotification() == null || barbeiro.getTokenPushNotification().isBlank()) {
            log.warn("Push cancelado: Barbeiro '{}' não possui token cadastrado.", barbeiro.getNome());
            return;
        }

        try {
            // 2. Prepara os dados
            String dataFormatada = agendamento.getDataHoraInicio().format(FORMATTER);
            String titulo = "✂️ Novo Corte Agendado!";
            String mensagem = String.format("%s - %s\nCliente: %s",
                    agendamento.getServico().getNome(),
                    dataFormatada,
                    agendamento.getCliente().getNome());

            // 3. Monta o Payload do OneSignal
            Map<String, Object> payload = new HashMap<>();
            payload.put("app_id", oneSignalAppId);
            payload.put("include_player_ids", List.of(barbeiro.getTokenPushNotification())); // Array de IDs

            // Conteúdo (Inglês é obrigatório como fallback no OneSignal, mas usamos o texto em PT)
            payload.put("headings", Map.of("en", titulo));
            payload.put("contents", Map.of("en", mensagem));

            // Dados ocultos para o App abrir na tela certa
            payload.put("data", Map.of(
                    "tipo", "NOVO_AGENDAMENTO",
                    "agendamentoId", agendamento.getId()
            ));

            // 4. Headers (Autenticação)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + oneSignalApiKey);

            // 5. Envio
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            // O RestTemplate converte o Map para JSON automaticamente, não precisa do ObjectMapper manual
            restTemplate.postForObject(ONESIGNAL_URL, request, String.class);

            log.info("Push enviado para Barbeiro: {}", barbeiro.getNome());

        } catch (Exception e) {
            // Log de erro robusto (não deixa o erro subir e atrapalhar o fluxo principal)
            log.error("Erro ao enviar notificação OneSignal: {}", e.getMessage(), e);
        }
    }
}