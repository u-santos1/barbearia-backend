package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {
    boolean existsByNomeIgnoreCase(String nome);
    List<Servico> findAllByAtivoTrue();
}