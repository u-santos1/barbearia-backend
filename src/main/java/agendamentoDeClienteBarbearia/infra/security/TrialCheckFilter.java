package agendamentoDeClienteBarbearia.infra.security;

import agendamentoDeClienteBarbearia.TipoPlano;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class TrialCheckFilter extends OncePerRequestFilter {

    private static final int DIAS_TRIAL = 7;

    // Esses prefixos nunca são bloqueados, independente do trial
    private static final List<String> CAMINHOS_LIBERADOS = List.of(
            "/auth/",
            "/pagamentos/",
            "/barbeiros/me",
            "/barbeiros",
            "/health"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Só verifica usuários autenticados cujo principal é um Barbeiro
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Barbeiro barbeiro)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Libera os caminhos essenciais
        String path = request.getRequestURI();
        boolean liberado = CAMINHOS_LIBERADOS.stream().anyMatch(path::startsWith);
        if (liberado) {
            filterChain.doFilter(request, response);
            return;
        }

        // Verifica trial apenas para plano SOLO
        if (barbeiro.getPlano() == TipoPlano.SOLO && barbeiro.getCreatedAt() != null) {
            long diasPassados = ChronoUnit.DAYS.between(barbeiro.getCreatedAt(), LocalDateTime.now());

            if (diasPassados >= DIAS_TRIAL) {
                response.setStatus(402);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"erro\": \"Período de teste expirado. Assine um plano para continuar.\"}"
                );
                return; // Interrompe a cadeia — não chega no Controller
            }
        }

        filterChain.doFilter(request, response);
    }
}

