package com.askaragoz.supportagent.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Nested DTO representing the AI-generated response inside a TicketDetailResponse.
 * Null when the ticket has not yet been resolved (status is PENDING or IN_PROGRESS).
 */
@Getter
@Setter
@NoArgsConstructor
public class AiResponseDto {

    private UUID id;
    private String responseText;
    private List<UUID> retrievedArticleIds;
    private LocalDateTime createdAt;
}
