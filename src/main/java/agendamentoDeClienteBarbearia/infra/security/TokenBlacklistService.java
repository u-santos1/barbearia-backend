package agendamentoDeClienteBarbearia.infra.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {
    private final Cache<String, Boolean> blacklist = Caffeine.newBuilder()
            .expireAfterAccess(2, TimeUnit.HOURS)
            .build();

    public void invalidar(String token){
        blacklist.put(token, true);
    }
    public boolean isInvalido(String token){
        return blacklist.getIfPresent(token) != null;
    }

}
