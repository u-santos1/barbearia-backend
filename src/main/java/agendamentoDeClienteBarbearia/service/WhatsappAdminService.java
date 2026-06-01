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
    // 4. DISPARAR MENSAGEM DE TESTE IMEDIATO
    public String enviarMensagemTeste(String nomeInstancia, String telefone) {
        String url = getBaseUrl() + "/message/sendText/" + nomeInstancia;

        // Limpa o número e garante o código 55 (Brasil)
        String numeroLimpo = telefone.replaceAll("\\D", "");
        if (!numeroLimpo.startsWith("55")) numeroLimpo = "55" + numeroLimpo;

        Map<String, String> payload = new HashMap<>();
        payload.put("number", numeroLimpo);
        payload.put("text", "🚀 *Teste do Barber Pro!*\nSe você recebeu esta mensagem, o seu motor de WhatsApp SaaS está 100% online e pronto para enviar lembretes aos clientes!");

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, getHeaders());

        try {
            restTemplate.postForObject(url, entity, String.class);
            return "{\"status\": \"Mensagem enviada com sucesso!\"}";
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.err.println("❌ ERRO DA EVOLUTION API (Envio): " + e.getResponseBodyAsString());
            throw e;
        }
    }
}