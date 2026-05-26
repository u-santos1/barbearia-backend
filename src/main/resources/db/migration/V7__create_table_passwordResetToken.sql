CREATE TABLE tb_password_reset_tokens (
id BIGSERIAL PRIMARY KEY,
token VARCHAR(255) NOT NULL UNIQUE,
data_expiracao TIMESTAMP NOT NULL,
barbeiro_id BIGINT NOT NULL,

CONSTRAINT fk_reset_token_barbeiro FOREIGN KEY (barbeiro_id) REFERENCES tb_barbeiros (id) ON DELETE CASCADE
);

CREATE INDEX idx_data_expiracao ON tb_password_reset_tokens(data_expiracao);

CREATE UNIQUE INDEX idx_token_reset ON tb_password_reset_tokens(token);)