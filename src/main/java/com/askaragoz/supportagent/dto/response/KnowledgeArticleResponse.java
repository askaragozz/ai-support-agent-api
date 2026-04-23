package com.askaragoz.supportagent.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response body for all knowledge base endpoints.
 * The 'embedding' field is intentionally excluded — it is an internal implementation
 * detail (1536 floats) that has no value to API callers.
 */
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeArticleResponse {

    private UUID id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
