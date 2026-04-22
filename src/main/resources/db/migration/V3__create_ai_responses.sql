-- Stores the AI-generated response for each resolved support ticket.
-- One row per ticket (enforced by UNIQUE on ticket_id).
CREATE TABLE ai_responses (
    id                    UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id             UUID      NOT NULL UNIQUE,
    response_text         TEXT      NOT NULL,
    retrieved_article_ids JSONB,    -- JSON array of KnowledgeArticle UUIDs used in this response
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_ai_responses_ticket
        FOREIGN KEY (ticket_id) REFERENCES support_tickets (id) ON DELETE CASCADE
);
