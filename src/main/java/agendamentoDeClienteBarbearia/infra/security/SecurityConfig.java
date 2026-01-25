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
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                    // 1. O que é PÚBLICO (Login, Cadastro, Listar Barbeiros/Serviços, Agendar)
                    req.requestMatchers(HttpMethod.POST, "/auth/login").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/barbeiros").permitAll(); // Cadastro de Dono
                    req.requestMatchers(HttpMethod.GET, "/barbeiros").permitAll();  // Para a Home
                    req.requestMatchers(HttpMethod.GET, "/servicos").permitAll();   // Para a Home
                    req.requestMatchers(HttpMethod.POST, "/clientes").permitAll();  // Cadastro rápido no agendamento
                    req.requestMatchers(HttpMethod.POST, "/agendamentos").permitAll(); // O cliente agendando

                    // NOVO: Disponibilidade é pública (o front consulta antes de logar)
                    req.requestMatchers(HttpMethod.GET, "/agendamentos/disponibilidade").permitAll();
                    req.requestMatchers(HttpMethod.GET, "/agendamentos/barbeiro/**").permitAll(); // Horários ocupados

                    // 2. O que é RESTRITO (Admin/Barbeiro Logado)
                    req.requestMatchers("/bloqueios/**").authenticated(); // Só logado bloqueia agenda
                    req.requestMatchers("/clientes/**").authenticated();  // Só logado vê lista de clientes
                    req.requestMatchers("/agendamentos/meus").authenticated();
                    req.requestMatchers("/agendamentos/admin/**").authenticated(); // Financeiro
                    req.requestMatchers(HttpMethod.PUT, "/agendamentos/**").authenticated(); // Concluir/Confirmar
                    req.requestMatchers(HttpMethod.DELETE, "/agendamentos/**").authenticated(); // Cancelar

                    // 3. Qualquer outra coisa
                    req.anyRequest().authenticated();
                })
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