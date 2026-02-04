package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);
    Optional<Cliente> findByTelefone(String telefone);
    boolean existsByEmail(String email);
    boolean existsByTelefone(String telefone);
    List<Cliente> findAllByDonoId(Long donoId);
}