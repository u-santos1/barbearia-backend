package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoAgendamentoDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Agendamento;
import agendamentoDeClienteBarbearia.model.Barbeiro;
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
    @DisplayName("Deve retornar o DTO quando o agendamento for encontrado e pertencer à mesma barbearia (Dono)")
    void deveBuscarPorIdComSucesso() {
        // 1. ARRANGE (Preparação)
        Long idAgendamento = 1L;
        Long idDono = 100L;

        // Criamos o Dono que está logado no sistema
        Barbeiro donoLogado = new Barbeiro();
        donoLogado.setId(idDono);
        donoLogado.setDono(null); // Como ele é o dono supremo, ele não tem chefe

        // Criamos um funcionário que pertence a esse dono
        Barbeiro funcionario = new Barbeiro();
        funcionario.setId(200L);
        funcionario.setDono(donoLogado); // Vinculamos o funcionário ao dono

        // Preparamos o Agendamento feito pelo funcionário
        Agendamento agendamentoMock = new Agendamento();
        agendamentoMock.setId(idAgendamento);
        agendamentoMock.setBarbeiro(funcionario);

        // O repositório agora faz uma busca global simples
        when(agendamentoRepository.findById(idAgendamento))
                .thenReturn(Optional.of(agendamentoMock));

        // 2. ACT (Ação)
        // Passamos o objeto 'donoLogado' inteiro, como o novo Service exige
        DetalhamentoAgendamentoDTO resultado = service.buscarPorId(idAgendamento, donoLogado);

        // 3. ASSERT (Verificação)
        assertNotNull(resultado);
        assertEquals(idAgendamento, resultado.id()); // O novo DTO usa records (resultado.id() em vez de getId())

        // Garantimos que o repositório foi chamado da nova forma
        verify(agendamentoRepository).findById(idAgendamento);
    }

    @Test
    @DisplayName("Deve lançar RegraDeNegocioException quando o agendamento não existir no banco de dados")
    void deveLancarExcecaoQuandoAgendamentoNaoEncontrado() {
        // 1. ARRANGE
        Long idAgendamento = 99L;
        Barbeiro donoLogado = new Barbeiro();
        donoLogado.setId(1L);

        // Mockamos exatamente o método que o código real chama: findById
        when(agendamentoRepository.findById(idAgendamento))
                .thenReturn(Optional.empty());

        // 2 & 3. ACT & ASSERT
        // Trocamos para a exceção correta que o seu código lança
        RegraDeNegocioException exception = assertThrows(RegraDeNegocioException.class, () -> {
            service.buscarPorId(idAgendamento, donoLogado);
        });

        assertEquals("Agendamento não encontrado.", exception.getMessage());

        // Verificamos exatamente o método que o código real chama
        verify(agendamentoRepository).findById(idAgendamento);
    }
}
