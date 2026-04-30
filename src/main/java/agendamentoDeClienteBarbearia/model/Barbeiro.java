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
import org.springframework.util.StringUtils;

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

    @Column(nullable = false)
    private String senha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dono_id")
    private Barbeiro dono;


    @Column(name = "comissao_porcentagem", precision = 5, scale = 2)
    private BigDecimal comissaoPorcentagem = new BigDecimal("50.00");

    @Column(length = 100)
    private String especialidade;

    @Column(nullable = false)
    private Boolean trabalhaComoBarbeiro = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 20)
    private PerfilAcesso perfil = PerfilAcesso.BARBEIRO;

    private Boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoPlano plano = TipoPlano.SOLO;

    private String tokenPushNotification;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private String barbeariaNome;
    private String corPrimaria;
    private String imagemFundo;
    private String whatsappContato;
    private String instagramUrl;
    private String mensagemOla;


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


        if (StringUtils.hasText(dados.barbeariaNome())) {
            this.barbeariaNome = dados.barbeariaNome();
        }

        if (StringUtils.hasText(dados.corPrimaria())) {
            this.corPrimaria = dados.corPrimaria();
        }

        if (StringUtils.hasText(dados.imagemFundo())) {
            this.imagemFundo = dados.imagemFundo();
        }

        if (StringUtils.hasText(dados.whatsappContato())) {
            this.whatsappContato = dados.whatsappContato();
        }

        if (StringUtils.hasText(dados.instagramUrl())) {
            this.instagramUrl = dados.instagramUrl();
        }

        if (StringUtils.hasText(dados.mensagemOla())) {
            this.mensagemOla = dados.mensagemOla();
        }
    }

}