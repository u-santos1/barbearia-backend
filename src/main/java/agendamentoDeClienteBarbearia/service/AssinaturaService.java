package agendamentoDeClienteBarbearia.service;

import agendamentoDeClienteBarbearia.StatusAssinatura;
import agendamentoDeClienteBarbearia.dtosResponse.AssinaturaDTO;
import agendamentoDeClienteBarbearia.model.*;
import agendamentoDeClienteBarbearia.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssinaturaService {

    private final AssinaturaClienteRepository assinaturaRepo;
    private final PlanoAssinaturaRepository planoRepo;
    private final BarbeiroRepository barbeiroRepo;
    private final ClienteRepository clienteRepo;
    private final UsoAssinaturaRepository usoRepo;

    // -----------------------------------------------
    // PLANOS
    // -----------------------------------------------

    @Transactional
    public AssinaturaDTO.PlanoResponseDTO criarPlano(AssinaturaDTO.CriarPlanoDTO dto, Long donoId) {
        Barbeiro dono = barbeiroRepo.findById(donoId)
                .orElseThrow(() -> new RuntimeException("Barbeiro não encontrado"));

        PlanoAssinatura plano = new PlanoAssinatura();
        plano.setNome(dto.nome());
        plano.setDescricao(dto.descricao());
        plano.setPreco(dto.preco());
        plano.setQuantidadeCortes(dto.quantidadeCortes());
        plano.setVigenciaDias(dto.vigenciaDias());
        plano.setDono(dono);

        return AssinaturaDTO.PlanoResponseDTO.from(planoRepo.save(plano));
    }

    @Transactional(readOnly = true)
    public List<AssinaturaDTO.PlanoResponseDTO> listarPlanos(Long donoId) {
        return planoRepo.findByDonoIdAndAtivoTrue(donoId)
                .stream().map(AssinaturaDTO.PlanoResponseDTO::from).toList();
    }

    @Transactional
    public void excluirPlano(Long planoId, Long donoId) {
        PlanoAssinatura plano = planoRepo.findById(planoId)
                .orElseThrow(() -> new RuntimeException("Plano não encontrado"));

        if (!plano.getDono().getId().equals(donoId)) {
            throw new RuntimeException("Sem permissão para excluir este plano");
        }
        plano.desativar();
    }

    // -----------------------------------------------
    // ASSINATURAS
    // -----------------------------------------------

    @Transactional
    public AssinaturaDTO.AssinaturaResponseDTO assinarManual(AssinaturaDTO.AssinarDTO dto, Long barbeiroId) {
        Barbeiro barbeiro = barbeiroRepo.findById(barbeiroId)
                .orElseThrow(() -> new RuntimeException("Barbeiro não encontrado"));
        Cliente cliente = clienteRepo.findById(dto.clienteId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        PlanoAssinatura plano = planoRepo.findById(dto.planoId())
                .orElseThrow(() -> new RuntimeException("Plano não encontrado"));

        AssinaturaCliente assinatura = new AssinaturaCliente();
        assinatura.setCliente(cliente);
        assinatura.setPlano(plano);
        assinatura.setBarbeiro(barbeiro);
        assinatura.setDataInicio(LocalDate.now());
        assinatura.setDataExpiracao(LocalDate.now().plusDays(plano.getVigenciaDias()));
        assinatura.setCortesDisponiveis(plano.getQuantidadeCortes());
        assinatura.setCortesUsados(0);
        assinatura.setStatus(StatusAssinatura.ATIVA);
        assinatura.setFormaPagamento("MANUAL");
        assinatura.setObservacao(dto.observacao());

        return AssinaturaDTO.AssinaturaResponseDTO.from(assinaturaRepo.save(assinatura));
    }

    @Transactional(readOnly = true)
    public List<AssinaturaDTO.AssinaturaResponseDTO> listarAssinaturas(Long barbeiroId) {
        return assinaturaRepo.findByBarbeiroId(barbeiroId)
                .stream()
                .peek(AssinaturaCliente::verificarExpiracao)
                .map(AssinaturaDTO.AssinaturaResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssinaturaDTO.AssinaturaResponseDTO buscarAssinaturaAtiva(Long clienteId) {
        return assinaturaRepo.findByClienteIdAndStatus(clienteId, StatusAssinatura.ATIVA)
                .map(AssinaturaDTO.AssinaturaResponseDTO::from)
                .orElseThrow(() -> new RuntimeException("Nenhuma assinatura ativa encontrada"));
    }

    // -----------------------------------------------
    // USAR CORTE
    // -----------------------------------------------

    @Transactional
    public AssinaturaDTO.AssinaturaResponseDTO usarCorte(AssinaturaDTO.UsarCorteDTO dto, Long barbeiroId) {
        Barbeiro barbeiro = barbeiroRepo.findById(barbeiroId)
                .orElseThrow(() -> new RuntimeException("Barbeiro não encontrado"));

        AssinaturaCliente assinatura = assinaturaRepo.findById(dto.assinaturaId())
                .orElseThrow(() -> new RuntimeException("Assinatura não encontrada"));

        assinatura.verificarExpiracao();
        assinatura.usarCorte(); // valida e incrementa cortesUsados

        // Registra o uso no histórico
        UsoAssinatura uso = new UsoAssinatura();
        uso.setAssinatura(assinatura);
        uso.setBarbeiro(barbeiro);
        uso.setAgendamentoId(dto.agendamentoId());
        uso.setObservacao(dto.observacao());
        usoRepo.save(uso);

        return AssinaturaDTO.AssinaturaResponseDTO.from(assinaturaRepo.save(assinatura));
    }

    @Transactional
    public void cancelarAssinatura(Long assinaturaId, Long barbeiroId) {
        AssinaturaCliente assinatura = assinaturaRepo.findById(assinaturaId)
                .orElseThrow(() -> new RuntimeException("Assinatura não encontrada"));

        if (!assinatura.getBarbeiro().getId().equals(barbeiroId)) {
            throw new RuntimeException("Sem permissão para cancelar esta assinatura");
        }
        assinatura.setStatus(StatusAssinatura.CANCELADA);
    }

    // -----------------------------------------------
    // MERCADO PAGO — Webhook
    // -----------------------------------------------

    @Transactional
    public void processarPagamentoMp(String pagamentoId, String status) {
        assinaturaRepo.findByPagamentoMpId(pagamentoId).ifPresent(assinatura -> {
            assinatura.setPagamentoMpStatus(status);
            if ("approved".equals(status)) {
                assinatura.setStatus(StatusAssinatura.ATIVA);
            } else if ("rejected".equals(status)) {
                assinatura.setStatus(StatusAssinatura.CANCELADA);
            }
        });
    }
}
