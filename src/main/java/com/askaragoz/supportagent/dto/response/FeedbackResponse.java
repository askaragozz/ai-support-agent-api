package com.askaragoz.supportagent.dto.response;

import com.askaragoz.supportagent.domain.FeedbackRating;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response body for POST /api/v1/tickets/{id}/feedback (201 Created).
 */
@Getter
@Setter
@NoArgsConstructor
public class FeedbackResponse {

    private UUID id;
    private UUID aiResponseId;
    private FeedbackRating rating;
    private String comment;
    private LocalDateTime createdAt;
}
