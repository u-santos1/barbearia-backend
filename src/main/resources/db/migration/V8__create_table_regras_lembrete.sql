CREATE TABLE regras_lembrete (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    tempo VARCHAR(50),
    hora TIME,
    msg TEXT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    dono_id BIGINT NOT NULL,
    CONSTRAINT fk_regras_lembrete_dono FOREIGN KEY (dono_id) REFERENCES barbeiros(id) ON DELETE CASCADE
);