package com.askaragoz.supportagent.service;

import com.askaragoz.supportagent.domain.Feedback;
import com.askaragoz.supportagent.domain.SupportTicket;
import com.askaragoz.supportagent.domain.TicketStatus;
import com.askaragoz.supportagent.dto.request.FeedbackRequest;
import com.askaragoz.supportagent.dto.request.TicketCreateRequest;
import com.askaragoz.supportagent.dto.response.FeedbackResponse;
import com.askaragoz.supportagent.dto.response.TicketDetailResponse;
import com.askaragoz.supportagent.dto.response.TicketResponse;
import com.askaragoz.supportagent.mapper.FeedbackMapper;
import com.askaragoz.supportagent.mapper.TicketMapper;
import com.askaragoz.supportagent.repository.FeedbackRepository;
import com.askaragoz.supportagent.repository.SupportTicketRepository;
import com.askaragoz.supportagent.service.ai.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Business logic for support ticket operations.
 *
 * @Service — marks this as a Spring-managed service bean.
 *            Spring creates one instance and injects it into TicketController.
 *
 * @RequiredArgsConstructor — Lombok generates a constructor for all final fields.
 *            Spring uses constructor injection — the recommended approach over
 *            field injection (@Autowired) because it makes dependencies explicit
 *            and enables easy unit testing without a Spring context.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final SupportTicketRepository ticketRepository;
    private final FeedbackRepository feedbackRepository;
    private final RagService ragService;
    private final TicketMapper ticketMapper;
    private final FeedbackMapper feedbackMapper;

    /**
     * Creates a new support ticket and triggers async AI processing.
     *
     * NO @Transactional here — intentional.
     * ticketRepository.save() opens and commits its own transaction immediately.
     * The ticket is visible in the DB before ragService.processTicketAsync() is called.
     * If createTicket were @Transactional, the ticket would be committed only when
     * this method returns — after the async thread has already started and may have
     * tried to find the ticket (resulting in "Ticket not found").
     */
    public TicketDetailResponse createTicket(TicketCreateRequest request) {
        SupportTicket ticket = SupportTicket.builder()
                .userEmail(request.getUserEmail())
                .subject(request.getSubject())
                .description(request.getDescription())
                .status(TicketStatus.PENDING)
                .build();

        ticket = ticketRepository.save(ticket); // commits immediately

        // Trigger async AI pipeline — returns immediately, runs on a virtual thread.
        // The client polls GET /tickets/{id} to check when status becomes RESOLVED.
        ragService.processTicketAsync(ticket.getId());

        log.info("Created ticket {} for {}", ticket.getId(), ticket.getUserEmail());
        return ticketMapper.toTicketDetailResponse(ticket);
    }

    /**
     * Returns a paginated list of tickets for a given email.
     *
     * @Transactional(readOnly = true) — opens a read-only session.
     *   'readOnly' tells Hibernate to skip dirty-checking (no need to track changes)
     *   and signals to the DB it can use a read replica if available.
     *   Required here because the mapper accesses ticket fields within the same session.
     */
    @Transactional(readOnly = true)
    public Page<TicketResponse> getTickets(String email, Pageable pageable) {
        return ticketRepository.findByUserEmail(email, pageable)
                .map(ticketMapper::toTicketResponse);
    }

    /**
     * Returns the full details of a single ticket.
     *
     * @Transactional(readOnly = true) is required because the mapper accesses
     * ticket.getAiResponse() — a LAZY relationship. Without an open session, Hibernate
     * would throw LazyInitializationException. The transaction keeps the session open
     * while MapStruct traverses the relationship.
     */
    @Transactional(readOnly = true)
    public TicketDetailResponse getTicket(UUID id) {
        return ticketRepository.findById(id)
                .map(ticketMapper::toTicketDetailResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ticket not found: " + id));
    }

    /**
     * Retries AI processing for a ticket that previously failed.
     * Returns 409 Conflict if the ticket is not in FAILED status.
     *
     * NO @Transactional — same reason as createTicket: the ticket must be committed
     * before the async thread starts.
     */
    public TicketDetailResponse retryTicket(UUID id) {
        if (!ticketRepository.existsByIdAndStatus(id, TicketStatus.FAILED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ticket " + id + " cannot be retried — it is not in FAILED status");
        }

        SupportTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ticket not found: " + id));

        ticket.setStatus(TicketStatus.PENDING);
        ticket.setErrorMessage(null);
        ticket = ticketRepository.save(ticket); // commits immediately

        ragService.processTicketAsync(ticket.getId());

        log.info("Retrying ticket {}", id);
        return ticketMapper.toTicketDetailResponse(ticket);
    }

    /**
     * Records user feedback for a resolved ticket's AI response.
     *
     * @Transactional required — ticket.getAiResponse() is a LAZY load.
     * The open session allows Hibernate to fetch the aiResponse within this method.
     */
    @Transactional
    public FeedbackResponse addFeedback(UUID ticketId, FeedbackRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Ticket not found: " + ticketId));

        if (ticket.getAiResponse() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ticket " + ticketId + " has no AI response yet — it may not be resolved");
        }

        Feedback feedback = Feedback.builder()
                .aiResponse(ticket.getAiResponse())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        feedback = feedbackRepository.save(feedback);
        return feedbackMapper.toResponse(feedback);
    }
}
