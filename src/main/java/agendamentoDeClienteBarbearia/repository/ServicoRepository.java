package agendamentoDeClienteBarbearia.repository;


import agendamentoDeClienteBarbearia.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;





public interface ServicoRepository extends JpaRepository<Servico, Long> {

    // Verifica se existe ignorando maiúsculas/minúsculas
    // Essencial para não ter "Corte" e "corte" no banco
    boolean existsByNomeIgnoreCase(String nome);

    // Traz apenas os ativos (Soft Delete)
    List<Servico> findAllByAtivoTrue();
    List<Servico> findAllByDonoId(Long donoId);

    boolean existsByNomeIgnoreCaseAndDonoId(String nome, Long donoId);

    // ✅ ADICIONE PARA CORRIGIR O "findAllByDonoIdAndAtivoTrue":
    List<Servico> findAllByDonoIdAndAtivoTrue(Long donoId);

    @Query("SELECT s FROM Servico s WHERE s.barbeiro.loja.id = :lojaId")
    List<Servico> findAllByLojaId(Long lojaId);

    List<Servico> findAllByBarbeiroId(Long barbeiroId);
}