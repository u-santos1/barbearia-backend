package agendamentoDeClienteBarbearia.service;


import agendamentoDeClienteBarbearia.model.Agendamento;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotificacaoService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper; // Para criar JSON seguro

    // Formatador thread-safe
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    // CONFIGURAÇÕES (Vêm do application.properties)
    @Value("${onesignal.app.id}")
    private String oneSignalAppId;

    @Value("${onesignal.api.key}")
    private String oneSignalApiKey;

    public NotificacaoService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        // Define timeout para não travar a thread async para sempre se a API cair
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(java.time.Duration.ofSeconds(5))
                .setReadTimeout(java.time.Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Envia notificação em background (Fire-and-forget).
     * Não trava o agendamento do cliente.
     */
    @Async
    public void notificarBarbeiro(Barbeiro barbeiro, Agendamento agendamento) {
        // 1. Validação de Token (Essencial)
        if (barbeiro.getTokenPushNotification() == null || barbeiro.getTokenPushNotification().isBlank()) {
            log.debug("Notificação ignorada: Barbeiro '{}' (ID: {}) não possui token push cadastrado.",
                    barbeiro.getNome(), barbeiro.getId());
            return;
        }

        try {
            // 2. Construção da Mensagem
            String dataFormatada = agendamento.getDataHoraInicio().format(FORMATTER);
            String titulo = "✂️ Novo Agendamento!";
            String mensagem = String.format("%s agendou: %s - %s",
                    agendamento.getCliente().getNome(),
                    agendamento.getServico().getNome(),
                    dataFormatada);

            // 3. Envio Real
            enviarPushOneSignal(barbeiro.getTokenPushNotification(), titulo, mensagem);

        } catch (Exception e) {
            // Log de erro sem quebrar a aplicação
            log.error("Falha ao enviar push para {}: {}", barbeiro.getNome(), e.getMessage());
        }
    }

    private void enviarPushOneSignal(String playerIds, String titulo, String mensagem) {
        String url = "https://onesignal.com/api/v1/notifications";

        try {
            // Montagem do Payload (JSON)
            Map<String, Object> payload = new HashMap<>();
            payload.put("app_id", oneSignalAppId);
            payload.put("include_player_ids", List.of(playerIds)); // Lista de destinatários

            // Conteúdo (Suporta multilinguagem, aqui fixo PT/EN)
            payload.put("headings", Map.of("en", titulo));
            payload.put("contents", Map.of("en", mensagem));

            // Dados extras (útil para abrir o app direto no agendamento)
            payload.put("data", Map.of("tipo", "NOVO_AGENDAMENTO"));

            // Headers de Autorização
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + oneSignalApiKey);

            // Conversão para JSON String
            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            // Disparo HTTP POST
            restTemplate.postForObject(url, request, String.class);

            log.info("Push enviado com sucesso para device: {}", playerIds);

        } catch (Exception e) {
            throw new RuntimeException("Erro na integração OneSignal: " + e.getMessage());
        }
    }
}