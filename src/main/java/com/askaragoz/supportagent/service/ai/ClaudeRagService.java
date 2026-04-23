package com.askaragoz.supportagent.service.ai;

import com.askaragoz.supportagent.domain.AiResponse;
import com.askaragoz.supportagent.domain.KnowledgeArticle;
import com.askaragoz.supportagent.domain.SupportTicket;
import com.askaragoz.supportagent.domain.TicketStatus;
import com.askaragoz.supportagent.domain.converter.FloatArrayToVectorConverter;
import com.askaragoz.supportagent.repository.AiResponseRepository;
import com.askaragoz.supportagent.repository.KnowledgeArticleRepository;
import com.askaragoz.supportagent.repository.SupportTicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Real RAG implementation using Claude (generation) and OpenAI (embeddings).
 * Active only when app.ai.mock-enabled=false in application-local.yml.
 *
 * Full pipeline:
 *   1. Embed the ticket description via OpenAI text-embedding-3-small (1536 dims)
 *   2. Find the top-K most similar knowledge articles via pgvector cosine search
 *   3. Build a prompt that injects those articles as context
 *   4. Send the prompt to Claude claude-haiku-4-5-20251001 and capture the response
 *   5. Persist the AiResponse and update the ticket status to RESOLVED
 *
 * DEPENDENCY NOTES:
 * ChatClient.Builder — auto-configured by spring-ai-starter-model-anthropic.
 *   Reads spring.ai.anthropic.api-key and model options from application.yml.
 *   We call .build() once in the constructor to create a reusable ChatClient.
 *
 * EmbeddingModel — auto-configured by spring-ai-starter-model-openai.
 *   Reads spring.ai.openai.api-key and embedding model from application.yml.
 *   Used ONLY for embedding; all generation is handled by Claude.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.ai.mock-enabled", havingValue = "false")
public class ClaudeRagService implements RagService {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final SupportTicketRepository ticketRepository;
    private final KnowledgeArticleRepository knowledgeRepository;
    private final AiResponseRepository aiResponseRepository;
    private final FloatArrayToVectorConverter vectorConverter;
    private final int topK;

    /**
     * Constructor injection — all dependencies are declared as constructor parameters.
     * Spring resolves and injects them automatically at startup.
     *
     * @Value("${app.rag.top-k:5}") — reads the property from application.yml.
     * The ':5' is a default value if the property is absent.
     *
     * ChatClient.Builder is injected (not ChatClient directly) so we can call
     * .build() here and hold a single reusable instance.
     */
    public ClaudeRagService(
            ChatClient.Builder chatClientBuilder,
            EmbeddingModel embeddingModel,
            SupportTicketRepository ticketRepository,
            KnowledgeArticleRepository knowledgeRepository,
            AiResponseRepository aiResponseRepository,
            @Value("${app.rag.top-k:5}") int topK) {

        this.chatClient = chatClientBuilder.build();
        this.embeddingModel = embeddingModel;
        this.ticketRepository = ticketRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.aiResponseRepository = aiResponseRepository;
        this.vectorConverter = new FloatArrayToVectorConverter();
        this.topK = topK;
    }

    @Async
    @Override
    public void processTicketAsync(UUID ticketId) {
        log.info("Claude RAG pipeline started for ticket {}", ticketId);
        try {
            // ── Step 1: Mark IN_PROGRESS ──────────────────────────────────────────
            SupportTicket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticket = ticketRepository.save(ticket);

            // ── Step 2: Embed the ticket description ──────────────────────────────
            // embeddingModel.embed() calls OpenAI's text-embedding-3-small API
            // and returns a float[] of 1536 dimensions.
            float[] embedding = embeddingModel.embed(ticket.getDescription());

            // Convert float[] to pgvector string format "[v1,v2,...]" for the native query.
            String vectorString = vectorConverter.convertToDatabaseColumn(embedding);

            // ── Step 3: Retrieve similar knowledge articles ───────────────────────
            // Uses the <=> cosine distance operator via the native pgvector query
            // in KnowledgeArticleRepository. Returns the top-K closest articles.
            List<KnowledgeArticle> articles = knowledgeRepository.findTopKBySimilarity(vectorString, topK);
            log.debug("Retrieved {} knowledge articles for ticket {}", articles.size(), ticketId);

            // ── Step 4: Call Claude ───────────────────────────────────────────────
            // chatClient.prompt().user(...).call().content() sends the prompt to
            // Claude and returns the generated text as a plain String.
            String responseText = chatClient.prompt()
                    .user(buildPrompt(ticket, articles))
                    .call()
                    .content();

            // ── Step 5: Persist the AI response ──────────────────────────────────
            List<UUID> articleIds = articles.stream()
                    .map(KnowledgeArticle::getId)
                    .toList();

            AiResponse response = AiResponse.builder()
                    .ticket(ticket)
                    .responseText(responseText)
                    .retrievedArticleIds(articleIds)
                    .build();
            aiResponseRepository.save(response);

            // ── Step 6: Mark RESOLVED ─────────────────────────────────────────────
            // Must link aiResponse before saving — same orphanRemoval reason as MockRagService.
            ticket.setAiResponse(response);
            ticket.setStatus(TicketStatus.RESOLVED);
            ticketRepository.save(ticket);

            log.info("Claude RAG pipeline completed for ticket {}", ticketId);

        } catch (Exception e) {
            log.error("Claude RAG pipeline failed for ticket {}", ticketId, e);
            markFailed(ticketId, e.getMessage());
        }
    }

    /**
     * Builds the RAG prompt by injecting retrieved articles as context.
     *
     * The prompt follows the standard RAG pattern:
     *   [System instruction] + [Retrieved context] + [User question]
     *
     * Grounding Claude's response in the knowledge base articles reduces hallucination
     * and ensures answers are consistent with the actual product documentation.
     */
    private String buildPrompt(SupportTicket ticket, List<KnowledgeArticle> articles) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful customer support agent for Nexus Platform.\n");
        sb.append("Answer the customer's question using the knowledge base articles provided below.\n");
        sb.append("Be concise, friendly, and actionable. ");
        sb.append("If the articles do not contain relevant information, say so honestly and suggest contacting support.\n\n");

        if (!articles.isEmpty()) {
            sb.append("Knowledge Base Articles:\n");
            for (int i = 0; i < articles.size(); i++) {
                KnowledgeArticle article = articles.get(i);
                sb.append("---\n");
                sb.append("Article ").append(i + 1).append(": ").append(article.getTitle()).append("\n");
                sb.append(article.getContent()).append("\n");
            }
            sb.append("---\n\n");
        }

        sb.append("Customer Support Request:\n");
        sb.append("Subject: ").append(ticket.getSubject()).append("\n");
        sb.append("Description: ").append(ticket.getDescription()).append("\n\n");
        sb.append("Please provide a helpful response.");

        return sb.toString();
    }

    private void markFailed(UUID ticketId, String errorMessage) {
        ticketRepository.findById(ticketId).ifPresent(ticket -> {
            ticket.setStatus(TicketStatus.FAILED);
            ticket.setErrorMessage(errorMessage != null ? errorMessage : "Unknown error during AI processing");
            ticketRepository.save(ticket);
        });
    }
}
