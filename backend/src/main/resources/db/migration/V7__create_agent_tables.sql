-- The agent only ever proposes. Runs, every tool call and each proposal were kept for review and reporting.
CREATE TABLE agent_runs (
    id            BIGSERIAL PRIMARY KEY,
    claim_id      BIGINT        NOT NULL REFERENCES claims (id) ON DELETE CASCADE,
    requested_by  BIGINT        NOT NULL REFERENCES app_users (id),
    question      VARCHAR(500)  NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    answer        TEXT,
    supported     BOOLEAN       NOT NULL DEFAULT FALSE,
    citations     VARCHAR(1000),
    tool_calls    INTEGER       NOT NULL DEFAULT 0,
    model         VARCHAR(80)   NOT NULL,
    finished_at   TIMESTAMPTZ,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_agent_runs_claim ON agent_runs (claim_id, created_at DESC);

CREATE TABLE agent_traces (
    id           BIGSERIAL PRIMARY KEY,
    run_id       BIGINT        NOT NULL REFERENCES agent_runs (id) ON DELETE CASCADE,
    step         INTEGER       NOT NULL,
    tool_name    VARCHAR(60)   NOT NULL,
    input        VARCHAR(2000) NOT NULL,
    output       TEXT          NOT NULL,
    failed       BOOLEAN       NOT NULL DEFAULT FALSE,
    duration_ms  BIGINT        NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_agent_traces_run ON agent_traces (run_id, step);

CREATE TABLE agent_proposals (
    id             BIGSERIAL PRIMARY KEY,
    run_id         BIGINT        NOT NULL REFERENCES agent_runs (id) ON DELETE CASCADE,
    claim_id       BIGINT        NOT NULL REFERENCES claims (id) ON DELETE CASCADE,
    action         VARCHAR(20)   NOT NULL CHECK (action IN ('FAST_TRACK', 'REQUEST_INFO', 'INVESTIGATE')),
    reason         VARCHAR(1000) NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    decided_by     BIGINT        REFERENCES app_users (id),
    decision_note  VARCHAR(1000),
    decided_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_agent_proposals_status ON agent_proposals (status, created_at);
