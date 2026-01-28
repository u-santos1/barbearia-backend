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
                
                // 2. SESSÃO STATELESS (Obrigatório para JWT)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 3. AUTORIZAÇÃO DE ROTAS
                .authorizeHttpRequests(req -> {
                    // --- CORREÇÃO DE PREFLIGHT (CRÍTICO PARA VERCEL) ---
                    // Garante que o navegador possa "perguntar" se a API está online antes de enviar dados
                    req.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // --- ESCRITA PÚBLICA (CADASTROS/LOGIN) ---
                    req.requestMatchers(HttpMethod.POST, "/auth/login").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/barbeiros").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/clientes").permitAll();   
                    req.requestMatchers(HttpMethod.POST, "/agendamentos").permitAll(); 

                    // --- LEITURA PÚBLICA (CARREGAR TELA) ---
                    req.requestMatchers(HttpMethod.GET, "/servicos").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/barbeiros").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/agendamentos/disponibilidade").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/agendamentos/barbeiro/**").permitAll();

                    // --- ÁREA RESTRITA (ADMIN/PAINEL) ---
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
