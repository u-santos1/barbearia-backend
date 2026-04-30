package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.HistoricoPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoricoPagamentoRepository extends JpaRepository<HistoricoPagamento, Long> {

    boolean existsByPagamentoIdMp(Long pagamentoIdMp);
}
