package agendamentoDeClienteBarbearia.service;


import agendamentoDeClienteBarbearia.dtos.LoginDTO;
import agendamentoDeClienteBarbearia.dtosResponse.TokenJWTData;
import agendamentoDeClienteBarbearia.infra.security.TokenService;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService implements UserDetailsService {

    @Autowired
    private AuthenticationManager manager; // O Service agora gerencia a autenticação

    @Autowired
    private TokenService tokenService;


    @Autowired
    private agendamentoDeClienteBarbearia.repository.BarbeiroRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
    }

    // --- AQUI ESTÁ A REGRA DE NEGÓCIO DO LOGIN ---
    public TokenJWTData realizarLogin(LoginDTO dados) {
        // 1. Cria o token de dados (ainda não autenticado)
        var authenticationToken = new UsernamePasswordAuthenticationToken(dados.email(), dados.senha());

        // 2. O Spring Security bate no banco e verifica o hash da senha
        var authentication = manager.authenticate(authenticationToken);

        // 3. Se passou, pegamos o usuário logado
        var usuario = (Barbeiro) authentication.getPrincipal();

        // 4. Geramos o Token JWT
        var tokenJWT = tokenService.gerarToken(usuario);

        // 5. Retornamos o DTO pronto
        return new TokenJWTData(tokenJWT, usuario.getNome(), usuario.getId());
    }
}