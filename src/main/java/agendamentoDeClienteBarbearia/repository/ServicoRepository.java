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

    // Busca serviços pelo ID do Barbeiro
    @Query("SELECT s FROM Servico s WHERE s.barbeiro.id = :barbeiroId")
    List<Servico> findAllByBarbeiroId(@Param("barbeiroId") Long barbeiroId);

    // Busca serviços pelo ID da Loja (através do barbeiro)
    @Query("SELECT s FROM Servico s WHERE s.barbeiro.loja.id = :lojaId")
    List<Servico> findAllByLojaId(@Param("lojaId") Long lojaId);


    // ========================================================================
    // 2. MÉTODOS PARA O SERVICO SERVICE (COMPATIBILIDADE COM "DONO")
    // ========================================================================

    // Tradução: O Service pede "Dono", nós buscamos no campo "Barbeiro"
    // Verifica se já existe um serviço com esse nome para este barbeiro
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Servico s WHERE LOWER(s.nome) = LOWER(:nome) AND s.barbeiro.id = :donoId")
    boolean existsByNomeIgnoreCaseAndDonoId(@Param("nome") String nome, @Param("donoId") Long donoId);

    // Tradução: Busca serviços ativos de um "Dono" (que na verdade é Barbeiro)
    @Query("SELECT s FROM Servico s WHERE s.barbeiro.id = :donoId AND s.ativo = true")
    List<Servico> findAllByDonoIdAndAtivoTrue(@Param("donoId") Long donoId);
}