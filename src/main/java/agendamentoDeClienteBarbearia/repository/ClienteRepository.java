package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // --- BUSCA ISOLADA POR DONO (Multi-tenancy) ---
    // Essencial para o método service.salvar() e para os testes
    Optional<Cliente> findByEmailAndDono(String email, Barbeiro dono);

    Optional<Cliente> findByTelefoneAndDono(String telefone, Barbeiro dono);

    // --- LISTAGEM ---
    List<Cliente> findAllByDonoId(Long donoId);

    // --- VERIFICAÇÕES DE EXISTÊNCIA ---
    boolean existsByEmailAndDono(String email, Barbeiro dono);

    boolean existsByTelefoneAndDono(String telefone, Barbeiro dono);

    // --- BUSCAS GLOBAIS (Opcionais) ---
    // Mantenha apenas se você tiver algum fluxo administrativo global
    Optional<Cliente> findByEmail(String email);
    Optional<Cliente> findByTelefone(String telefone);
}