package com.askaragoz.supportagent.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity storing user feedback on an AI-generated response.
 * Created via POST /tickets/{id}/feedback after a ticket reaches RESOLVED status.
 */
@Entity
@Table(name = "feedback")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The AI response being rated.
     * @JoinColumn creates the ai_response_id FK column in the feedback table.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_response_id", nullable = false, unique = true)
    private AiResponse aiResponse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackRating rating;

    /** Optional free-text comment from the user explaining their rating. */
    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
