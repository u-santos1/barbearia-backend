package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;
import agendamentoDeClienteBarbearia.model.Agendamento;
import agendamentoDeClienteBarbearia.repository.AgendamentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AgendamentoServiceTest {
    @Mock
    private AgendamentoRepository agendamentoRepository;

    @InjectMocks
    private AgendamentoService service;

    @Test
    @DisplayName("Deve retornar lista de DTOs quando o dono possui agendamentos")
    void listarTodosDoDonoCenario1() {

        Long donoId = 1L;
        Agendamento agendamentoSimulado = new Agendamento();
        agendamentoSimulado.setId(10L);

        when(agendamentoRepository.findAllByBarbeiroDonoId(donoId))
                .thenReturn(List.of(agendamentoSimulado));

        List<DetalhamentoAgendamentoDTO> resultado = service.listarTodosDoDono(donoId);


        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(10L, resultado.get(0).id());
        verify(agendamentoRepository, times(1)).findAllByBarbeiroDonoId(donoId);
    }
}
