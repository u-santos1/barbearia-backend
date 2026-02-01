package agendamentoDeClienteBarbearia.controller;



import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    // Rota raiz para a Railway bater e ver que está tudo bem
    @GetMapping("/")
    public String healthCheck() {
        return "API da Barbearia está ONLINE! 🚀";
    }
}