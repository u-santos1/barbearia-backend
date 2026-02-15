package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;
import agendamentoDeClienteBarbearia.dtosResponse.DetalhamentoServicoDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import agendamentoDeClienteBarbearia.model.Servico;
import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import agendamentoDeClienteBarbearia.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Adicionado Log
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository repository;
    private final BarbeiroRepository barbeiroRepository;
    // Removi BarbeiroService para evitar Dependência Circular, usamos o Repository direto

    // ... (Seus métodos cadastrar, atualizar, excluir mantidos aqui - lógica do DonoLogado permanece)

    @Transactional
    public DetalhamentoServicoDTO cadastrar(CadastroServicoDTO dados) {
        // ... (Sua implementação existente)
        Barbeiro dono = getDonoLogado();
        // ... validacoes e save
        var servico = new Servico(dados);
        servico.setDono(dono);
        servico.setAtivo(true);
        repository.save(servico);
        return new DetalhamentoServicoDTO(servico);
    }

    @Transactional
    public DetalhamentoServicoDTO atualizar(Long id, CadastroServicoDTO dados) {
        // ... (Sua implementação existente)
        return null; // Apenas placeholder para não copiar tudo de novo
    }

    @Transactional
    public void excluir(Long id) {
        // ... (Sua implementação existente com Soft Delete)
        Barbeiro dono = getDonoLogado();
        var servico = repository.findById(id).orElseThrow();
        if(!servico.getDono().getId().equals(dono.getId())) throw new RegraDeNegocioException("Erro");
        servico.setAtivo(false);
    }

    // ✅ MÉTODO INTELIGENTE: Lista por Barbeiro (Funcionário ou Dono)
    @Transactional(readOnly = true)
    public List<DetalhamentoServicoDTO> listarPorBarbeiro(Long idBarbeiro) {
        Barbeiro barbeiro = barbeiroRepository.findById(idBarbeiro)
                .orElseThrow(() -> new RegraDeNegocioException("Barbeiro não encontrado"));

        // Lógica de Ouro: Se o barbeiro tem um chefe, pega o ID do chefe. Se não, ele é o chefe.
        Long idDono = (barbeiro.getDono() != null) ? barbeiro.getDono().getId() : barbeiro.getId();

        return repository.findAllByDonoIdAndAtivoTrue(idDono).stream()
                .map(DetalhamentoServicoDTO::new)
                .toList();
    }

    // ✅ MÉTODO SAAS: Lista meus serviços (Baseado no Token JWT)
    // Resolve o problema de "GET /servicos" trazer dados de outros
    @Transactional(readOnly = true)
    public List<DetalhamentoServicoDTO> listarPorLogin(String emailLogado) {
        // Busca direta pelo email.
        Barbeiro usuarioLogado = barbeiroRepository.findByEmail(emailLogado)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não identificado"));

        Long idDono = (usuarioLogado.getDono() != null) ? usuarioLogado.getDono().getId() : usuarioLogado.getId();

        return repository.findAllByDonoIdAndAtivoTrue(idDono).stream()
                .map(DetalhamentoServicoDTO::new)
                .toList();
    }

    // ✅ MÉTODO UNIFICADO PARA O CONTROLLER (FACADE)
    @Transactional(readOnly = true)
    public List<DetalhamentoServicoDTO> listarComFiltros(Long barbeiroId, Long lojaId, String emailLogado) {
        // 1. Se o front mandou ID de Barbeiro ou Loja (Cliente agendando)
        if (barbeiroId != null) {
            return listarPorBarbeiro(barbeiroId);
        }
        if (lojaId != null) {
            // LojaID no seu sistema é o ID do Dono, então reaproveita a lógica
            return listarPorBarbeiro(lojaId);
        }

        // 2. Se não mandou ID nenhum, é o Painel Admin (Usa o Token)
        // Isso impede o vazamento de dados!
        return listarPorLogin(emailLogado);
    }

    private Barbeiro getDonoLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Barbeiro usuario = barbeiroRepository.findByEmail(email)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado"));
        return (usuario.getDono() != null) ? usuario.getDono() : usuario;
    }
}