-- Existing rows store access_token and refresh_token as plaintext.
-- The AES-256-GCM AttributeConverter introduced alongside this migration
-- cannot decrypt those values, so all sessions must be invalidated.
-- All users will be prompted to log in again via Spotify OAuth2.
TRUNCATE TABLE users;
