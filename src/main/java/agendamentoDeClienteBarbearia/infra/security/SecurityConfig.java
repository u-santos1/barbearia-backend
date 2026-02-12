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

                // 2. SESSÃO STATELESS (REST API)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. AUTORIZAÇÃO DE ROTAS
                .authorizeHttpRequests(req -> {
                    // --- HEALTHCHECK & INFRA (Railway/Actuator) ---
                    req.requestMatchers("/", "/error", "/favicon.ico").permitAll();
                    req.requestMatchers("/actuator/**").permitAll();

                    // --- PREFLIGHT (CORS) ---
                    // Libera requisições OPTIONS (necessário para o navegador verificar permissões)
                    req.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // --- DOCUMENTAÇÃO (Swagger) ---


                    // --- LOGIN (Barbeiro/Admin) ---


                    req.requestMatchers("/auth/**").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/login").permitAll();      // <--- O ERRO 403 ERA AQUI
                    req.requestMatchers(HttpMethod.POST, "/auth/login").permitAll(); // <--- E AQUI

                    req.requestMatchers("/", "/error", "/favicon.ico").permitAll();
                    req.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();

                    req.requestMatchers(HttpMethod.GET, "/agendamentos/cliente").permitAll();
                    req.requestMatchers(HttpMethod.DELETE, "/agendamentos/cliente/**").permitAll();

                    // --- LEITURA PÚBLICA (Cliente acessa sem login) ---
                    // Adicionei "/**" no final para garantir que sub-rotas e query params passem
                    req.requestMatchers(HttpMethod.GET, "/servicos/**").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/barbeiros/**").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/agendamentos/disponibilidade/**").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/agendamentos/barbeiro/**").permitAll();

                    // --- ESCRITA PÚBLICA (Cliente agenda/cadastra) ---
                    req.requestMatchers(HttpMethod.POST, "/clientes").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/agendamentos").permitAll();

                    // Webhook de pagamento (Se tiver)
                    req.requestMatchers(HttpMethod.POST, "/pagamentos/webhook").permitAll();

                    // Cadastro de Barbeiro (Geralmente público para novos cadastros)
                    req.requestMatchers(HttpMethod.POST, "/barbeiros").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/barbeiros/registro").permitAll();

                    // --- TUDO O RESTO EXIGE TOKEN JWT ---
                    req.anyRequest().authenticated();
                })

                // 4. FILTRO DE TOKEN
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
    // Isso é vital para o Frontend (Vercel/Localhost) conversar com o Backend
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Permite qualquer origem (Frontend)
        configuration.addAllowedOriginPattern("*");

        // Métodos permitidos
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));

        // Cabeçalhos permitidos (Authorization, Content-Type, etc)
        configuration.setAllowedHeaders(List.of("*"));

        // Permite credenciais/cookies (Importante para alguns navegadores)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
