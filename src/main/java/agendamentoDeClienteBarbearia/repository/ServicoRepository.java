package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    // ✅ CORREÇÃO: Adicionamos a @Query aqui para ensinar ao Spring
    // que queremos buscar pelo ID do objeto Barbeiro
    @Query("SELECT s FROM Servico s WHERE s.barbeiro.id = :barbeiroId")
    List<Servico> findAllByBarbeiroId(@Param("barbeiroId") Long barbeiroId);

    // Busca serviços de todos os barbeiros de uma Loja específica
    @Query("SELECT s FROM Servico s WHERE s.barbeiro.loja.id = :lojaId")
    List<Servico> findAllByLojaId(@Param("lojaId") Long lojaId);

    @Query("SELECT COUNT(s) > 0 FROM Servico s WHERE LOWER(s.nome) = LOWER(:nome) AND s.barbeiro.id = :donoId")
    boolean existsByNomeIgnoreCaseAndDonoId(@Param("nome") String nome, @Param("donoId") Long donoId);


    @Query("SELECT s FROM Servico s WHERE s.barbeiro.id = :donoId AND s.ativo = true")
    List<Servico> findAllByDonoIdAndAtivoTrue(@Param("donoId") Long donoId);
}