-- Install the pgvector extension.
-- 'IF NOT EXISTS' makes this safe to run on databases that already have it.
-- This MUST run before any vector column or operator can be used.
CREATE EXTENSION IF NOT EXISTS vector;

-- Add the embedding column to knowledge_articles.
-- vector(1536) matches the output dimension of OpenAI's text-embedding-3-small model exactly.
-- Nullable: seed articles start without embeddings; they are filled in by the knowledge service.
ALTER TABLE knowledge_articles ADD COLUMN embedding vector(1536);

-- IVFFlat approximate nearest-neighbour index using cosine distance.
-- 'lists = 100' divides the vector space into 100 clusters — a good default for tables up to ~1M rows.
-- This makes the <=> similarity query orders of magnitude faster than a full sequential scan.
-- vector_cosine_ops tells the index to optimise for cosine distance (matches the <=> operator).
CREATE INDEX idx_knowledge_articles_embedding
    ON knowledge_articles
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
