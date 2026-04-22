package com.askaragoz.supportagent.service.ai;

import java.util.UUID;

/**
 * Contract for the Retrieval-Augmented Generation (RAG) pipeline.
 *
 * RAG is the AI pattern used by this application:
 *   1. Embed the user's question as a vector
 *   2. Search the knowledge base for semantically similar articles (Retrieval)
 *   3. Inject the retrieved articles into a prompt as context (Augmented)
 *   4. Send the prompt to Claude, which generates a grounded answer (Generation)
 *
 * Two implementations exist:
 *   - MockRagService  — active when app.ai.mock-enabled=true (default); no API calls needed
 *   - ClaudeRagService — active when app.ai.mock-enabled=false; requires real API keys
 *
 * The active implementation is selected at startup via @ConditionalOnProperty,
 * so only one bean of type RagService exists at runtime.
 */
public interface RagService {

    /**
     * Asynchronously processes a support ticket through the full RAG pipeline.
     *
     * This method is annotated @Async in each implementation, meaning it returns
     * immediately and runs on a separate virtual thread. The caller (TicketService)
     * does not wait for it to complete — the client polls GET /tickets/{id} instead.
     *
     * Status transitions driven by this method:
     *   PENDING → IN_PROGRESS → RESOLVED  (happy path)
     *   PENDING → IN_PROGRESS → FAILED    (on any exception)
     *
     * @param ticketId the ID of the SupportTicket to process
     */
    void processTicketAsync(UUID ticketId);
}
