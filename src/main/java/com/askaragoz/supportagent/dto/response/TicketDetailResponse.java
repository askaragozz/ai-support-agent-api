package com.askaragoz.supportagent.dto.response;

import com.askaragoz.supportagent.domain.TicketStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Full ticket representation returned by:
 *   POST /api/v1/tickets          (202 — initial creation)
 *   GET  /api/v1/tickets/{id}     (200 — status polling)
 *   POST /api/v1/tickets/{id}/retry (202 — after retry)
 *
 * Includes the full description and the nested AI response (null until RESOLVED).
 * Clients poll this endpoint to check when status transitions to RESOLVED or FAILED.
 */
@Getter
@Setter
@NoArgsConstructor
public class TicketDetailResponse {

    private UUID id;
    private String userEmail;
    private String subject;
    private String description;
    private TicketStatus status;

    /** Populated only when status = FAILED. Describes what went wrong. */
    private String errorMessage;

    /** Populated only when status = RESOLVED. Null for PENDING and IN_PROGRESS. */
    private AiResponseDto aiResponse;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
