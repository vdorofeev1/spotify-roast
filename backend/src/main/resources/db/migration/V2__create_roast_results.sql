CREATE TABLE roast_results (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    roast_text          TEXT NOT NULL,
    top_artists_json    JSONB,
    top_tracks_json     JSONB,
    audio_features_json JSONB,
    generated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_roast_results_user_id ON roast_results(user_id);
