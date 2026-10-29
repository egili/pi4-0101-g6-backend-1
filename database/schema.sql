-- =============================================================================
-- Projeto Integrador — Cofre de Senhas Criptografadas (Cofre Digital)
-- Script DDL de Inicialização do Banco de Dados PostgreSQL
-- Contratos Atendidos: CT9 (Tabela usuario) e CT10 (Tabela credencial)
-- Tarefa #823: Criação de tabelas em banco vazio com username único e credencial
-- vinculada ao usuário via Foreign Key.
-- =============================================================================

-- Criação da tabela usuario (CT9)
CREATE TABLE IF NOT EXISTS usuario (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    senha_mestra_hash TEXT NOT NULL,
    salt TEXT NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Criação da tabela credencial (CT10)
CREATE TABLE IF NOT EXISTS credencial (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    servico VARCHAR(255) NOT NULL,
    usuario_servico VARCHAR(255) NOT NULL,
    dados_cifrados TEXT NOT NULL,
    iv TEXT NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para otimização de consultas
CREATE INDEX IF NOT EXISTS idx_usuario_username ON usuario(username);
CREATE INDEX IF NOT EXISTS idx_credencial_usuario_id ON credencial(usuario_id);
