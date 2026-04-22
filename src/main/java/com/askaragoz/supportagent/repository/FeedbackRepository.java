package com.askaragoz.supportagent.repository;

import com.askaragoz.supportagent.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for Feedback.
 * Standard JpaRepository CRUD operations are sufficient for this entity.
 */
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
}
