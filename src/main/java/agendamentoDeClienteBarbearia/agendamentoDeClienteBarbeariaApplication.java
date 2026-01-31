package agendamentoDeClienteBarbearia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class agendamentoDeClienteBarbeariaApplication {

	public static void main(String[] args) {
		SpringApplication.run(agendamentoDeClienteBarbeariaApplication.class, args);
	}

}
