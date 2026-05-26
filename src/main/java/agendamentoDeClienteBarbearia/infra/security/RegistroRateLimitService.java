package agendamentoDeClienteBarbearia.infra.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Interner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RegistroRateLimitService {

    private static final int MAX_TENTATIVAS = 5;

    public final Cache<String, Integer> tentativasCache = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(5000)
            .build();

    public void registrarFalha(String email){
        Integer tentativas = tentativasCache.getIfPresent(email);
        if (tentativas == null){
            tentativas = 0;
        }
        tentativas ++;
        tentativasCache.put(email, tentativas);
        log.warn("Login falho registrado para: {} (Tentativa {}/{}", email, tentativas, MAX_TENTATIVAS);
    }
    public void limparTentativas(String email){
        tentativasCache.invalidate(email);
    }
    public boolean isBloqueado(String email){
        Integer tentativas = tentativasCache.getIfPresent(email);
        return tentativas != null && tentativas >= MAX_TENTATIVAS;
    }
}
