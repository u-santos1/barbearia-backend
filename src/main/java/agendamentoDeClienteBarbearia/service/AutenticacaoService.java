package agendamentoDeClienteBarbearia.service;


import agendamentoDeClienteBarbearia.dtos.LoginDTO;
import agendamentoDeClienteBarbearia.dtosResponse.TokenJWTData;
import agendamentoDeClienteBarbearia.infra.security.TokenService;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;




import agendamentoDeClienteBarbearia.repository.BarbeiroRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

@Slf4j // Logger para auditoria de segurança
@Service
public class AutenticacaoService implements UserDetailsService {

    private final AuthenticationManager manager;
    private final TokenService tokenService;
    private final BarbeiroRepository repository;

    // Injeção via construtor (Melhor prática que @Autowired nos campos)
    // O @Lazy no manager é necessário para evitar Dependência Circular com o SecurityConfig
    public AutenticacaoService(@Lazy AuthenticationManager manager,
                               TokenService tokenService,
                               BarbeiroRepository repository) {
        this.manager = manager;
        this.tokenService = tokenService;
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // O repositório já deve ter um índice no email para isso ser rápido
        return repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
    }

    public TokenJWTData realizarLogin(LoginDTO dados) {
        try {
            // 1. Cria o token de tentativa (não autenticado ainda)
            var authenticationToken = new UsernamePasswordAuthenticationToken(dados.email(), dados.senha());

            // 2. Tenta autenticar (Vai chamar o loadUserByUsername e verificar hash da senha)
            var authentication = manager.authenticate(authenticationToken);

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
            String nomeBarbearia = "Barbearia";
            if (logado.getDono() != null) {
                nomeBarbearia = logado.getDono().getNome(); // Ou um campo nomeFantasia se tiver
            }

            // 6. Retorna DTO Completo para o Front-end
            return new TokenJWTData(
                    tokenJWT,
                    logado.getId(),
                    logado.getNome(),
                    logado.getEmail(),
                    logado.getPerfil() != null ? logado.getPerfil().name() : "BARBEIRO",
                    nomeBarbearia
            );

        } catch (BadCredentialsException e) {
            log.warn("Tentativa de login falha (senha inválida): {}", dados.email());
            // Lança erro genérico para não expor se o email existe ou não (Segurança)
            throw new BadCredentialsException("Email ou senha inválidos");
        } catch (DisabledException e) {
            log.warn("Tentativa de login em conta desativada: {}", dados.email());
            throw new DisabledException("Sua conta está desativada. Entre em contato com o administrador.");
        }
    }
}