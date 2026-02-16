package agendamentoDeClienteBarbearia.infra.security;


import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter { // <--- Vem do starter-web

    private final TokenService tokenService;
    private final BarbeiroRepository repository;
    public SecurityFilter(TokenService tokenService, BarbeiroRepository repository){
        this.tokenService = tokenService;
        this.repository = repository;
    }



    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        var tokenJWT = recuperarToken(request);

        if (tokenJWT != null) {
            try {
                // Tenta validar. Se o token for inválido, o getSubject estoura erro e cai no catch.
                var login = tokenService.getSubject(tokenJWT);

                // Verifica se o login veio preenchido
                if (login != null) {
                    var usuario = repository.findByEmail(login).orElse(null);

                    // Só autentica se achou o usuário
                    if (usuario != null) {
                        var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                // Token inválido, expirado ou usuário não achado.
                // Apenas ignoramos e limpamos o contexto. O Spring vai barrar lá na frente com 403.
                SecurityContextHolder.clearContext();


            }
        }

        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null) {
            // .trim() remove espaços antes e depois de tudo
            // .isBlank() verifica se sobrou alguma coisa
            if (!authorizationHeader.isBlank() && authorizationHeader.toLowerCase().startsWith("bearer ")) {

                // Pega o token e remove espaços extras
                var token = authorizationHeader.substring(7).trim();

                // Se o token for vazio (só tinha "Bearer "), retorna null
                return token.isEmpty() ? null : token;
            }
        }

        return null;
    }
}
