package com.askaragoz.supportagent.controller;

import com.askaragoz.supportagent.dto.request.FeedbackRequest;
import com.askaragoz.supportagent.dto.request.TicketCreateRequest;
import com.askaragoz.supportagent.dto.response.FeedbackResponse;
import com.askaragoz.supportagent.dto.response.TicketDetailResponse;
import com.askaragoz.supportagent.dto.response.TicketResponse;
import com.askaragoz.supportagent.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for support ticket operations.
 *
 * @RestController — combines @Controller and @ResponseBody.
 *   Every method return value is serialised to JSON automatically by Jackson.
 *
 * @RequestMapping — sets the base path for all methods in this class.
 *
 * @RequiredArgsConstructor — injects TicketService via constructor injection.
 *
 * Controllers are kept thin — they only handle HTTP concerns (routing, status codes,
 * request/response serialisation). All business logic lives in TicketService.
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /**
     * Creates a new support ticket and triggers async AI processing.
     * Returns 202 Accepted because processing is not yet complete — the client
     * must poll GET /tickets/{id} to observe the RESOLVED or FAILED outcome.
     *
     * @Valid — triggers Bean Validation on the request body.
     *          If validation fails, Spring throws MethodArgumentNotValidException,
     *          which GlobalExceptionHandler converts to a 400 ProblemDetail.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TicketDetailResponse createTicket(@RequestBody @Valid TicketCreateRequest request) {
        return ticketService.createTicket(request);
    }

    /**
     * Returns a paginated list of tickets for a given email address.
     *
     * @PageableDefault — sets default pagination (page=0, size=20) when the
     *   client does not supply ?page= and ?size= query parameters.
     *
     * Spring Data automatically resolves Pageable from:
     *   ?page=0&size=10&sort=createdAt,desc
     */
    @GetMapping
    public Page<TicketResponse> getTickets(
            @RequestParam String email,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ticketService.getTickets(email, pageable);
    }

    /**
     * Returns full details for a single ticket.
     * The 'aiResponse' field is null until the AI pipeline completes.
     * Clients poll this endpoint to check for RESOLVED or FAILED status.
     */
    @GetMapping("/{id}")
    public TicketDetailResponse getTicket(@PathVariable UUID id) {
        return ticketService.getTicket(id);
    }

    /**
     * Retries AI processing for a ticket in FAILED status.
     * Returns 409 Conflict if the ticket is not in FAILED status.
     */
    @PostMapping("/{id}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TicketDetailResponse retryTicket(@PathVariable UUID id) {
        return ticketService.retryTicket(id);
    }

    /**
     * Records user feedback (POSITIVE or NEGATIVE) on an AI response.
     * Returns 409 Conflict if the ticket has not been resolved yet.
     */
    @PostMapping("/{id}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse addFeedback(
            @PathVariable UUID id,
            @RequestBody @Valid FeedbackRequest request) {
        return ticketService.addFeedback(id, request);
    }
}
