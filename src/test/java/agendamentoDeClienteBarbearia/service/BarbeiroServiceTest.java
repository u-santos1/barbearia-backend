package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.TipoPlano;
import agendamentoDeClienteBarbearia.dtos.CadastroBarbeiroDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.ExpedienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BarbeiroServiceTest {

    @Mock
    private BarbeiroRepository barbeiroRepository;

    @Mock
    private ExpedienteRepository expedienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private BarbeiroService barbeiroService;

    @Test
    @DisplayName("Deve lançar exceção quando usuário SOLO tenta cadastrar mais de 1 barbeiro após período de trial")
    void deveLancarExcecaoQuandoPlanoSoloTentarCadastrarMaisDeUmFuncionario() {
        // ARRANGE (Cenário)
        Barbeiro dono = new Barbeiro();
        dono.setId(1L);
        dono.setPlano(TipoPlano.SOLO);

        // Simula que o trial de 7 dias já acabou (20 dias atrás)
        dono.setCreatedAt(LocalDateTime.now().minusDays(20));

        // Simula que ele já tem 1 barbeiro cadastrado (o dono conta como 1)
        when(barbeiroRepository.countByDonoIdAndAtivoTrue(1L)).thenReturn(1L);

        // ACT & ASSERT
        RegraDeNegocioException erroCapturado = assertThrows(RegraDeNegocioException.class, () -> {
            // Chamando o método de validação que criamos no Service
            barbeiroService.validarLimiteDeBarbeiros(dono);
        });

        // Verificação da mensagem (ajuste conforme a mensagem real do seu método)
        assertEquals("O plano SOLO permite apenas 1 barbeiro. Faça o upgrade para o plano MULTI.", erroCapturado.getMessage());
    }

    @Test
    void deveCadastrarBarbeiroEMockarExpediente(){
        var dto = new CadastroBarbeiroDTO("Wesley", "wesley@email.com", "123456", "Corte", "21993434258", true, 100.0, new java.math.BigDecimal("0"));

        // Criamos um objeto de barbeiro "de mentira" para o mock retornar
        Barbeiro barbeiroFake = new Barbeiro();
        barbeiroFake.setId(1L);
        barbeiroFake.setNome("Wesley");
        barbeiroFake.setEmail("wesley@email.com");
        // ... preencha outros campos se o seu DTO exigir

        // ✅ AQUI ESTÁ O SEGREDO:
        // "Quando o repository.save for chamado com QUALQUER barbeiro, retorne o barbeiroFake"
        when(barbeiroRepository.save(any(Barbeiro.class))).thenReturn(barbeiroFake);

        // Se o seu service usa o encoder, mantenha o mock dele também:
        when(passwordEncoder.encode(any())).thenReturn("senha_criptografada");

        // 2. WHEN (Ação)
        var resultado = barbeiroService.cadastrarDono(dto);

        // 3. THEN (Verificação)
        assertNotNull(resultado);
        assertEquals("Wesley", resultado.nome());
        verify(expedienteRepository, times(1)).saveAll(anyList());
    }

}
