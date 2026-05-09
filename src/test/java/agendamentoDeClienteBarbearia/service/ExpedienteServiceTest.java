package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.dtos.DadosExpedienteDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Expediente;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.ExpedienteRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpedienteServiceTest {

    @Mock
    private ExpedienteRepository repository;
    @Mock
    private BarbeiroRepository barbeiroRepository;
    @InjectMocks
    private ExpedienteService expedienteService;

    @Test
    void deveLancarExcecaoQuandoBarbeiroNaoExistir() {
        // ARRANGE (Preparar o cenário)
        Long barbeiroIdInvalido = 99L;
        // Ensinando o dublê: "Quando procurarem o ID 99, devolva vazio."
        when(barbeiroRepository.findById(barbeiroIdInvalido)).thenReturn(Optional.empty());

        // ACT & ASSERT (Agir e Verificar)
        // Verificamos se o Java realmente joga a exceção EntityNotFoundException na nossa cara
        assertThrows(EntityNotFoundException.class, () -> {
            expedienteService.configurarExpediente(barbeiroIdInvalido, List.of());
        });

        // Verificação de Segurança: Garante que o sistema NUNCA tentou salvar nada no banco
        verify(repository, never()).save(any());
    }

    @Test
    void deveSalvarExpedienteComSucessoQuandoDadosForemValidos() {
        Long barbeiroId = 1L;
        Barbeiro barbeiro = new Barbeiro();
        barbeiro.setId(barbeiroId);

        DadosExpedienteDTO dto = new DadosExpedienteDTO(
                DayOfWeek.MONDAY,
                LocalTime.of(9,0),
                LocalTime.of(18, 0),
                true
        );

        when(barbeiroRepository.findById(barbeiroId)).thenReturn(Optional.of(barbeiro))
                .thenReturn(Optional.empty());

        expedienteService.configurarExpediente(barbeiroId, List.of(dto));

        verify(repository, times(1)).save(any(Expediente.class));
    }
}
