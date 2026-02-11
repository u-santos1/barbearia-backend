package agendamentoDeClienteBarbearia.model;



import agendamentoDeClienteBarbearia.PerfilAcesso;
import agendamentoDeClienteBarbearia.TipoPlano;
import agendamentoDeClienteBarbearia.dtos.AtualizacaoBarbeiroDTO;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "tb_barbeiros", indexes = {
        @Index(name = "idx_barbeiro_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Barbeiro implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @Column(nullable = false) // Senha encriptada é longa, deixe 255 padrão
    private String senha;

    @ManyToOne(fetch = FetchType.LAZY) // Evita carregar o dono sempre que carregar o barbeiro
    @JoinColumn(name = "dono_id")
    private Barbeiro dono;

    // Use BigDecimal para porcentagem financeira para evitar erros como 0.1 + 0.2 = 0.300000004
    @Column(name = "comissao_porcentagem", precision = 5, scale = 2)
    private BigDecimal comissaoPorcentagem = new BigDecimal("50.00");

    @Column(length = 100)
    private String especialidade;

    @Column(nullable = false)
    private Boolean trabalhaComoBarbeiro = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PerfilAcesso perfil;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoPlano plano = TipoPlano.SOLO;

    private String tokenPushNotification;

    @CreationTimestamp // Preenche a data/hora automaticamente ao salvar
    private LocalDateTime createdAt;

    private String barbeariaNome;
    private String corPrimaria;
    private String imagemFundo;
    private String whatsappContato;
    private String instagramUrl;
    private String mensagemOla;

    // Otimização: Authorities geralmente não mudam dinamicamente por request
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.perfil == null) return Collections.emptyList();
        // Adiciona ROLE_ prefixo que o Spring Security geralmente espera
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.perfil.name()));
    }

    @Override
    public String getPassword() { return senha; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return ativo; } // Se inativo, conta bloqueada

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return ativo; }
    // Dentro de Barbeiro.java
    public void atualizarInformacoes(AtualizacaoBarbeiroDTO dados) {
        if (dados.barbeariaNome() != null && !dados.barbeariaNome().isBlank()) {
            this.barbeariaNome = dados.barbeariaNome();
        }
        if (dados.corPrimaria() != null) {
            this.corPrimaria = dados.corPrimaria();
        }
        if (dados.imagemFundo() != null) {
            this.imagemFundo = dados.imagemFundo();
        }
        if (dados.whatsappContato() != null) {
            this.whatsappContato = dados.whatsappContato();
        }
        if (dados.instagramUrl() != null) {
            this.instagramUrl = dados.instagramUrl();
        }
        if (dados.mensagemOla() != null) {
            this.mensagemOla = dados.mensagemOla();
        }
    }
}