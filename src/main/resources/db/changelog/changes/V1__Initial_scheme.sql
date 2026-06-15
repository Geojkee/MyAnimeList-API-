CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,

    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,

    role          VARCHAR(20)  NOT NULL    DEFAULT 'ROLE_USER',

    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE anime
(
    id            BIGSERIAL PRIMARY KEY,

    title_romaji  VARCHAR(255) NOT NULL,
    title_english VARCHAR(255),

    type          VARCHAR(20)  NOT NULL,
    episode_count INT          NOT NULL    DEFAULT 0,

    status        VARCHAR(20)  NOT NULL,

    synopsis      TEXT,

    image_url     VARCHAR(512),

    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_anime_list
(
    id               BIGSERIAL PRIMARY KEY,

    user_id          BIGINT      NOT NULL,
    anime_id         BIGINT      NOT NULL,

    status           VARCHAR(20) NOT NULL,
    score            INT,
    watched_episodes INT         NOT NULL     DEFAULT 0,

    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_anime
        FOREIGN KEY (anime_id)
            REFERENCES anime (id)
            ON DELETE CASCADE,

    CONSTRAINT unique_user_anime
        UNIQUE (user_id, anime_id),

    CONSTRAINT score_check
        CHECK ( score >= 1 AND score <= 10 OR score IS NULL )
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_username ON users (username);

CREATE INDEX idx_anime_title ON anime (title_romaji);

CREATE INDEX idx_user_anime_user_id ON user_anime_list (user_id);
CREATE INDEX idx_user_anime_anime_id ON user_anime_list (anime_id);

ALTER TABLE anime
    ADD CONSTRAINT anime_status_check
        CHECK ( status IN ('ANNOUNCED', 'ONGOING', 'FINISHED'));

ALTER TABLE anime
    ADD CONSTRAINT anime_type_check
        CHECK ( type IN ('TV', 'MOVIE', 'OVA', 'SPECIAL'));

ALTER TABLE user_anime_list
    ADD CONSTRAINT user_anime_status_check
        CHECK ( status IN ('WATCHING', 'COMPLETED', 'ON_HOLD', 'DROPPED', 'PLAN_TO_WATCH'));