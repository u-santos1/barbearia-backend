package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    // ========================================================================
    // 1. MÉTODOS PARA O FRONTEND
    // ========================================================================

    // Busca serviços de um barbeiro específico (ex: João da Barbearia do Pedro)
    // Aqui usamos s.dono.id se o "barbeiroId" for o dono, mas se for funcionário...
    // Na verdade, no seu sistema atual, s.dono aponta para o CHEFE.
    // Vamos manter essa query simples focada no Dono por enquanto para garantir estabilidade.
    @Query("SELECT s FROM Servico s WHERE s.dono.id = :barbeiroId")
    List<Servico> findAllByBarbeiroId(@Param("barbeiroId") Long barbeiroId);

    // ✅ CORREÇÃO FINAL: O 'lojaId' na verdade é o ID do DONO.
    // Então a query é idêntica à de cima, mas semânticamente para "Loja".
    @Query("SELECT s FROM Servico s WHERE s.dono.id = :lojaId")
    List<Servico> findAllByLojaId(@Param("lojaId") Long lojaId);

    // ========================================================================
    // 2. MÉTODOS DE SERVIÇO INTERNO
    // ========================================================================

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Servico s WHERE LOWER(s.nome) = LOWER(:nome) AND s.dono.id = :donoId")
    boolean existsByNomeIgnoreCaseAndDonoId(@Param("nome") String nome, @Param("donoId") Long donoId);

    @Query("SELECT s FROM Servico s WHERE s.dono.id = :donoId AND s.ativo = true")
    List<Servico> findAllByDonoIdAndAtivoTrue(@Param("donoId") Long donoId);
}