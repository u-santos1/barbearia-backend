package agendamentoDeClienteBarbearia.service;

import jakarta.transaction.Transactional;
import agendamentoDeClienteBarbearia.repository.ServicoRepository;
import agendamentoDeClienteBarbearia.dtos.CadastroServicoDTO;
import agendamentoDeClienteBarbearia.model.Servico;
import org.springframework.stereotype.Service;

@Service
public class ServicoService {

    // Injeção via construtor (Melhor prática que @Autowired em field)
    private final ServicoRepository repository;

    public ServicoService(ServicoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Servico cadastrar(CadastroServicoDTO dados) {
        // Converter DTO para Entidade
        var servico = new Servico();
        servico.setNome(dados.nome());
        servico.setDescricao(dados.descricao());
        servico.setPreco(dados.preco());
        servico.setDuracaoEmMinutos(dados.duracaoEmMinutos());

        return repository.save(servico);
    }
}