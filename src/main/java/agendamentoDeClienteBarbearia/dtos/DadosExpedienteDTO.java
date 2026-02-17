package agendamentoDeClienteBarbearia.dtos;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record DadosExpedienteDTO(
        DayOfWeek diaSemana,
        LocalTime abertura,
        LocalTime fechamento,
        boolean trabalha
) {}