package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByEmail(String email);
    Optional<Cliente> findByEmail(String email);

    // ADICIONE ESTA LINHA:
    Optional<Cliente> findByTelefone(String telefone);
}