-- Adiciona a coluna despesa na tabela barbeiros
ALTER TABLE barbeiros ADD COLUMN despesa NUMERIC(10, 2) DEFAULT 0.00;
