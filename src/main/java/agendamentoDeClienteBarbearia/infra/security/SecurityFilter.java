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
                // 1. Tenta validar o token. Se falhar, vai pro CATCH.
                var login = tokenService.getSubject(tokenJWT);

                // 2. Verifica se o login não é nulo
                if (login != null) {
                    var usuario = repository.findByEmail(login).orElse(null);

                    // 3. Verifica se achou o usuário no banco
                    if (usuario != null) {
                        var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                // AQUI ESTÁ A CORREÇÃO DO ERRO 500!
                // Em vez de quebrar o servidor, nós apenas limpamos a autenticação.
                // O Spring Security vai retornar 403 (Proibido) pacificamente depois.
                SecurityContextHolder.clearContext();
                // System.out.println("Token inválido ignorado: " + e.getMessage()); // Descomente para debug se quiser
            }
        }

        filterChain.doFilter(request, response);
    }

    // Método auxiliar seguro contra strings vazias
    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null) {
            if (!authorizationHeader.isBlank() && authorizationHeader.toLowerCase().startsWith("bearer ")) {
                var token = authorizationHeader.substring(7).trim();
                return token.isEmpty() ? null : token;
            }
        }
        return null;
    }
}
