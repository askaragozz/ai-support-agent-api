package com.askaragoz.supportagent.dto.request;

import com.askaragoz.supportagent.domain.FeedbackRating;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /api/v1/tickets/{id}/feedback.
 */
@Getter
@Setter
@NoArgsConstructor
public class FeedbackRequest {

    @NotNull(message = "Rating is required")
    private FeedbackRating rating;

    /** Optional free-text explanation for the rating. */
    private String comment;
}
