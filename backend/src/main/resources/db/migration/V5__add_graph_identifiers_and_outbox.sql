-- Identifiers that fraud rings tend to share. Bank accounts are stored only as salted hashes.
ALTER TABLE policies ADD COLUMN holder_phone VARCHAR(20);
ALTER TABLE policies ADD COLUMN payout_account_hash VARCHAR(64);
ALTER TABLE policies ADD COLUMN annual_premium NUMERIC(12, 2);
ALTER TABLE claims ADD COLUMN repair_dealer VARCHAR(120);

CREATE TABLE outbox_events (
    id              BIGSERIAL PRIMARY KEY,
    aggregate_type  VARCHAR(40)  NOT NULL,
    aggregate_id    BIGINT       NOT NULL,
    event_type      VARCHAR(40)  NOT NULL,
    attempts        INTEGER      NOT NULL DEFAULT 0,
    last_error      VARCHAR(500),
    processed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_pending ON outbox_events (id) WHERE processed_at IS NULL;
