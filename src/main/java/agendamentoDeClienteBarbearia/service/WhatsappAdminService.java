package agendamentoDeClienteBarbearia.service;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsappAdminService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${whatsapp.api.url}")
    private String whatsappApiUrl;

    @Value("${whatsapp.api.token}")
    private String whatsappApiToken;

    private String getBaseUrl() {
        if (whatsappApiUrl.contains("/message/sendText")) {
            return whatsappApiUrl.split("/message/sendText")[0];
        }
        return whatsappApiUrl;
    }
    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", whatsappApiToken);
        return headers;
    }


    public String obterStatus(String nome) {
        String url = getBaseUrl() + "/instance/connectionState/" + nome;
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", whatsappApiToken);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return response.getBody();
    }

    public String lerQrCode(String nome) {
        String url = getBaseUrl() + "/instance/connect/" + nome;
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", whatsappApiToken);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return response.getBody();
    }

    public String criarInstancia(String nomeInstancia) {
        String url = getBaseUrl() + "/instance/create";

        Map<String, Object> payload = new HashMap<>();
        payload.put("instanceName", nomeInstancia);
        payload.put("qrcode", true);
        payload.put("token", nomeInstancia);
        payload.put("integration", "WHATSAPP-BAILEYS");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, getHeaders());

        try {
            // Tenta criar do zero
            return restTemplate.postForObject(url, entity, String.class);
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String respostaErro = e.getResponseBodyAsString();

            // BLINDAGEM SAAS: Se o erro disser que já existe, nós ignoramos o erro e buscamos o QR Code!
            if (respostaErro.contains("already in use")) {
                System.out.println("⚠️ Instância [" + nomeInstancia + "] já existe! Buscando o QR Code existente...");
                return lerQrCode(nomeInstancia); // Reaproveita o método que já temos nesta mesma classe!
            }

            // Se for outro erro diferente, aí sim nós imprimimos a vermelho e paramos
            System.err.println("❌ ERRO DA EVOLUTION API (Create): " + respostaErro);
            throw e;
        }
    }
}