package agendamentoDeClienteBarbearia.repository;

import agendamentoDeClienteBarbearia.model.Expediente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface ExpedienteRepository extends JpaRepository<Expediente, Long> {

    // Busca o horário de um dia específico (ex: SEXTA para o Barbeiro X)
    Optional<Expediente> findByBarbeiroIdAndDiaSemana(Long barbeiroId, DayOfWeek diaSemana);

    // Busca a semana inteira (para configurar no painel)
    List<Expediente> findByBarbeiroIdOrderByDiaSemana(Long barbeiroId);
}