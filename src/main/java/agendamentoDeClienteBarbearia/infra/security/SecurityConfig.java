package agendamentoDeClienteBarbearia.infra.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private SecurityFilter securityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 1. CORS & CSRF
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                // 2. SESSÃO STATELESS
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. AUTORIZAÇÃO DE ROTAS
                .authorizeHttpRequests(req -> {
                    // --- HEALTHCHECK & INFRA ---
                    req.requestMatchers("/", "/error").permitAll(); // Adicionei /error aqui também pra evitar 403 em falhas
                    req.requestMatchers("/actuator/**").permitAll();

                    // --- PREFLIGHT (CORS) ---
                    req.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // --- DOCUMENTAÇÃO ---
                    req.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();

                    // --- LOGIN ---
                    req.requestMatchers("/auth/**").permitAll();

                    // --- CADASTROS (ESCRITA PÚBLICA) ---
                    // CORRIGIDO AQUI:
                    req.requestMatchers(HttpMethod.GET, "/servicos/barbeiro/**").permitAll(); // ✅ LIBERA SERVIÇOS
                    req.requestMatchers(HttpMethod.POST, "/barbeiros").permitAll();
                    // Mantemos esse por segurança caso tenha algum link antigo
                    req.requestMatchers(HttpMethod.POST, "/barbeiros/registro").permitAll();

                    req.requestMatchers(HttpMethod.POST, "/clientes").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/agendamentos").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/pagamentos/webhook").permitAll();

                    // --- LEITURA PÚBLICA ---
                    req.requestMatchers(HttpMethod.GET, "/servicos").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/barbeiros/**").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/agendamentos/disponibilidade").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/agendamentos/barbeiro/**").permitAll();

                    // --- TUDO O RESTO EXIGE LOGIN ---
                    req.anyRequest().authenticated();
                })

                // 4. FILTRO DE TOKEN JWT
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // --- BEANS DE AUTENTICAÇÃO ---
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // --- CONFIGURAÇÃO DE CORS (GLOBAL) ---
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // IMPORTANTE: 'addAllowedOriginPattern("*")' é melhor que 'setAllowedOrigins("*")'
        // pois permite credenciais (allowCredentials=true) funcionar com wildcard.
        // Isso libera Localhost, Vercel Production e Vercel Preview.
        configuration.addAllowedOriginPattern("*");

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
