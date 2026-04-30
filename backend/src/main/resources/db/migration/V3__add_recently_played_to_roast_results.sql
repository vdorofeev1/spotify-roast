ALTER TABLE roast_results
    ADD COLUMN IF NOT EXISTS recently_played_json JSONB;
