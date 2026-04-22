package com.askaragoz.supportagent.repository;

import com.askaragoz.supportagent.domain.SupportTicket;
import com.askaragoz.supportagent.domain.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data repository for SupportTicket.
 *
 * Extending JpaRepository<SupportTicket, UUID> gives us all standard CRUD operations for free:
 *   save(), findById(), findAll(), delete(), count(), existsById(), etc.
 * Spring Data generates the SQL implementations at startup — we only write the interface.
 */
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    /**
     * Returns a paginated list of tickets belonging to a specific user.
     *
     * @EntityGraph  — overrides the LAZY fetch strategy for the aiResponse field on this query only.
     *                 Hibernate issues one SQL JOIN instead of N separate queries for N tickets.
     *
     *                 Without @EntityGraph (N+1 problem):
     *                   Query 1: SELECT * FROM support_tickets WHERE user_email = ?  → 20 rows
     *                   Query 2..21: SELECT * FROM ai_responses WHERE ticket_id = ?  (20 extra hits)
     *
     *                 With @EntityGraph (single query):
     *                   SELECT t.*, a.* FROM support_tickets t
     *                   LEFT JOIN ai_responses a ON a.ticket_id = t.id
     *                   WHERE t.user_email = ?
     *
     * Method name derivation — Spring Data reads the method name and generates the WHERE clause:
     *   findBy + UserEmail → WHERE user_email = ?
     * The Pageable parameter automatically adds ORDER BY, LIMIT, and OFFSET.
     */
    @EntityGraph(attributePaths = {"aiResponse"})
    Page<SupportTicket> findByUserEmail(String userEmail, Pageable pageable);

    /**
     * Checks whether a ticket with the given ID exists in a specific status.
     * Used by the retry endpoint to confirm the ticket is FAILED before retrying.
     *
     * existsBy + Id + And + Status → SELECT COUNT(*) > 0 WHERE id = ? AND status = ?
     */
    boolean existsByIdAndStatus(UUID id, TicketStatus status);
}
