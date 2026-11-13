-- V6: login security.

-- ---------- Brute-force protection: count wrong passwords and lock the account for a while
ALTER TABLE users ADD COLUMN failed_login_attempts INTEGER   NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until          TIMESTAMP;           -- null = not locked

-- ---------- Refresh tokens (the long-lived half of a login)
-- Only a SHA-256 hash of the token is stored: if this table leaks, the tokens can't be used.
-- A token is "used up" (revoked_at set) every time it is exchanged for a new one.
CREATE TABLE refresh_tokens (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,     -- SHA-256 as 64 hex characters
    expires_at  TIMESTAMP   NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    revoked_at  TIMESTAMP                        -- null = still usable
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
