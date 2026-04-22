package com.askaragoz.supportagent.service.ai;

import com.askaragoz.supportagent.domain.AiResponse;
import com.askaragoz.supportagent.domain.SupportTicket;
import com.askaragoz.supportagent.domain.TicketStatus;
import com.askaragoz.supportagent.repository.AiResponseRepository;
import com.askaragoz.supportagent.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Mock RAG implementation — active when app.ai.mock-enabled=true (the default).
 *
 * Runs the full async status-transition flow (PENDING → IN_PROGRESS → RESOLVED)
 * and persists a fake AiResponse, but skips all real AI calls:
 *   - No OpenAI embedding API call
 *   - No pgvector similarity search
 *   - No Claude generation call
 *
 * This lets the entire application run and be tested without any API keys.
 *
 * @ConditionalOnProperty — Spring only creates this bean when the property matches.
 *   matchIfMissing = true means this bean is also created if the property is absent,
 *   making mock mode the safe default.
 *
 * @RequiredArgsConstructor — Lombok generates a constructor for all final fields,
 *   which Spring uses for dependency injection (constructor injection).
 *
 * @Slf4j — Lombok injects a 'log' field (SLF4J Logger) for structured logging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ai.mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockRagService implements RagService {

    private final SupportTicketRepository ticketRepository;
    private final AiResponseRepository aiResponseRepository;

    /**
     * @Async — Spring runs this method on a separate virtual thread (configured in AsyncConfig).
     * The caller returns immediately with HTTP 202; the client polls for completion.
     *
     * IMPORTANT: @Async only works when called from a DIFFERENT bean.
     * If you call this method from within MockRagService itself, the proxy is bypassed
     * and the method runs synchronously. The TicketService (added in Phase 4) calls this.
     */
    @Async
    @Override
    public void processTicketAsync(UUID ticketId) {
        log.info("Mock RAG pipeline started for ticket {}", ticketId);
        try {
            // ── Step 1: Mark IN_PROGRESS ──────────────────────────────────────────
            // Each repository.save() is its own short transaction (Spring Data default).
            // Committing here makes IN_PROGRESS visible to the polling client immediately.
            SupportTicket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticket = ticketRepository.save(ticket);

            // ── Step 2: Simulate AI processing time ───────────────────────────────
            Thread.sleep(1500);

            // ── Step 3: Persist a mock AI response ───────────────────────────────
            AiResponse response = AiResponse.builder()
                    .ticket(ticket)
                    .responseText(buildMockResponse(ticket))
                    .retrievedArticleIds(List.of())
                    .build();
            aiResponseRepository.save(response);

            // ── Step 4: Mark RESOLVED ─────────────────────────────────────────────
            ticket.setStatus(TicketStatus.RESOLVED);
            ticketRepository.save(ticket);

            log.info("Mock RAG pipeline completed for ticket {}", ticketId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markFailed(ticketId, "Processing was interrupted");
        } catch (Exception e) {
            log.error("Mock RAG pipeline failed for ticket {}", ticketId, e);
            markFailed(ticketId, e.getMessage());
        }
    }

    private String buildMockResponse(SupportTicket ticket) {
        return String.format(
                "Thank you for reaching out to Nexus Platform support regarding \"%s\". " +
                "This is a simulated response — the application is running in mock mode. " +
                "To enable real AI responses, set app.ai.mock-enabled=false in " +
                "application-local.yml and provide valid API keys.",
                ticket.getSubject()
        );
    }

    private void markFailed(UUID ticketId, String errorMessage) {
        ticketRepository.findById(ticketId).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.FAILED);
            ticket.setErrorMessage(errorMessage);
            ticketRepository.save(ticket);
        });
    }
}
