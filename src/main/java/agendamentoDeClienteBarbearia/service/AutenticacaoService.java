package agendamentoDeClienteBarbearia.service;


import agendamentoDeClienteBarbearia.dtos.LoginDTO;
import agendamentoDeClienteBarbearia.dtosResponse.TokenJWTData;
import agendamentoDeClienteBarbearia.infra.security.TokenService;
import agendamentoDeClienteBarbearia.model.Barbeiro;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService implements UserDetailsService {

    @Autowired
    @Lazy
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
        var authenticationToken = new UsernamePasswordAuthenticationToken(dados.email(), dados.senha());
        var authentication = manager.authenticate(authenticationToken);
        var tokenJWT = tokenService.gerarToken((Barbeiro) authentication.getPrincipal());

        // Pega o usuário logado
        Barbeiro logado = (Barbeiro) authentication.getPrincipal();

        // Retorna o Token + Nome + Especialidade (Perfil)
        return new TokenJWTData(tokenJWT, logado.getNome(), logado.getEspecialidade());
    }
}