package com.askaragoz.supportagent.domain;

/**
 * Represents the lifecycle of a support ticket.
 *
 * State machine:
 *   PENDING     → IN_PROGRESS  (async AI job picks it up)
 *   IN_PROGRESS → RESOLVED     (AI response saved successfully)
 *   IN_PROGRESS → FAILED       (error during AI processing)
 *   FAILED      → IN_PROGRESS  (user retries via POST /tickets/{id}/retry)
 */
public enum TicketStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED,
    FAILED
}
