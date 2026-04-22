-- Knowledge base articles used by the RAG pipeline.
-- The 'embedding' vector column is intentionally absent here.
-- It is added in V5 after the pgvector extension is installed.
-- Flyway runs migrations in version order, so V5 can safely ALTER this table.
CREATE TABLE knowledge_articles (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title      VARCHAR(500) NOT NULL,
    content    TEXT         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
