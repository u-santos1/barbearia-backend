package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.dtos.DadosRegraLembreteDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoRegraLembreteDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.RegraLembrete;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.RegraLembreteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegraLembreteService {

    private final RegraLembreteRepository repository;
    private final BarbeiroRepository barbeiroRepository;

    @Transactional
    public DetalhamentoRegraLembreteDTO criar(DadosRegraLembreteDTO dados, String emailLogado) {
        Barbeiro dono = barbeiroRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new EntityNotFoundException("Dono não encontrado"));

        RegraLembrete regra = new RegraLembrete();
        regra.setNome(dados.nome());
        regra.setTempo(dados.tempo());
        regra.setHora(dados.hora());
        regra.setMsg(dados.msg());
        regra.setAtivo(dados.ativo() != null ? dados.ativo() : true);
        regra.setAtivo(dados.ativo());
        regra.setDono(dono);

        repository.save(regra);
        return new DetalhamentoRegraLembreteDTO(regra);
    }

    public List<DetalhamentoRegraLembreteDTO> listar(String emailLogado) {
        return repository.findAllByDonoEmail(emailLogado)
                .stream().map(DetalhamentoRegraLembreteDTO::new).toList();
    }

    @Transactional
    public DetalhamentoRegraLembreteDTO atualizar(Long id, DadosRegraLembreteDTO dados, String emailLogado) {
        RegraLembrete regra = repository.findByIdAndDonoEmail(id, emailLogado)
                .orElseThrow(() -> new RegraDeNegocioException("Regra não encontrada."));

        if (dados.nome() != null) regra.setNome(dados.nome());
        if (dados.msg() != null) regra.setMsg(dados.msg());
        if (dados.hora() != null) regra.setHora(dados.hora());
        if (dados.ativo() != null) regra.setAtivo(dados.ativo());

        return new DetalhamentoRegraLembreteDTO(regra);
    }

    @Transactional
    public void deletar(Long id, String emailLogado) {
        RegraLembrete regra = repository.findByIdAndDonoEmail(id, emailLogado)
                .orElseThrow(() -> new RegraDeNegocioException("Regra não encontrada."));
        repository.delete(regra);
    }
}