-- Policy wording chunks for retrieval. The vector size matched both the local hashing embedder and the
-- 384-dimension Azure OpenAI setting, so switching embedders only re-indexes rows.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE policy_clauses (
    id            BIGSERIAL PRIMARY KEY,
    clause_id     VARCHAR(20)  NOT NULL UNIQUE,
    heading       VARCHAR(160) NOT NULL,
    body          TEXT         NOT NULL,
    content_hash  VARCHAR(64)  NOT NULL,
    embedder      VARCHAR(40)  NOT NULL,
    embedding     vector(384)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_policy_clauses_embedding ON policy_clauses USING hnsw (embedding vector_cosine_ops);
