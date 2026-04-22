package com.askaragoz.supportagent.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a customer support ticket.
 *
 * @Entity     — marks this class as Hibernate-managed; maps to the support_tickets table.
 * @Table      — explicitly names the table so the mapping is obvious at a glance.
 * @Getter/@Setter — Lombok generates getters and setters for every field at compile time.
 * @Builder    — Lombok generates a fluent builder:
 *               SupportTicket.builder().userEmail("x").subject("y").build()
 * @NoArgsConstructor — JPA requires a no-arg constructor to instantiate entities via reflection.
 * @AllArgsConstructor — required alongside @Builder when @NoArgsConstructor is also present.
 */
@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

    /**
     * @Id                   — marks this field as the primary key.
     * @GeneratedValue(UUID) — Hibernate generates a random UUID for new records.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Email of the customer who submitted the ticket. Used to scope GET /tickets?email= queries. */
    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String subject;

    /**
     * columnDefinition = "TEXT" — instructs Hibernate to use PostgreSQL's unbounded TEXT type
     * rather than the default VARCHAR(255). Use TEXT for any field without a known length limit.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * @Enumerated(EnumType.STRING) — stores the enum as its name ("PENDING", "RESOLVED", etc.)
     * rather than its ordinal integer (0, 1, 2...).
     * Always use STRING — ordinal values break silently if enum constants are ever reordered.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    /**
     * Populated only when status = FAILED.
     * Stores a human-readable message to help diagnose what went wrong during AI processing.
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Bidirectional inverse side of the AiResponse → SupportTicket relationship.
     *
     * mappedBy = "ticket"  — AiResponse owns the FK column (ticket_id); this side is read-only.
     * fetch = LAZY         — Hibernate does NOT load AiResponse when loading a ticket.
     *                        Prevents N+1: loading 20 tickets won't trigger 20 extra queries.
     *                        Use @EntityGraph on the repository to load it only when needed.
     * cascade = ALL        — persist/delete operations on SupportTicket cascade to AiResponse.
     * orphanRemoval = true — if aiResponse is set to null, the orphaned row is deleted.
     */
    @OneToOne(mappedBy = "ticket", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private AiResponse aiResponse;

    /**
     * @CreationTimestamp — Hibernate sets this automatically on first INSERT. Never updated.
     * updatable = false  — prevents accidental overwrites on subsequent saves.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** @UpdateTimestamp — Hibernate refreshes this on every INSERT and UPDATE. */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
