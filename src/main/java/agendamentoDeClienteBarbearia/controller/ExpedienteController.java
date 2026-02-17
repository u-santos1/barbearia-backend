package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.DadosExpedienteDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Expediente;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.ExpedienteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expediente")
@RequiredArgsConstructor
public class ExpedienteController {

    private final ExpedienteRepository repository;
    private final BarbeiroRepository barbeiroRepository;

    // Salva ou Atualiza a semana inteira do Barbeiro
    @PostMapping("/{barbeiroId}")
    @Transactional
    public ResponseEntity configurarExpediente(@PathVariable Long barbeiroId, @RequestBody List<DadosExpedienteDTO> dados) {
        Barbeiro barbeiro = barbeiroRepository.findById(barbeiroId).orElseThrow();

        for (DadosExpedienteDTO d : dados) {
            // Tenta achar o dia já existente ou cria um novo
            Expediente expediente = repository.findByBarbeiroIdAndDiaSemana(barbeiroId, d.diaSemana())
                    .orElse(new Expediente());

            expediente.setBarbeiro(barbeiro);
            expediente.setDiaSemana(d.diaSemana());
            expediente.setAbertura(d.abertura());
            expediente.setFechamento(d.fechamento());
            expediente.setTrabalha(d.trabalha());

            repository.save(expediente);
        }
        return ResponseEntity.ok().build();
    }

    // Lista a configuração atual
    @GetMapping("/{barbeiroId}")
    public ResponseEntity<List<Expediente>> listar(@PathVariable Long barbeiroId) {
        return ResponseEntity.ok(repository.findByBarbeiroIdOrderByDiaSemana(barbeiroId));
    }
}