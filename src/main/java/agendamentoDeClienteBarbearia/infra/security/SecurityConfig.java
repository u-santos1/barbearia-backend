package agendamentoDeClienteBarbearia.infra.security;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private SecurityFilter securityFilter;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 1. CORS LIBERADO (Para o Front não dar erro de Preflight)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {

                    // ====================================================
                    // GRUPO 1: QUEM ALTERA O BANCO PUBLICAMENTE
                    // (Se faltar algum aqui, dá erro 403 no site)
                    // ====================================================
                    req.requestMatchers(HttpMethod.POST, "/auth/login").permitAll(); // Entrar
                    req.requestMatchers(HttpMethod.POST, "/barbeiros").permitAll();  // Criar conta do Dono
                    req.requestMatchers(HttpMethod.POST, "/clientes").permitAll();   // Salvar Cliente
                    req.requestMatchers(HttpMethod.POST, "/agendamentos").permitAll(); // Salvar Agendamento

                    // ====================================================
                    // GRUPO 2: QUEM APENAS LÊ DADOS (Público)
                    // ====================================================
                    req.requestMatchers(HttpMethod.GET, "/servicos").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/barbeiros").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/agendamentos/disponibilidade").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/agendamentos/barbeiro/**").permitAll();

                    // ====================================================
                    // GRUPO 3: TUDO O RESTO É RESTRITO (Admin/Barbeiro)
                    // (Bloqueios, Deletes, Updates, Financeiro)
                    // ====================================================
                    req.anyRequest().authenticated();
                })
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // --- CONFIGURAÇÃO DE CORS ---
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Permite qualquer origem (Frontend Vercel, Localhost, etc)
        configuration.addAllowedOriginPattern("*");

        // Libera todos os métodos HTTP (inclusive OPTIONS que causa o erro de preflight)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));

        // Libera cabeçalhos
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}