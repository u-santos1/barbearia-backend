package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.model.Agendamento;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.RegraLembrete;
import agendamentoDeClienteBarbearia.repository.AgendamentoRepository;
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

    // Constantes para evitar Strings mágicas espalhadas
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
                              RegraLembreteRepository regraLembreteRepository) {
        // Timeout é CRUCIAL em chamadas externas.
        // Se o OneSignal cair, sua aplicação desiste em 5 segundos e não trava.
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.agendamentoRepository = agendamentoRepository;
        this.regraLembreteRepository = regraLembreteRepository;
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

    @Scheduled(fixedRate = 10000)
    public void rotinaLembretesWhatsApp() {
        log.info("Iniciando varredura para envio de lembretes via WhatsApp (Motor Dinâmico)...");
        LocalDateTime agora = LocalDateTime.now(TIMEZONE_BRASIL);
        LocalTime horaAtual = agora.toLocalTime();

        // 1. Busca todas as regras ATIVAS no banco de dados
        List<RegraLembrete> regras = regraLembreteRepository.findByAtivoTrue();

        for (RegraLembrete regra : regras) {
            LocalDateTime inicioBusca = null;
            LocalDateTime fimBusca = null;

            // 2. Interpreta a regra baseada nas opções do seu frontend
            if (regra.getTempo() != null) {
                String tempoStr = regra.getTempo().toLowerCase();

                if (tempoStr.contains("2 horas")) {
                    // Busca agendamentos que acontecem daqui a 2 horas exatas (+15 min de margem)
                    inicioBusca = agora.plusHours(2);
                    fimBusca = inicioBusca.plusMinutes(15);
                }
                else if (tempoStr.contains("1 dia") || tempoStr.contains("personalizado")) {
                    // Dispara num horário fixo (ex: 18:00) verificando agendamentos do dia de amanhã
                    if (regra.getHora() != null && !horaAtual.isBefore(regra.getHora()) && horaAtual.isBefore(regra.getHora().plusMinutes(16))) {
                        inicioBusca = agora.plusDays(1).with(LocalTime.MIN);
                        fimBusca = agora.plusDays(1).with(LocalTime.MAX);
                    }
                }
                else if (tempoStr.contains("mesmo dia")) {
                    // Dispara num horário fixo (ex: 09:00) verificando agendamentos do dia de hoje
                    if (regra.getHora() != null && !horaAtual.isBefore(regra.getHora()) && horaAtual.isBefore(regra.getHora().plusMinutes(16))) {
                        inicioBusca = agora.with(LocalTime.MIN);
                        fimBusca = agora.with(LocalTime.MAX);
                    }
                }
            }

            // 3. Se identificou que é a hora de disparar algo, busca os agendamentos correspondentes
            if (inicioBusca != null && fimBusca != null) {
                // Filtra pelo Dono específico da regra
                List<Agendamento> agendamentos = agendamentoRepository
                        .buscarAgendamentosParaLembreteDinamico(inicioBusca, fimBusca, regra.getDono().getId());

                for (Agendamento agendamento : agendamentos) {
                    enviarLembreteWhatsApp(agendamento, regra);
                }
            }
        }
    }

    @Async
    public void enviarLembreteWhatsApp(Agendamento agendamento, RegraLembrete regra) {
        String telefoneCliente = agendamento.getCliente().getTelefone();
        if (telefoneCliente == null || telefoneCliente.isBlank()) {
            log.warn("Lembrete ignorado: Cliente '{}' sem telefone.", agendamento.getCliente().getNome());
            return;
        }

        try {
            String numeroLimpo = telefoneCliente.replaceAll("\\D", "");

            // Substitui as variáveis {cliente}, {horário}, etc. pelo texto real
            String mensagemFinal = montarMensagemPersonalizada(regra.getMsg(), agendamento);

            Map<String, String> payload = new HashMap<>();
            payload.put("number", "55" + numeroLimpo);
            payload.put("text", mensagemFinal);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + whatsappApiToken);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
            restTemplate.postForObject(whatsappApiUrl, request, String.class);

            log.info("Lembrete WhatsApp (Regra: '{}') enviado para o cliente: {}", regra.getNome(), agendamento.getCliente().getNome());

        } catch (Exception e) {
            log.error("Erro ao enviar lembrete de WhatsApp para {}: {}", agendamento.getCliente().getNome(), e.getMessage());
        }
    }

    /**
     * Helper para substituir as tags dinâmicas do frontend no corpo da mensagem.
     */
    private String montarMensagemPersonalizada(String template, Agendamento agendamento) {
        String dataFormatada = agendamento.getDataHoraInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String horaFormatada = agendamento.getDataHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm"));

        return template
                .replace("{cliente}", agendamento.getCliente().getNome())
                .replace("{barbearia}", agendamento.getBarbeiro().getNome()) // Ajuste se tiver o nome do salão/dono
                .replace("{serviço}", agendamento.getServico().getNome())
                .replace("{data}", dataFormatada)
                .replace("{horário}", horaFormatada)
                .replace("{profissional}", agendamento.getBarbeiro().getNome());
    }
}