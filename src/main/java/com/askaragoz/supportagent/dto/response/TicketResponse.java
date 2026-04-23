package com.askaragoz.supportagent.dto.response;

import com.askaragoz.supportagent.domain.TicketStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lightweight ticket representation used in paginated list responses.
 * GET /api/v1/tickets?email=
 *
 * Omits 'description' and 'aiResponse' to keep list responses compact.
 * Use TicketDetailResponse when the full ticket is needed.
 */
@Getter
@Setter
@NoArgsConstructor
public class TicketResponse {

    private UUID id;
    private String userEmail;
    private String subject;
    private TicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
