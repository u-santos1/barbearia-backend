package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.TipoPlano;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BarbeiroServiceTest {

    @Mock
    private BarbeiroRepository barbeiroRepository;

    @InjectMocks
    private BarbeiroService barbeiroService;

    @Test
    void deveLancarExcecaoQuandoPlanoTentarCadastrarQuartoFuncionario(){

        Barbeiro dono = new Barbeiro();
        dono.setId(1L);
        dono.setPlano(TipoPlano.SOLO);

        dono.setCreatedAt(LocalDateTime.now().minusDays(5));

        when(barbeiroRepository.countByDonoIdAndAtivoTrue(1L)).thenReturn(3L);

        RegraDeNegocioException erroCapturado = assertThrows(RegraDeNegocioException.class, () -> {
            barbeiroService.validarLimitesDoPlano(dono);
        });
        assertEquals("Limite atingido. O plano SOLO permite até 3 funcionários. Faça upgrade para MULTI.", erroCapturado.getMessage());

    }
}
