package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import jakarta.transaction.Transactional;
import agendamentoDeClienteBarbearia.repository.ServicoRepository;
import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;
import agendamentoDeClienteBarbearia.model.Servico;
import org.springframework.stereotype.Service;



import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;
import agendamentoDeClienteBarbearia.infra.RegraDeNegocioException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicoService {

    private static final Logger log = LoggerFactory.getLogger(ServicoService.class);
    private final ServicoRepository repository;
    private final BarbeiroRepository barbeiroRepository;

    public ServicoService(ServicoRepository repository, BarbeiroRepository barbeiroRepository) {
        this.repository = repository;
        this.barbeiroRepository = barbeiroRepository;
    }

    @Transactional
    public Servico cadastrar(CadastroServicoDTO dados) {
        log.info("Tentando cadastrar novo serviço: {}", dados.nome());

        // 1. Validação de Duplicidade (Regra de Ouro)
        // Ignora maiúsculas/minúsculas para evitar "Corte" e "corte"
        if (repository.existsByNomeIgnoreCase(dados.nome().trim())) {
            log.warn("Tentativa de cadastro duplicado para: {}", dados.nome());
            throw new RegraDeNegocioException("Já existe um serviço cadastrado com este nome.");
        }

        // 2. Criação Limpa (O Service não precisa saber quais campos a entidade tem)
        var servico = new Servico(dados);

        Servico salvo = repository.save(servico);
        log.info("Serviço '{}' cadastrado com ID: {}", salvo.getNome(), salvo.getId());

        return salvo;
    }

    public List<Servico> listarAtivos() {
        return repository.findAllByAtivoTrue(); // Crie esse método no Repository
    }

    @Transactional
    public void excluir(Long id) {
        var servico = repository.findById(id)
                .orElseThrow(() -> new RegraDeNegocioException("Serviço não encontrado"));
        servico.excluir(); // Chama aquele método 'ativo = false' que criamos na Entidade
    }
}