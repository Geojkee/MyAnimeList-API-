CREATE TABLE refresh_tokens
(
    id          BIGSERIAL PRIMARY KEY,

    user_id     BIGINT      NOT NULL,

    token       VARCHAR(64) NOT NULL UNIQUE,

    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    revoked_at  TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_user_id ON refresh_tokens (user_id);