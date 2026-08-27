CREATE TABLE comments
(
    id         BIGSERIAL PRIMARY KEY,

    user_id    BIGINT       NOT NULL,
    anime_id   BIGINT       NOT NULL,

    text       VARCHAR(500) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_comment_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_comment_anime
        FOREIGN KEY (anime_id)
            REFERENCES anime (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_comments_anime_id ON comments (anime_id);
CREATE INDEX idx_comments_user_id ON comments (user_id);