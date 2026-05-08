package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.dtos.DadosExpedienteDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Expediente;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.ExpedienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpedienteService {

    private final ExpedienteRepository repository;
    private final BarbeiroRepository barbeiroRepository;

    @Transactional
    public void configurarExpediente(Long barbeiroId, List<DadosExpedienteDTO> dados) {

        Barbeiro barbeiro = barbeiroRepository.findById(barbeiroId)
                .orElseThrow(() -> new EntityNotFoundException("Barbeiro não encontrado."));

        for (DadosExpedienteDTO d : dados) {

            Expediente expediente = repository.findByBarbeiroIdAndDiaSemana(barbeiroId, d.diaSemana())
                    .orElse(new Expediente());

            expediente.setBarbeiro(barbeiro);
            expediente.setDiaSemana(d.diaSemana());
            expediente.setAbertura(d.abertura());
            expediente.setFechamento(d.fechamento());
            expediente.setTrabalha(d.trabalha());

            repository.save(expediente);
        }
    }

    public List<Expediente> listarPorBarbeiro(Long barbeiroId) {
        return repository.findByBarbeiroIdOrderByDiaSemana(barbeiroId);
    }
}
