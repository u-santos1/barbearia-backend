package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.model.Agendamento;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoService {

    // Exemplo simulado. Para produção, recomendo a lib do OneSignal ou Firebase Admin SDK
    public void notificarBarbeiro(Barbeiro barbeiro, Agendamento agendamento) {
        if (barbeiro.getTokenPushNotification() == null) {
            System.out.println("⚠️ Barbeiro " + barbeiro.getNome() + " não tem token de push.");
            return;
        }

        String titulo = "Novo Agendamento! ✂️";
        String mensagem = "Cliente " + agendamento.getCliente().getNome() +
                " agendou para " + agendamento.getDataHoraInicio();

        System.out.println("🔔 ENVIANDO PUSH PARA: " + barbeiro.getNome());
        System.out.println("MSG: " + mensagem);

        // AQUI VOCÊ COLOCA O CÓDIGO DO ONE SIGNAL / FIREBASE
        // enviarRequestParaOneSignal(barbeiro.getTokenPushNotification(), titulo, mensagem);
    }
}