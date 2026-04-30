package agendamentoDeClienteBarbearia.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historico_pagamentos")
@Getter @Setter
public class HistoricoPagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pagamento_id_mp", unique = true, nullable = false)
    private Long pagamentoIdMp;

    @Column(nullable = false)
    private String status;

    private LocalDateTime dataProcessamento;
}
