-- User feedback on AI-generated responses.
-- One row per AI response (enforced by UNIQUE on ai_response_id).
CREATE TABLE feedback (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ai_response_id UUID        NOT NULL UNIQUE,
    rating         VARCHAR(20) NOT NULL,  -- maps to FeedbackRating enum: POSITIVE|NEGATIVE
    comment        TEXT,                  -- optional free-text from the user
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_feedback_ai_response
        FOREIGN KEY (ai_response_id) REFERENCES ai_responses (id) ON DELETE CASCADE
);
