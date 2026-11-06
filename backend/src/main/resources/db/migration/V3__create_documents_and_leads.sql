CREATE TABLE claim_documents (
    id            BIGSERIAL PRIMARY KEY,
    claim_id      BIGINT       NOT NULL REFERENCES claims (id) ON DELETE CASCADE,
    uploaded_by   BIGINT       NOT NULL REFERENCES app_users (id),
    file_name     VARCHAR(160) NOT NULL,
    content_type  VARCHAR(60)  NOT NULL,
    size_bytes    BIGINT       NOT NULL CHECK (size_bytes > 0),
    sha256        VARCHAR(64)     NOT NULL,
    content       BYTEA        NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_claim ON claim_documents (claim_id);

CREATE TABLE quote_leads (
    id                  BIGSERIAL PRIMARY KEY,
    full_name           VARCHAR(120)   NOT NULL,
    email               VARCHAR(160)   NOT NULL,
    phone               VARCHAR(20),
    state_code          VARCHAR(2)       NOT NULL,
    equipment_category  VARCHAR(30)    NOT NULL,
    equipment_value     NUMERIC(14, 2) NOT NULL,
    model_year          INTEGER        NOT NULL,
    deductible          NUMERIC(12, 2) NOT NULL,
    annual_premium      NUMERIC(12, 2) NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'NEW',
    assigned_agent_id   BIGINT         REFERENCES app_users (id),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_leads_status ON quote_leads (status);
