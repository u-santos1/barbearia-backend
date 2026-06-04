
ALTER TABLE barbeiros
ADD COLUMN data_cadastro DATE;

ALTER TABLE barbeiros
ADD COLUMN data_expiracao_saas DATE;


UPDATE barbeiros
SET data_cadastro = CURRENT_DATE
WHERE data_cadastro IS NULL;