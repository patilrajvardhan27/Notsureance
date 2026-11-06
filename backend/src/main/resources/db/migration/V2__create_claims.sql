CREATE TABLE claims (
    id                  BIGSERIAL PRIMARY KEY,
    claim_number        VARCHAR(24)    NOT NULL UNIQUE,
    policy_id           BIGINT         NOT NULL REFERENCES policies (id),
    equipment_id        BIGINT         NOT NULL REFERENCES equipment (id),
    filed_by            BIGINT         NOT NULL REFERENCES app_users (id),
    assigned_adjuster   BIGINT         REFERENCES app_users (id),
    cause               VARCHAR(30)    NOT NULL,
    status              VARCHAR(20)    NOT NULL,
    incident_date       DATE           NOT NULL,
    reported_at         TIMESTAMPTZ    NOT NULL,
    description         TEXT           NOT NULL,
    estimated_loss      NUMERIC(14, 2) NOT NULL CHECK (estimated_loss > 0),
    approved_amount     NUMERIC(14, 2) CHECK (approved_amount >= 0),
    incident_latitude   NUMERIC(9, 6),
    incident_longitude  NUMERIC(9, 6),
    harvest_critical    BOOLEAN        NOT NULL DEFAULT FALSE,
    fraud_score         NUMERIC(5, 4),
    predicted_payout    NUMERIC(14, 2),
    anomaly_score       NUMERIC(6, 3),
    predicted_cause     VARCHAR(30),
    triage_lane         VARCHAR(30),
    priority_score      INTEGER        NOT NULL DEFAULT 0,
    decision_reason     TEXT,
    confirmed_fraud     BOOLEAN        NOT NULL DEFAULT FALSE,
    version             BIGINT         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_claims_status ON claims (status);
CREATE INDEX idx_claims_filed_by ON claims (filed_by);
CREATE INDEX idx_claims_policy ON claims (policy_id);
CREATE INDEX idx_claims_priority ON claims (priority_score DESC);

CREATE TABLE claim_events (
    id          BIGSERIAL PRIMARY KEY,
    claim_id    BIGINT       NOT NULL REFERENCES claims (id) ON DELETE CASCADE,
    actor_id    BIGINT       REFERENCES app_users (id),
    event_type  VARCHAR(30)  NOT NULL,
    from_status VARCHAR(20),
    to_status   VARCHAR(20),
    note        VARCHAR(2000),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_claim_events_claim ON claim_events (claim_id, created_at);
