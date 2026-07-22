CREATE TABLE genres
(
    id   BIGSERIAL PRIMARY KEY,

    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE anime_genre
(
    anime_id BIGINT NOT NULL,
    genre_id BIGINT NOT NULL,

    PRIMARY KEY (anime_id, genre_id),

    CONSTRAINT fk_anime_genre_anime
        FOREIGN KEY (anime_id)
            REFERENCES anime (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_anime_genre_genre
        FOREIGN KEY (genre_id)
            REFERENCES genres (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_anime_genre_genre_id ON anime_genre (genre_id);