CREATE TABLE friendlist
(
    id         BIGSERIAL PRIMARY KEY,

    user_id    BIGINT      NOT NULL,
    friend_id  BIGINT      NOT NULL,

    status     VARCHAR(20) NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_friendlist_user
        FOREIGN KEY (user_id)
            references users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_friendlist_friend
        FOREIGN KEY (friend_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_user_not_equal_friend CHECK (user_id != friend_id),
    CONSTRAINT uk_friendlist_user_friend UNIQUE (user_id, friend_id)
);

CREATE INDEX idx_friendships_user_id ON friendlist (user_id);
CREATE INDEX idx_friendships_friend_id ON friendlist (friend_id);
CREATE INDEX idx_friendships_status ON friendlist (status);