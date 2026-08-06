CREATE TABLE user_anime_list
(
    id               BIGSERIAL PRIMARY KEY,

    user_id          BIGINT      NOT NULL,
    anime_id         BIGINT      NOT NULL,

    status           VARCHAR(20) NOT NULL,

    score            INT CHECK (score >= 1 AND score <= 10),

    watched_episodes INT         NOT NULL     DEFAULT 0,

    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_anime_list_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_user_anime_list_anime
        FOREIGN KEY (anime_id)
            REFERENCES anime (id)
            ON DELETE CASCADE,

    CONSTRAINT unique_user_anime
        UNIQUE (user_id, anime_id)
);

CREATE INDEX idx_user_anime_list_user_status ON user_anime_list (user_id, status);