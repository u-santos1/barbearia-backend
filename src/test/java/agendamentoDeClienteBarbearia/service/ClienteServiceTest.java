package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoClienteDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Cliente;
import agendamentoDeClienteBarbearia.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @InjectMocks
    private ClienteService service;

    @Mock
    private ClienteRepository repository;

    @Mock
    private Barbeiro donoMock;

    @Test
    @DisplayName("CENÁRIO 2: Cliente JÁ EXISTE (Pelo Telefone) -> Deve Atualizar")
    void deveAtualizarClienteExistentePeloTelefone() {
        // 1. ARRANGE
        Cliente clienteNoBanco = new Cliente();
        clienteNoBanco.setId(10L);
        clienteNoBanco.setTelefone("21999999999");
        clienteNoBanco.setDono(donoMock);

        // 🚩 CORREÇÃO CRÍTICA: Verifique a ordem no seu CadastroClienteDTO.java
        // Geralmente: (Nome, Email, Telefone, BarbeiroId)
        CadastroClienteDTO dadosNovos = new CadastroClienteDTO(
                "João Atualizado",
                "joao@email.com",  // Email
                "21999999999",    // Telefone (Agora no lugar certo!)
                null
        );

        // Mocks sincronizados com o Repository SaaS
        when(repository.findByTelefoneAndDono("21999999999", donoMock)).thenReturn(Optional.of(clienteNoBanco));
        when(repository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        // 2. ACT
        // Agora o telefone chegará preenchido e passará pela validação da linha 36
        DetalhamentoClienteDTO resultado = service.salvar(dadosNovos, donoMock);

        // 3. ASSERT
        assertEquals("João Atualizado", resultado.nome());
        verify(repository).save(any());
    }
}