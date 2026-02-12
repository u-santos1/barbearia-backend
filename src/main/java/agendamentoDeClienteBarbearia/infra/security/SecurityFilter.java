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

        // 1. Tenta extrair o token do cabeçalho Authorization
        var tokenJWT = recuperarToken(request);

        // 2. Só entra na lógica de autenticação se o token existir
        if (tokenJWT != null) {
            try {
                // Valida o token e extrai o login (subject)
                var login = tokenService.getSubject(tokenJWT);

                // Busca o usuário no banco de dados
                var usuario = repository.findByEmail(login).orElse(null);

                if (usuario != null) {
                    // Se o usuário existe, cria o objeto de autenticação e coloca no contexto do Spring
                    var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // LOG de debug (Opcional): ajuda a identificar tokens malformados ou expirados sem travar a API
                System.err.println("Erro na validação do JWT: " + e.getMessage());

                // IMPORTANTE: Limpamos o contexto para garantir que nenhuma autenticação residual permaneça
                SecurityContextHolder.clearContext();
            }
        }

        // 3. CONTINUIDADE: Este comando DEVE ser executado sempre,
        // com ou sem token, para que a requisição siga para o próximo filtro.
        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");

        // Verifica se o cabeçalho existe E se começa com "Bearer " antes de cortar a string
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7).trim();
        }

        return null;
    }
}
