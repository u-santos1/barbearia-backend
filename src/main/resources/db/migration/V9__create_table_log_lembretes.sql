CREATE TABLE log_lembretes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agendamento_id BIGINT NOT NULL,
    regra_id BIGINT NOT NULL,
    data_envio DATETIME NOT NULL,
    CONSTRAINT fk_agendamento FOREIGN KEY (agendamento_id) REFERENCES agendamento(id),
    CONSTRAINT fk_regra FOREIGN KEY (regra_id) REFERENCES regras_lembrete(id)
);