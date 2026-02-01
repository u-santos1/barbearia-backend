package agendamentoDeClienteBarbearia.infra;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Lê a variável do application.properties
    // Se não existir, usa "*" (libera tudo) como padrão para evitar erro
    @Value("${api.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Aplica a TODAS as rotas da API
                .allowedOrigins(allowedOrigins.split(",")) // Permite múltiplos domínios (separados por vírgula)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "TRACE", "CONNECT") // Métodos permitidos
                .allowedHeaders("*") // Permite todos os headers (Authorization, Content-Type, etc)
                .allowCredentials(true); // Permite envio de Cookies/Auth (Importante para produção)
    }
}