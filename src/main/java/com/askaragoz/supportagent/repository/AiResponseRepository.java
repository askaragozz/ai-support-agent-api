package com.askaragoz.supportagent.repository;

import com.askaragoz.supportagent.domain.AiResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for AiResponse.
 * Standard JpaRepository CRUD operations are sufficient for this entity.
 */
public interface AiResponseRepository extends JpaRepository<AiResponse, UUID> {
}
