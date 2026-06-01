package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.model.Agendamento;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.LogLembrete;
import agendamentoDeClienteBarbearia.model.RegraLembrete;
import agendamentoDeClienteBarbearia.repository.AgendamentoRepository;
import agendamentoDeClienteBarbearia.repository.LogLembreteRepository;
import agendamentoDeClienteBarbearia.repository.RegraLembreteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class NotificacaoService {

    private final RestTemplate restTemplate;
    private final AgendamentoRepository agendamentoRepository;
    private final RegraLembreteRepository regraLembreteRepository;
    private final LogLembreteRepository logLembreteRepository; // Nova dependência

    private static final String ONESIGNAL_URL = "https://onesignal.com/api/v1/notifications";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");
    private static final ZoneId TIMEZONE_BRASIL = ZoneId.of("America/Sao_Paulo");

    @Value("${onesignal.app.id}")
    private String oneSignalAppId;

    @Value("${onesignal.api.key}")
    private String oneSignalApiKey;

    @Value("${whatsapp.api.url}")
    private String whatsappApiUrl;

    @Value("${whatsapp.api.token}")
    private String whatsappApiToken;

    public NotificacaoService(RestTemplateBuilder restTemplateBuilder,
                              AgendamentoRepository agendamentoRepository,
                              RegraLembreteRepository regraLembreteRepository,
                              LogLembreteRepository logLembreteRepository) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.agendamentoRepository = agendamentoRepository;
        this.regraLembreteRepository = regraLembreteRepository;
        this.logLembreteRepository = logLembreteRepository;
    }

    @Async
    public void notificarBarbeiro(Barbeiro barbeiro, Agendamento agendamento) {
        if (barbeiro.getTokenPushNotification() == null || barbeiro.getTokenPushNotification().isBlank()) {
            return;
        }

        try {
            String dataFormatada = agendamento.getDataHoraInicio().format(FORMATTER);
            String titulo = "✂️ Novo Corte Agendado!";
            String mensagem = String.format("%s - %s\nCliente: %s",
                    agendamento.getServico().getNome(),
                    dataFormatada,
                    agendamento.getCliente().getNome());

            Map<String, Object> payload = new HashMap<>();
            payload.put("app_id", oneSignalAppId);
            payload.put("include_player_ids", List.of(barbeiro.getTokenPushNotification()));
            payload.put("headings", Map.of("en", titulo));
            payload.put("contents", Map.of("en", mensagem));
            payload.put("data", Map.of("tipo", "NOVO_AGENDAMENTO", "agendamentoId", agendamento.getId()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + oneSignalApiKey);

            restTemplate.postForObject(ONESIGNAL_URL, new HttpEntity<>(payload, headers), String.class);
            log.info("Push enviado para Barbeiro: {}", barbeiro.getNome());

        } catch (Exception e) {
            log.error("Erro ao enviar notificação OneSignal: {}", e.getMessage());
        }
    }
    @Scheduled(fixedRate = 60000)
    public void rotinaLembretesWhatsApp() {
        LocalDateTime agora = LocalDateTime.now(TIMEZONE_BRASIL);
        LocalTime horaAtual = agora.toLocalTime();

        List<RegraLembrete> regras = regraLembreteRepository.findByAtivoTrue();

        for (RegraLembrete regra : regras) {
            LocalDateTime inicioBusca = null;
            LocalDateTime fimBusca = null;

            if (regra.getTempo() != null) {
                String tempoStr = regra.getTempo().toLowerCase();

                if (tempoStr.contains("2 horas")) {
                    inicioBusca = agora.plusHours(2);
                    fimBusca = inicioBusca.plusMinutes(15);
                }
                else if (tempoStr.contains("1 dia") || tempoStr.contains("personalizado")) {
                    if (regra.getHora() != null && !horaAtual.isBefore(regra.getHora()) && horaAtual.isBefore(regra.getHora().plusMinutes(16))) {
                        inicioBusca = agora.plusDays(1).with(LocalTime.MIN);
                        fimBusca = agora.plusDays(1).with(LocalTime.MAX);
                    }
                }
                else if (tempoStr.contains("mesmo dia")) {
                    if (regra.getHora() != null && !horaAtual.isBefore(regra.getHora()) && horaAtual.isBefore(regra.getHora().plusMinutes(16))) {
                        inicioBusca = agora.with(LocalTime.MIN);
                        fimBusca = agora.with(LocalTime.MAX);
                    }
                }
            }

            if (inicioBusca != null && fimBusca != null) {
                List<Agendamento> agendamentos = agendamentoRepository
                        .buscarAgendamentosParaLembreteDinamico(inicioBusca, fimBusca, regra.getDono().getId());

                for (Agendamento agendamento : agendamentos) {
                    // ANTI-SPAM: Só envia se não existir log na tabela
                    if (!logLembreteRepository.existsByAgendamentoIdAndRegraId(agendamento.getId(), regra.getId())) {
                        enviarLembreteWhatsApp(agendamento, regra);
                    }
                }
            }
        }
    }

    @Async
    public void enviarLembreteWhatsApp(Agendamento agendamento, RegraLembrete regra) {
        if (agendamento.getCliente().getTelefone() == null || agendamento.getCliente().getTelefone().isBlank()) return;

        try {
            String numeroLimpo = agendamento.getCliente().getTelefone().replaceAll("\\D", "");

            // PREVENÇÃO: Evitar duplicar o "55" se o cliente já se cadastrou com o código do Brasil
            if (!numeroLimpo.startsWith("55")) {
                numeroLimpo = "55" + numeroLimpo;
            }

            // ATENÇÃO: Passamos a 'regra' também para ele saber o nome da Barbearia
            String mensagemFinal = montarMensagemPersonalizada(regra.getMsg(), agendamento, regra);

            Map<String, String> payload = new HashMap<>();
            payload.put("number", numeroLimpo);
            payload.put("text", mensagemFinal);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", whatsappApiToken);

            // CORREÇÃO DE HIERARQUIA SAAS:
            // Em vez de pegar do agendamento.getBarbeiro() (que pode ser um funcionário sem zap),
            // Nós pegamos da regra.getDono(), garantindo que vai usar a instância do Dono da Barbearia!
            String emailDono = regra.getDono().getEmail();
            String nomeInstancia = "zap-" + emailDono.replaceAll("[^a-zA-Z0-9]", "");

            String baseUrl = whatsappApiUrl.contains("/message/sendText")
                    ? whatsappApiUrl.split("/message/sendText")[0]
                    : whatsappApiUrl;

            String urlDinamica = baseUrl + "/message/sendText/" + nomeInstancia;

            // O GRANDE BUG CORRIGIDO: Agora usamos a urlDinamica na requisição!
            restTemplate.postForObject(urlDinamica, new HttpEntity<>(payload, headers), String.class);

            // REGISTRA O SUCESSO NO BANCO
            logLembreteRepository.save(new LogLembrete(agendamento.getId(), regra.getId()));

            // Adicionei o nomeInstancia no log para você ter a prova de que usou a conta certa
            log.info("Lembrete WhatsApp (Regra: '{}') enviado p/ {} através da instância [{}]",
                    regra.getNome(), agendamento.getCliente().getNome(), nomeInstancia);

        } catch (Exception e) {
            log.error("Erro ao enviar WhatsApp para {}: {}", agendamento.getCliente().getNome(), e.getMessage());
        }
    }

    // Corrigido para receber a 'regra' e extrair os dados perfeitamente
    private String montarMensagemPersonalizada(String template, Agendamento agendamento, RegraLembrete regra) {
        String cliente = (agendamento.getCliente() != null) ? agendamento.getCliente().getNome() : "Cliente";

        // Separação inteligente: Barbearia vs Profissional
        String barbearia = "Barbearia";
        if (regra.getDono() != null && regra.getDono().getBarbeariaNome() != null && !regra.getDono().getBarbeariaNome().isBlank()) {
            barbearia = regra.getDono().getBarbeariaNome();
        }

        String profissional = (agendamento.getBarbeiro() != null) ? agendamento.getBarbeiro().getNome() : "nosso profissional";
        String servico = (agendamento.getServico() != null) ? agendamento.getServico().getNome() : "Serviço";

        String data = agendamento.getDataHoraInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String hora = agendamento.getDataHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm"));

        return template
                .replace("{cliente}", cliente)
                .replace("{barbearia}", barbearia)
                .replace("{serviço}", servico)
                .replace("{data}", data)
                .replace("{horário}", hora)
                .replace("{profissional}", profissional); // Agora fica perfeito!
    }

}