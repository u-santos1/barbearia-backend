package agendamentoDeClienteBarbearia.infra.security;



import agendamentoDeClienteBarbearia.model.Barbeiro;
import com.auth0.jwt.JWT; // <--- IMPORTANTE
import com.auth0.jwt.algorithms.Algorithm; // <--- IMPORTANTE
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.auth0.jwt.interfaces.DecodedJWT; // <--- ESSE É O IMPORT QUE FALTA
import com.auth0.jwt.interfaces.JWTVerifier; // <--- IMPORTANTE TAMBÉM

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String gerarToken(Barbeiro usuario) {
        try {
            var algoritmo = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("API Barbearia")
                    .withSubject(usuario.getEmail())
                    .withClaim("id", usuario.getId())
                    .withExpiresAt(dataExpiracao())
                    .sign(algoritmo);
        } catch (JWTCreationException exception){
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public String getSubject(String tokenJWT) {
        try {
            var algoritmo = Algorithm.HMAC256(secret);

            // 1. Cria o verificador
            JWTVerifier verificador = JWT.require(algoritmo)
                    .withIssuer("API Barbearia")
                    .build();

            // 2. Verifica o token e guarda o resultado na variável tipada
            DecodedJWT tokenDecodificado = verificador.verify(tokenJWT);

            // 3. Agora o Java sabe que 'tokenDecodificado' tem esse método
            return tokenDecodificado.getSubject();

        } catch (JWTVerificationException exception){
            throw new RuntimeException("Token JWT inválido ou expirado!");
        }
    }

    private Instant dataExpiracao() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
