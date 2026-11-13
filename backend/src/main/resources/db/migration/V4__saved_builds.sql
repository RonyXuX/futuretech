-- V4: ForgeAI saved builds. A build is a named list of parts a customer saved,
-- either picked by hand in the manual builder or suggested by the AI.

CREATE TABLE saved_builds (
    id          BIGSERIAL     PRIMARY KEY,
    user_id     BIGINT        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(100)  NOT NULL,
    source      VARCHAR(10)   NOT NULL,                -- MANUAL / AI
    use_case    VARCHAR(50),                           -- e.g. gaming, editing, office (from the AI form)
    budget      NUMERIC(10,2) CHECK (budget >= 0),     -- budget the user asked for, if any
    created_at  TIMESTAMP     NOT NULL
);
CREATE INDEX idx_saved_builds_user ON saved_builds(user_id);

-- The total price is NOT stored: it is calculated from the current product prices,
-- so a saved build always shows what it would cost today.
CREATE TABLE build_items (
    id         BIGSERIAL PRIMARY KEY,
    build_id   BIGINT    NOT NULL REFERENCES saved_builds(id) ON DELETE CASCADE,
    product_id BIGINT    NOT NULL REFERENCES products(id),
    quantity   INTEGER   NOT NULL DEFAULT 1 CHECK (quantity > 0),  -- e.g. 2 RAM kits or 2 SSDs
    UNIQUE (build_id, product_id)
);
CREATE INDEX idx_build_items_build ON build_items(build_id);
