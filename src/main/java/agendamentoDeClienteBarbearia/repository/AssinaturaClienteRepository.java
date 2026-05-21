package agendamentoDeClienteBarbearia.repository;


import agendamentoDeClienteBarbearia.StatusAssinatura;
import agendamentoDeClienteBarbearia.model.AssinaturaCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AssinaturaClienteRepository extends JpaRepository<AssinaturaCliente, Long> {

    // Todas as assinaturas ativas de um dono
    @Query("SELECT a FROM AssinaturaCliente a JOIN FETCH a.cliente JOIN FETCH a.plano WHERE a.barbeiro.id = :barbeiroId")
    List<AssinaturaCliente> findByBarbeiroId(@Param("barbeiroId") Long barbeiroId);

    // Assinatura ativa de um cliente específico
    @Query("SELECT a FROM AssinaturaCliente a JOIN FETCH a.plano WHERE a.cliente.id = :clienteId AND a.status = :status")
    Optional<AssinaturaCliente> findByClienteIdAndStatus(@Param("clienteId") Long clienteId, @Param("status") StatusAssinatura status);

    // Buscar pelo ID do pagamento MP
    Optional<AssinaturaCliente> findByPagamentoMpId(String pagamentoMpId);
}