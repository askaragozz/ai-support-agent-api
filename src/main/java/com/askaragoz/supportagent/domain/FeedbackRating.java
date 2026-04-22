package com.askaragoz.supportagent.domain;

/**
 * User rating for an AI-generated response.
 * Stored as a VARCHAR string in the feedback table (never as an ordinal integer).
 */
public enum FeedbackRating {
    POSITIVE,
    NEGATIVE
}
