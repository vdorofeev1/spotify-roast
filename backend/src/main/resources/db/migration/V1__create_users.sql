CREATE TABLE users (
    id               BIGSERIAL PRIMARY KEY,
    spotify_id       VARCHAR(100) NOT NULL UNIQUE,
    display_name     VARCHAR(255),
    access_token     TEXT NOT NULL,
    refresh_token    TEXT NOT NULL,
    token_expires_at TIMESTAMPTZ NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
