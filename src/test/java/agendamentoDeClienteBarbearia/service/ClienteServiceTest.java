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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("CENÁRIO 1: Cliente NOVO (Não existe no banco) -> Deve Criar")
    void deveCriarClienteNovo() {
        // ARRANGE
        CadastroClienteDTO dadosInput = new CadastroClienteDTO("João", "joao@email.com", "21999999999", null);

        // Definindo o comportamento dos mocks (Stubbing)
        when(repository.findByEmailAndDono(anyString(), any(Barbeiro.class))).thenReturn(Optional.empty());
        when(repository.findByTelefoneAndDono(anyString(), any(Barbeiro.class))).thenReturn(Optional.empty());

        when(repository.save(any(Cliente.class))).thenAnswer(i -> {
            Cliente c = i.getArgument(0);
            c.setId(1L);
            return c;
        });

        // ACT
        DetalhamentoClienteDTO resultado = service.salvar(dadosInput, donoMock);

        // ASSERT
        assertNotNull(resultado);
        assertEquals("João", resultado.nome());
        verify(repository, times(1)).save(any(Cliente.class));
    }

    @Test
    @DisplayName("CENÁRIO 2: Cliente JÁ EXISTE (Pelo Telefone) -> Deve Atualizar")
    void deveAtualizarClienteExistentePeloTelefone() {
        // 1. ARRANGE
        Cliente clienteNoBanco = new Cliente();
        clienteNoBanco.setId(10L);
        clienteNoBanco.setNome("João Antigo");
        clienteNoBanco.setTelefone("21999999999");
        clienteNoBanco.setDono(donoMock);

        CadastroClienteDTO dadosNovos = new CadastroClienteDTO("João Atualizado", "joao@email.com", "21999999999", null);

        // 👇 CORREÇÃO AQUI: Usando when().thenReturn() em vez de declarar o método
        when(repository.findByEmailAndDono(anyString(), eq(donoMock))).thenReturn(Optional.empty());
        when(repository.findByTelefoneAndDono(eq("21999999999"), eq(donoMock))).thenReturn(Optional.of(clienteNoBanco));

        when(repository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        // 2. ACT
        DetalhamentoClienteDTO resultado = service.salvar(dadosNovos, donoMock);

        // 3. ASSERT
        assertEquals("João Atualizado", resultado.nome());
        assertEquals(10L, resultado.id());
        verify(repository, times(1)).save(any(Cliente.class));
    }
}