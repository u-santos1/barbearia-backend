package agendamentoDeClienteBarbearia;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
public class agendamentoDeClienteBarbeariaApplication {

	public static void main(String[] args) {
		SpringApplication.run(agendamentoDeClienteBarbeariaApplication.class, args);
	}

	// ✅ CORREÇÃO: O método init fica FORA do main, mas DENTRO da classe
	@PostConstruct
	public void init() {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
		System.out.println("Fuso horário definido para: " + TimeZone.getDefault().getID());
	}
}