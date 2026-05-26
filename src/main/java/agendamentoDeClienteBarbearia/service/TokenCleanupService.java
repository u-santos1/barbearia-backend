package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    @Scheduled(cron = "0 0 2 * * *")
    public void limparTokenExpeiados(){
        log.info("[CRON JOB] Iniciando limpeza de tokens expirados no banco de dados...");
        passwordResetTokenRepository.deletarTokenExpirados(LocalDateTime.now());
        log.info("[CRON JOB] Limpeza concluída com sucesso.");
    }
}
