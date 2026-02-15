package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    // Usada tanto quando filtramos por barbeiro (descobrimos o dono dele) quanto pelo login.
    @Query("SELECT s FROM Servico s WHERE s.dono.id = :donoId AND s.ativo = true")
    List<Servico> findAllByDonoIdAndAtivoTrue(@Param("donoId") Long donoId);

    // Validação de duplicidade (Mantida)
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Servico s WHERE LOWER(s.nome) = LOWER(:nome) AND s.dono.id = :donoId")
    boolean existsByNomeIgnoreCaseAndDonoId(@Param("nome") String nome, @Param("donoId") Long donoId);

    // Método auxiliar para buscar pelo email (SaaS)
    @Query("SELECT s FROM Servico s WHERE s.dono.email = :email AND s.ativo = true")
    List<Servico> findAllByDonoEmail(@Param("email") String email);
}