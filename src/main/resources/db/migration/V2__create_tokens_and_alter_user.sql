-- Remover constraint NOT NULL do password
ALTER TABLE tb_users ALTER COLUMN password DROP NOT NULL;

-- Criar tabela de Tokens de Setup de Senha / Convites
CREATE TABLE tb_password_setup_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_password_setup_user FOREIGN KEY (user_id) REFERENCES tb_users (id) ON DELETE CASCADE
);
