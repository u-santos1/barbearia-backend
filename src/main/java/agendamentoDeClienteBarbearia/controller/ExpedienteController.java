package agendamentoDeClienteBarbearia.controller;

import agendamentoDeClienteBarbearia.dtos.DadosExpedienteDTO;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Expediente;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.ExpedienteRepository;
import agendamentoDeClienteBarbearia.service.ExpedienteService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RestController
@RequestMapping("/expediente")
@RequiredArgsConstructor
public class ExpedienteController {

    private final ExpedienteService service;

    // Salva ou Atualiza a semana inteira do Barbeiro
    @PostMapping("/{barbeiroId}")
    public ResponseEntity<Void> configurarExpediente(@PathVariable Long barbeiroId, @RequestBody List<DadosExpedienteDTO> dados) {
        service.configurarExpediente(barbeiroId, dados);
        return ResponseEntity.ok().build();
    }

    // Lista a configuração atual
    @GetMapping("/{barbeiroId}")
    public ResponseEntity<List<Expediente>> listar(@PathVariable Long barbeiroId) {
        List<Expediente> expedientes = service.listarPorBarbeiro(barbeiroId);
        return ResponseEntity.ok(expedientes);
    }
}