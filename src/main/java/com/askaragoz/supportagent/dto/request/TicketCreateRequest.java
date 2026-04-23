package com.askaragoz.supportagent.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /api/v1/tickets.
 *
 * DTOs (Data Transfer Objects) are the contract between the API and its callers.
 * Using DTOs instead of exposing JPA entities directly gives us:
 *   - Control over what fields are exposed and accepted
 *   - Validation before the data reaches the service layer
 *   - Freedom to evolve the DB schema without breaking the API contract
 *
 * @NotBlank  — fails validation if the field is null, empty, or whitespace-only.
 * @Email     — validates the string is a well-formed email address.
 * @Size      — enforces min/max string length.
 *
 * Validation errors are caught by GlobalExceptionHandler and returned as RFC 7807
 * ProblemDetail responses with field-level error details.
 */
@Getter
@Setter
@NoArgsConstructor
public class TicketCreateRequest {

    @NotBlank(message = "User email is required")
    @Email(message = "User email must be a valid email address")
    private String userEmail;

    @NotBlank(message = "Subject is required")
    @Size(max = 500, message = "Subject must not exceed 500 characters")
    private String subject;

    @NotBlank(message = "Description is required")
    private String description;
}
