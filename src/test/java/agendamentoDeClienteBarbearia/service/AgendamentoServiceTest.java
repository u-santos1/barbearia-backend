package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;
import agendamentoDeClienteBarbearia.model.Agendamento;
import agendamentoDeClienteBarbearia.repository.AgendamentoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;
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

    @Test
    @DisplayName("Deve retornar o DTO quando o agendamento for encontrado e pertencer ao dono")
    void deveBuscarPorIdComSucesso() {
        // 1. ARRANGE (Preparação)
        Long idAgendamento = 1L;
        Long  idLogado = 1L;

        Agendamento agendamentoMock = new Agendamento();
        agendamentoMock.setId(idAgendamento);


        when(agendamentoRepository.findByIdAndBarbeiroId(idAgendamento, idLogado))
                .thenReturn(Optional.of(agendamentoMock));

        // 2. ACT (Ação)
        DetalhamentoAgendamentoDTO resultado = service.buscarPorId(idAgendamento, idLogado);

        // 3. ASSERT (Verificação)
        assertNotNull(resultado);
        assertEquals(idAgendamento, resultado.id());


        verify(agendamentoRepository).findByIdAndBarbeiroId(idAgendamento, idLogado);
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException quando o agendamento não existir ou for de outro dono")
    void deveLancarExcecaoQuandoNaoEncontrado() {
        // 1. ARRANGE
        Long idAgendamento = 99L;
        Long idLogado = 1L;


        when(agendamentoRepository.findByIdAndBarbeiroId(idAgendamento, idLogado))
                .thenReturn(Optional.empty());

        // 2 & 3. ACT & ASSERT
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            service.buscarPorId(idAgendamento, idLogado);
        });

        assertEquals("Agendamento não encontrado.", exception.getMessage());
        verify(agendamentoRepository).findByIdAndBarbeiroId(idAgendamento, idLogado);
    }
}
