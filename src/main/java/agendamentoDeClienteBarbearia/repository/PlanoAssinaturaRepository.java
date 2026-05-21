package agendamentoDeClienteBarbearia.repository;


import agendamentoDeClienteBarbearia.model.PlanoAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlanoAssinaturaRepository extends JpaRepository<PlanoAssinatura, Long> {
    List<PlanoAssinatura> findByDonoIdAndAtivoTrue(Long donoId);
}