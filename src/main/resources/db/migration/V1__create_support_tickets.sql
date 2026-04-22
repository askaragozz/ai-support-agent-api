-- Support tickets table.
-- Stores every customer request submitted via POST /api/v1/tickets.
CREATE TABLE support_tickets (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_email    VARCHAR(255) NOT NULL,
    subject       VARCHAR(500) NOT NULL,
    description   TEXT         NOT NULL,
    status        VARCHAR(50)  NOT NULL,   -- maps to TicketStatus enum: PENDING|IN_PROGRESS|RESOLVED|FAILED
    error_message TEXT,                    -- populated only when status = FAILED
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Supports the most common query: GET /tickets?email=
CREATE INDEX idx_support_tickets_user_email ON support_tickets (user_email);

-- Supports admin/status-based lookups and the retry endpoint existence check.
CREATE INDEX idx_support_tickets_status ON support_tickets (status);
