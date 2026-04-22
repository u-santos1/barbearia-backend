-- Adicionando as colunas que faltam para bater com sua Entity
ALTER TABLE tb_expediente ADD COLUMN IF NOT EXISTS hora_inicio TIME;
ALTER TABLE tb_expediente ADD COLUMN IF NOT EXISTS almoco_inicio TIME;
ALTER TABLE tb_expediente ADD COLUMN IF NOT EXISTS almoco_fim TIME;
ALTER TABLE tb_expediente ADD COLUMN IF NOT EXISTS trabalha BOOLEAN DEFAULT TRUE;
ALTER TABLE tb_expediente ADD COLUMN IF NOT EXISTS ativo BOOLEAN DEFAULT TRUE;