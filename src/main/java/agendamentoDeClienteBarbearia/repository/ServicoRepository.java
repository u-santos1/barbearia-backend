package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    // ========================================================================
    // 1. MÉTODOS PARA O FRONTEND (HOME E AGENDAMENTO)
    // ========================================================================

    // ✅ CORREÇÃO: Mudamos 's.barbeiro.id' para 's.dono.id' porque é assim que está na Entidade.
    @Query("SELECT s FROM Servico s WHERE s.dono.id = :barbeiroId")
    List<Servico> findAllByBarbeiroId(@Param("barbeiroId") Long barbeiroId);

    // ✅ CORREÇÃO: Mudamos 's.barbeiro.loja' para 's.dono.loja'.
    @Query("SELECT s FROM Servico s WHERE s.dono.loja.id = :lojaId")
    List<Servico> findAllByLojaId(@Param("lojaId") Long lojaId);


    // ========================================================================
    // 2. MÉTODOS PARA O SERVICO SERVICE
    // ========================================================================

    // Verifica duplicidade usando o campo 'dono'
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Servico s WHERE LOWER(s.nome) = LOWER(:nome) AND s.dono.id = :donoId")
    boolean existsByNomeIgnoreCaseAndDonoId(@Param("nome") String nome, @Param("donoId") Long donoId);

    // Busca serviços ativos usando o campo 'dono'
    @Query("SELECT s FROM Servico s WHERE s.dono.id = :donoId AND s.ativo = true")
    List<Servico> findAllByDonoIdAndAtivoTrue(@Param("donoId") Long donoId);
}