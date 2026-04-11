-- Remover constraint NOT NULL do password (para usuários convidados)
ALTER TABLE tb_users ALTER COLUMN password DROP NOT NULL;

-- Criar tabela unificada de Tokens (Setup e Reset)
CREATE TABLE tb_auth_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR2(255) NOT NULL UNIQUE,
    type VARCHAR2(50) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id) REFERENCES tb_users (id) ON DELETE CASCADE
);
