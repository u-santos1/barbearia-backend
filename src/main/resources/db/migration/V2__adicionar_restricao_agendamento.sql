

-- 1. Instala a extensão necessária para comparações geométricas/temporais (se não existir)
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- 2. Adiciona a restrição de exclusão (Exclusion Constraint)
-- Tradução: "Para o mesmo barbeiro_id (=), os intervalos de tempo (&&) não podem se tocar"
ALTER TABLE tb_agendamentos
ADD CONSTRAINT no_overlap_agendamento
EXCLUDE USING GIST (
    barbeiro_id WITH =,
    tsrange(data_hora_inicio, data_hora_fim) WITH &&
);