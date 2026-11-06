CREATE TABLE app_users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('AGENT', 'ADJUSTER')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE policies (
    id              BIGSERIAL PRIMARY KEY,
    policy_number   VARCHAR(20)    NOT NULL UNIQUE,
    farm_name       VARCHAR(120)   NOT NULL,
    holder_name     VARCHAR(120)   NOT NULL,
    holder_email    VARCHAR(160)   NOT NULL,
    state_code      VARCHAR(2)       NOT NULL,
    county          VARCHAR(80)    NOT NULL,
    acreage         INTEGER        NOT NULL CHECK (acreage > 0),
    coverage_limit  NUMERIC(14, 2) NOT NULL CHECK (coverage_limit > 0),
    deductible      NUMERIC(12, 2) NOT NULL CHECK (deductible >= 0),
    effective_date  DATE           NOT NULL,
    expiry_date     DATE           NOT NULL,
    agent_id        BIGINT         NOT NULL REFERENCES app_users (id),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CHECK (expiry_date > effective_date)
);

CREATE INDEX idx_policies_agent ON policies (agent_id);

CREATE TABLE equipment (
    id             BIGSERIAL PRIMARY KEY,
    policy_id      BIGINT         NOT NULL REFERENCES policies (id) ON DELETE CASCADE,
    category       VARCHAR(30)    NOT NULL,
    make           VARCHAR(60)    NOT NULL,
    model          VARCHAR(60)    NOT NULL,
    model_year     INTEGER        NOT NULL CHECK (model_year BETWEEN 1950 AND 2100),
    serial_number  VARCHAR(40)    NOT NULL UNIQUE,
    insured_value  NUMERIC(14, 2) NOT NULL CHECK (insured_value > 0),
    engine_hours   INTEGER        NOT NULL DEFAULT 0 CHECK (engine_hours >= 0),
    gps_tracked    BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_equipment_policy ON equipment (policy_id);
