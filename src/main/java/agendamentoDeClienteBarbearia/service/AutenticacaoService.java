package agendamentoDeClienteBarbearia.service;


import agendamentoDeClienteBarbearia.dtos.LoginDTO;
import agendamentoDeClienteBarbearia.dtosResponse.TokenJWTData;
import agendamentoDeClienteBarbearia.infra.security.RegistroRateLimitService;
import agendamentoDeClienteBarbearia.infra.security.TokenService;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;




import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j // Logger para auditoria de segurança
@Service

public class AutenticacaoService implements UserDetailsService {

    private final Cache<String, Integer> tentativasCache = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build();

    private final AuthenticationManager manager;
    private final TokenService tokenService;
    private final BarbeiroRepository repository;
    private final RegistroRateLimitService rateLimitService;

    // Injeção via construtor (Melhor prática que @Autowired nos campos)
    // O @Lazy no manager é necessário para evitar Dependência Circular com o SecurityConfig
    public AutenticacaoService(@Lazy AuthenticationManager manager,
                               TokenService tokenService,
                               BarbeiroRepository repository,
                               RegistroRateLimitService rateLimitService) {
        this.manager = manager;
        this.tokenService = tokenService;
        this.repository = repository;
        this.rateLimitService = rateLimitService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // O repositório já deve ter um índice no email para isso ser rápido
        return repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
    }

    public TokenJWTData realizarLogin(LoginDTO dados) {
        String email = dados.email();
        Integer tentativas = tentativasCache.getIfPresent(email);

        if (rateLimitService.isBloqueado(email)){
            log.error("Bloqueio ativo: E-mail {} tentou logar, mas está em cooldown de 15 min.", email);
            throw new LockedException("Muitas tentativas falhas. Conta bloqueada por 15 minutos.");
        }


        try {
            // 1. Cria o token de tentativa (não autenticado ainda)
            var authenticationToken = new UsernamePasswordAuthenticationToken(dados.email(), dados.senha());

            // 2. Tenta autenticar (Vai chamar o loadUserByUsername e verificar hash da senha)
            var authentication = manager.authenticate(authenticationToken);

            rateLimitService.limparTentativas(email);


            // 3. Se passou, pega o usuário logado (Cast seguro pois o manager retornou sucesso)
            Barbeiro logado = (Barbeiro) authentication.getPrincipal();


            // 4. Verifica segurança adicional (Conta ativa?)
            if (!logado.isEnabled()) {
                throw new DisabledException("Conta inativa. Contate o suporte.");
            }


            // 5. Gera o JWT
            var tokenJWT = tokenService.gerarToken(logado);

            log.info("Login realizado com sucesso: {}", logado.getEmail());

            // Pega o nome da barbearia (se for dono, é a própria, se for funcionário, é a do dono)
            String nomeBarbearia;
            if (logado.getDono() != null) {
                nomeBarbearia = logado.getDono().getBarbeariaNome();
            } else {
                nomeBarbearia = logado.getBarbeariaNome();
            }

            // 6. Retorna DTO Completo para o Front-end
            return new TokenJWTData(
                    tokenJWT,
                    logado.getId(),
                    logado.getNome(),
                    logado.getEmail(),
                    logado.getWhatsappContato(),
                    logado.getPerfil() != null ? logado.getPerfil().name() : "BARBEIRO",
                    nomeBarbearia,
                    logado.getPlano() != null ? logado.getPlano().name() : "SOLO",
                    logado.getCreatedAt()
            );

        } catch (BadCredentialsException e) {
            rateLimitService.registrarFalha(email);
            // Lança erro genérico para não expor se o email existe ou não (Segurança)
            throw new BadCredentialsException("Email ou senha inválidos");
        } catch (DisabledException e) {
            log.warn("Tentativa de login em conta desativada: {}", dados.email());
            throw new DisabledException("Sua conta está desativada. Entre em contato com o administrador.");
        }
    }
}