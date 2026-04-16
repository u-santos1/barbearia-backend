package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.dtosResponse.RelatorioBarbeiroDTO;
import agendamentoDeClienteBarbearia.service.BarbeiroService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    BarbeiroRepository repository;

    @InjectMocks
    BarbeiroService service;

    @Test
    void deveRetornarRelatorioMensal() {
        // 1. Monta o dado falso
        var dto = new RelatorioBarbeiroDTO(
                1L, "João", 10L, 2L,
                new BigDecimal("500.00"),
                new BigDecimal("250.00")
        );

        // 2. Define o comportamento do mock
        when(repository.relatorioMensal(1L, 4, 2025))
                .thenReturn(List.of(dto));

        // 3. Chama o service
        var resultado = service.relatorioMensal(1L, 4, 2025);

        // 4. Verifica
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nomeBarbeiro()).isEqualTo("João");
        assertThat(resultado.get(0).totalDeAgendamentos()).isEqualTo(10L);

        // 5. Verifica se o repository foi chamado
        verify(repository).relatorioMensal(1L, 4, 2025);
    }

    @Test
    void deveRetornarListaVaziaQuandoSemAgendamentos() {
        when(repository.relatorioMensal(1L, 4, 2025))
                .thenReturn(Collections.emptyList());

        var resultado = service.relatorioMensal(1L, 4, 2025);

        assertThat(resultado).isEmpty();
    }
}
