package agendamentoDeClienteBarbearia.infra.security;

import agendamentoDeClienteBarbearia.PerfilAcesso;

import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final BarbeiroRepository repository;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        var tokenJWT = recuperarToken(request);

        // NOVO: Precisamos saber qual rota o usuário está a tentar aceder
        String path = request.getRequestURI();

        if (tokenJWT != null){
            if (tokenBlacklistService.isInvalido(tokenJWT)){
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        if (tokenJWT != null) {
            try {
                // 1. Tenta validar o token. Se falhar, vai pro CATCH.
                var login = tokenService.getSubject(tokenJWT);

                // 2. Verifica se o login não é nulo
                if (login != null) {
                    var usuario = repository.findByEmail(login).orElse(null);

                    // 3. Verifica se achou o usuário no banco
                    if (usuario != null) {

                        // ==================================================
                        // 🚨 TRAVA DO SAAS (PERÍODO DE TESTE OU MENSALIDADE)
                        // ==================================================
                        if (usuario.getPerfil() == PerfilAcesso.ADMIN && usuario.isAcessoBloqueado()) {

                            // Liberta APENAS as rotas vitais para ele conseguir pagar o PIX
                            if (!path.contains("/pagamento") && !path.contains("/planos") && !path.contains("/perfil") && !path.contains("/auth")) {
                                response.setStatus(402); // 402 = Payment Required
                                response.setContentType("application/json; charset=UTF-8");
                                response.getWriter().write("{\"erro\": \"A sua assinatura do Kliper expirou. Efetue o pagamento para continuar a usar o sistema.\"}");
                                return; // ⛔ BLOQUEIA A REQUISIÇÃO AQUI E NÃO AVANÇA
                            }
                        }
                        // ==================================================

                        var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (Exception e) {
                // 🚨 AGORA O JAVA AVISA SE ALGO CRASHAR DE FORMA INVISÍVEL
                System.out.println("🚨 ERRO NO FILTRO DE SEGURANÇA: " + e.getMessage());
                e.printStackTrace();
                SecurityContextHolder.clearContext();
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