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

    public String criarInstancia(String nome) {
        String url = getBaseUrl() + "/instance/create";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apikey", whatsappApiToken);

        Map<String, Object> body = new HashMap<>();
        body.put("instanceName", nome);
        body.put("token", whatsappApiToken);
        body.put("qrcode", true);

        ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        return response.getBody();
    }
}