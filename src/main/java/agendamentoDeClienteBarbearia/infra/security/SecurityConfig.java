package agendamentoDeClienteBarbearia.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
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
    private SecurityFilter securityFilter; // <--- Injetamos o filtro que cria a segurança via Token

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Nada de guardar sessão no servidor
                .authorizeHttpRequests(req -> req
                        // 1. ROTAS DE AUTENTICAÇÃO (Sempre Abertas)
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll() // <--- LOGIN
                        .requestMatchers(HttpMethod.POST, "/barbeiros").permitAll()  // <--- CADASTRO DO DONO

                        // 2. ROTAS PÚBLICAS (Cliente acessa sem login)
                        .requestMatchers(HttpMethod.GET, "/barbeiros/**").permitAll() // Listar barbeiros
                        .requestMatchers(HttpMethod.GET, "/servicos/**").permitAll()  // Listar serviços
                        .requestMatchers(HttpMethod.POST, "/agendamentos").permitAll() // Criar agendamento
                        .requestMatchers("/agendamentos/barbeiro/**").permitAll() // Ver horários ocupados

                        // Cliente precisa ver/cancelar (se for via link público, mantém liberado)
                        .requestMatchers("/agendamentos/cliente/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/agendamentos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/clientes/recuperar-id").permitAll()
                        .requestMatchers(HttpMethod.POST, "/clientes").permitAll()

                        // 3. ROTAS PROTEGIDAS (Só Dono Logado com Token JWT acessa)
                        // Qualquer outra rota não listada acima exige Token
                        .anyRequest().authenticated()
                )
                // Adiciona o filtro JWT antes do filtro padrão do Spring
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // --- CONFIGURAÇÃO NECESSÁRIA PARA O LOGIN FUNCIONAR ---
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // --- CRIPTOGRAFIA DE SENHA (PADRÃO DE MERCADO) ---
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // --- CONFIGURAÇÃO DE CORS (Mantive a sua, está ótima para dev) ---
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}