CREATE TABLE password_reset_tokens (
    id          BINARY(16)   NOT NULL PRIMARY KEY,
    user_id     BINARY(16)   NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMP(6) NOT NULL,
    used_at     TIMESTAMP(6) NULL,
    created_at  TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_user ON password_reset_tokens (user_id);
CREATE INDEX idx_password_reset_expires ON password_reset_tokens (expires_at);
