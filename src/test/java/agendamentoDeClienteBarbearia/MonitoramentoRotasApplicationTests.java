package agendamentoDeClienteBarbearia;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class MonitoramentoRotasApplicationTests {

	@Test
	void contextLoads() {
	}
	@Test
	void gerarHashDeSenha() {
		PasswordEncoder encoder = new BCryptPasswordEncoder();
		String hash = encoder.encode("minhaSenha");
		System.out.println("Hash gerado: " + hash);
	}
}
