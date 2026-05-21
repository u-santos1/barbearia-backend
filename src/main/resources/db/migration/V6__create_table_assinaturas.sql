
CREATE TABLE tb_planos_assinatura (
    id                 BIGSERIAL PRIMARY KEY,
    nome               VARCHAR(100) NOT NULL,
    descricao          VARCHAR(255),
    preco              NUMERIC(19,2) NOT NULL,
    quantidade_cortes  INTEGER NOT NULL,
    vigencia_dias      INTEGER NOT NULL DEFAULT 30,
    ativo              BOOLEAN NOT NULL DEFAULT TRUE,
    dono_id            BIGINT NOT NULL REFERENCES tb_barbeiros(id),
    created_at         TIMESTAMP DEFAULT NOW()
);


CREATE TABLE tb_assinaturas_clientes (
    id                    BIGSERIAL PRIMARY KEY,
    cliente_id            BIGINT NOT NULL REFERENCES tb_clientes(id),
    plano_id              BIGINT NOT NULL REFERENCES tb_planos_assinatura(id),
    barbeiro_id           BIGINT NOT NULL REFERENCES tb_barbeiros(id),
    data_inicio           DATE NOT NULL,
    data_expiracao        DATE NOT NULL,
    cortes_disponiveis    INTEGER NOT NULL,
    cortes_usados         INTEGER NOT NULL DEFAULT 0,
    status                VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    forma_pagamento       VARCHAR(30),
    pagamento_mp_id       VARCHAR(100),
    pagamento_mp_status   VARCHAR(30),
    created_at            TIMESTAMP DEFAULT NOW(),

    CONSTRAINT chk_status CHECK (status IN ('ATIVA','EXPIRADA','CANCELADA','PENDENTE_PAGAMENTO')),
    CONSTRAINT chk_cortes CHECK (cortes_usados <= cortes_disponiveis)
);


CREATE TABLE tb_usos_assinatura (
    id               BIGSERIAL PRIMARY KEY,
    assinatura_id    BIGINT NOT NULL REFERENCES tb_assinaturas_clientes(id),
    agendamento_id   BIGINT REFERENCES tb_agendamentos(id),
    barbeiro_id      BIGINT NOT NULL REFERENCES tb_barbeiros(id),
    data_uso         TIMESTAMP NOT NULL DEFAULT NOW(),
    observacao       VARCHAR(255)
);

CREATE INDEX idx_assinatura_cliente ON tb_assinaturas_clientes(cliente_id);
CREATE INDEX idx_assinatura_status  ON tb_assinaturas_clientes(status);
CREATE INDEX idx_uso_assinatura     ON tb_usos_assinatura(assinatura_id);