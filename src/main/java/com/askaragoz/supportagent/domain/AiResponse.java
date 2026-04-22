package com.askaragoz.supportagent.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity storing the AI-generated response for a resolved support ticket.
 * Created by the async RAG pipeline after Claude returns an answer.
 */
@Entity
@Table(name = "ai_responses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The support ticket this response answers.
     *
     * @OneToOne  — each AI response belongs to exactly one ticket.
     * @JoinColumn — creates the ticket_id FK column in ai_responses.
     *               unique = true enforces the one-to-one constraint at the DB level.
     * fetch = LAZY — do not load SupportTicket automatically; only when explicitly accessed.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private SupportTicket ticket;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String responseText;

    /**
     * UUIDs of the KnowledgeArticle rows retrieved by the vector similarity search.
     * Stored as a PostgreSQL JSONB array: ["uuid1", "uuid2", ...]
     *
     * @JdbcTypeCode(SqlTypes.JSON) — Hibernate 6 built-in JSON support.
     *                                Serialises/deserialises via Jackson automatically.
     *                                No manual converter needed for JSON.
     * columnDefinition = "jsonb"   — uses PostgreSQL's binary JSON type, which is more
     *                                efficient to query and index than plain json.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<UUID> retrievedArticleIds;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
