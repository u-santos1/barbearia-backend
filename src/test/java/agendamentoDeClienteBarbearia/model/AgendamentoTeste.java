package agendamentoDeClienteBarbearia.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;


public class AgendamentoTeste {

    @Test
    void deveRetornarFalsoQuandoHorarioFimUltrapassaExpediente() {
        Agendamento agendamento = new Agendamento();

        LocalDate dataHoje = LocalDate.of(2026, 4, 20);

        agendamento.setDataHoraInicio(dataHoje.atTime(8, 0));
        agendamento.setDataHoraFim(dataHoje.atTime(18, 0));

        LocalDateTime inicioSolicitado = LocalDateTime.of(2026, 4, 20, 17, 45);
        LocalDateTime fimSolicitado = inicioSolicitado.plusMinutes(30);

        boolean estaDisponivel = agendamento.estaDentroDoExpediente(inicioSolicitado, fimSolicitado);


        assertFalse(estaDisponivel, "Deveria ser falso pois termina 18:15");
    }
}
