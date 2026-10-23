-- Tabelas do sistema de estoque (SQLite).
-- Executado toda vez que o programa abre o banco; so cria o que ainda nao existe.

CREATE TABLE IF NOT EXISTS fornecedor (
    cnpj     TEXT PRIMARY KEY,
    nome     TEXT NOT NULL CHECK (length(trim(nome)) > 0),
    telefone TEXT NOT NULL DEFAULT ''
);

-- Precos em centavos (inteiro): o SQLite nao tem tipo decimal exato.
CREATE TABLE IF NOT EXISTS produto (
    codigo             TEXT PRIMARY KEY,
    nome               TEXT NOT NULL CHECK (length(trim(nome)) > 0),
    preco_centavos     INTEGER NOT NULL CHECK (preco_centavos >= 0),
    quantidade_estoque INTEGER NOT NULL DEFAULT 0 CHECK (quantidade_estoque >= 0),
    quantidade_minima  INTEGER NOT NULL DEFAULT 0 CHECK (quantidade_minima >= 0),
    categoria          TEXT NOT NULL CHECK (categoria IN ('ELETRONICOS', 'ALIMENTOS', 'LIMPEZA', 'VESTUARIO', 'PAPELARIA', 'OUTROS')),
    fornecedor_cnpj    TEXT NOT NULL REFERENCES fornecedor (cnpj)
);

CREATE INDEX IF NOT EXISTS idx_produto_fornecedor ON produto (fornecedor_cnpj);
CREATE INDEX IF NOT EXISTS idx_produto_categoria ON produto (categoria);

-- Historico de entradas e saidas. Guarda o preco unitario da epoca.
CREATE TABLE IF NOT EXISTS movimentacao (
    id                      INTEGER PRIMARY KEY,
    produto_codigo          TEXT NOT NULL REFERENCES produto (codigo),
    tipo                    TEXT NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    quantidade              INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario_centavos INTEGER NOT NULL CHECK (preco_unitario_centavos >= 0),
    data_hora               TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_movimentacao_produto ON movimentacao (produto_codigo);
