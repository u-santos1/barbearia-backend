package agendamentoDeClienteBarbearia.dtosResponse;

import java.time.LocalDateTime;

public interface HistoricoLembreteProjection {
    LocalDateTime getDataEnvio();
    String getClienteNome();
    String getTelefoneDestino();
    String getStatus();
    String getRegraNome();
}