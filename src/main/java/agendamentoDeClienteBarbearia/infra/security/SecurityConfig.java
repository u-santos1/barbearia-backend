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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                    // 1. INFRA E DOCS (Público)
                    req.requestMatchers("/", "/error", "/favicon.ico").permitAll();
                    req.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    req.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();

                    // 2. AUTENTICAÇÃO E WEBHOOKS (Público)
                    req.requestMatchers("/auth/**").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/pagamentos/webhook").permitAll();

                    // 3. FLUXO DO CLIENTE - AGENDAMENTO (Público)
                    // Liberamos apenas o que o cliente final precisa para marcar horário
                    req.requestMatchers(HttpMethod.POST, "/clientes", "/agendamentos").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/servicos/**", "/barbeiros/**").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/agendamentos/disponibilidade/**").permitAll();

                    // Cadastro de barbeiro (Se for público no seu modelo)
                    req.requestMatchers(HttpMethod.POST, "/barbeiros", "/barbeiros/registro").permitAll();

                    // 4. BLOQUEIO DE SEGURANÇA (AQUI ESTAVA O ERRO)
                    // Removemos as rotas de 'admin' e 'barbeiro' do permitAll e jogamos para cá
                    // Agora, qualquer rota que comece com esses prefixos EXIGE token JWT
                    req.requestMatchers("/agendamentos/admin/**").authenticated();
                    req.requestMatchers("/agendamentos/barbeiro/**").authenticated();
                    req.requestMatchers("/agendamentos/cliente/**").authenticated();
                    req.requestMatchers("/agendamentos/buscar").authenticated();

                    // 5. QUALQUER OUTRA ROTA
                    req.anyRequest().authenticated();
                })
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
        configuration.setAllowedOrigins(Arrays.asList(
                "https://barbearia-frontend-rose.vercel.app",
                "https://barbearia-frontend-9aynanzh7-u-santos1s-projects.vercel.app",
                "http://127.0.0.1:5500",
                "http://localhost:3000"
        ));

        // Métodos permitidos
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));

        // Cabeçalhos permitidos (Authorization, Content-Type, etc)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // Permite credenciais/cookies (Importante para alguns navegadores)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
