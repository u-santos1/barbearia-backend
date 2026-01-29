package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.model.Agendamento;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import org.springframework.stereotype.Service;



import agendamentoDeClienteBarbearia.model.Agendamento;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class NotificacaoService {

    // 1. Logger profissional (Slf4j)
    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    // Formatador estático (Thread-safe e performático)
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    /**
     * @Async faz esse método rodar em uma thread separada.
     * O servidor responde "OK" para o cliente IMEDIATAMENTE,
     * enquanto o envio do push acontece em segundo plano.
     */
    @Async
    public void notificarBarbeiro(Barbeiro barbeiro, Agendamento agendamento) {

        // 2. Validação Rápida
        if (barbeiro.getTokenPushNotification() == null || barbeiro.getTokenPushNotification().isBlank()) {
            log.warn("⚠️ Notificação ignorada: Barbeiro '{}' não possui token de push.", barbeiro.getNome());
            return;
        }

        try {
            // 3. Formatação Humanizada
            String dataFormatada = agendamento.getDataHoraInicio().format(FORMATTER);

            String titulo = "Novo Agendamento! ✂️";
            String mensagem = String.format("Cliente %s agendou para %s",
                    agendamento.getCliente().getNome(),
                    dataFormatada);

            log.info("🔔 Iniciando envio de Push para: {}", barbeiro.getNome());

            // 4. Chamada Real (Simulada aqui, mas preparada para HTTP)
            enviarRequestOneSignal(barbeiro.getTokenPushNotification(), titulo, mensagem);

            log.info("✅ Push enviado com sucesso para {}", barbeiro.getNome());

        } catch (Exception e) {
            // Como é Async, se der erro aqui, NINGUÉM fica sabendo se não tiver log.
            log.error("❌ Erro ao enviar notificação para {}: {}", barbeiro.getNome(), e.getMessage());
        }
    }

    // Método privado para isolar a integração com API Externa
    private void enviarRequestOneSignal(String token, String titulo, String mensagem) {
        // AQUI entraria o RestTemplate ou WebClient
        // Exemplo de log estruturado que facilitaria o debug:
        log.debug("Payload OneSignal: { target: {}, title: {}, body: {} }", token, titulo, mensagem);

        // Simulação de delay de rede (para provar que o @Async é necessário)
        try { Thread.sleep(100); } catch (InterruptedException e) {}
    }
}