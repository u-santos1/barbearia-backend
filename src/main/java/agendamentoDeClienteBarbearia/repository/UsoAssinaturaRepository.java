package agendamentoDeClienteBarbearia.repository;


import agendamentoDeClienteBarbearia.model.UsoAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UsoAssinaturaRepository extends JpaRepository<UsoAssinatura, Long> {
    List<UsoAssinatura> findByAssinaturaId(Long assinaturaId);
}