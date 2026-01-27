package agendamentoDeClienteBarbearia.service;



import agendamentoDeClienteBarbearia.dtos.CadastroClienteDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoClienteDTO;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Habilita o Mockito
class ClienteServiceTest {

    @InjectMocks // Cria o Service de verdade e injeta os mocks nele
    private ClienteService service;

    @Mock // Cria um Repository "Falso" (Simulado)
    private ClienteRepository repository;

    @Test
    @DisplayName("CENÁRIO 1: Cliente NOVO (Não existe no banco) -> Deve Criar")
    void deveCriarClienteNovo() {
        // ARRANGE
        // CORREÇÃO AQUI: Verifique a ordem no seu DTO.
        // Vou assumir que é (Nome, Telefone, Email) baseado no erro.
        CadastroClienteDTO dadosInput = new CadastroClienteDTO(
                "Wesley",
                "21999999999",      // Telefone primeiro?
                "wesley@email.com"  // Email depois?
        );

        // Se o seu DTO for (Nome, Email, Telefone), me avise, pois aí o erro seria outro.

        // Para evitar erros de "Strict Stubbing", use o 'anyString()' nos mocks de busca
        // Isso diz: "Se buscar qualquer email, retorne vazio"
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(repository.findByTelefone(anyString())).thenReturn(Optional.empty());

        when(repository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        // ACT
        DetalhamentoClienteDTO resultado = service.cadastrarOuAtualizar(dadosInput);

        // ASSERT
        assertNotNull(resultado);
        assertEquals("Wesley", resultado.nome());
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("CENÁRIO 2: Cliente JÁ EXISTE (Pelo Telefone) -> Deve Atualizar")
    void deveAtualizarClienteExistentePeloTelefone() {
        // 1. ARRANGE
        Cliente clienteNoBanco = new Cliente();
        clienteNoBanco.setId(1L);
        clienteNoBanco.setNome("Wesley Antigo");
        clienteNoBanco.setTelefone("21999999999");

        // Email é NULL aqui
        CadastroClienteDTO dadosNovos = new CadastroClienteDTO("Wesley Novo", "21999999999", null);

        // --- CORREÇÃO ---
        // REMOVIDO: when(repository.findByEmail(any())).thenReturn(Optional.empty());
        // Motivo: Como o email é null, o Service nem tenta buscar, então não precisamos 'mockar' isso.

        // Mantemos apenas a busca pelo telefone, que é o que vai acontecer de verdade
        when(repository.findByTelefone("21999999999")).thenReturn(Optional.of(clienteNoBanco));

        // 2. ACT
        DetalhamentoClienteDTO resultado = service.cadastrarOuAtualizar(dadosNovos);

        // 3. ASSERT
        assertEquals("Wesley Novo", resultado.nome());
        assertEquals(1L, resultado.id());
    }
}