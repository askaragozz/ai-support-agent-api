package com.askaragoz.supportagent.controller;

import com.askaragoz.supportagent.dto.request.KnowledgeArticleRequest;
import com.askaragoz.supportagent.dto.response.KnowledgeArticleResponse;
import com.askaragoz.supportagent.service.KnowledgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for knowledge base article CRUD operations.
 */
@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgeArticleResponse createArticle(@RequestBody @Valid KnowledgeArticleRequest request) {
        return knowledgeService.createArticle(request);
    }

    @GetMapping
    public Page<KnowledgeArticleResponse> getArticles(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return knowledgeService.getArticles(pageable);
    }

    @GetMapping("/{id}")
    public KnowledgeArticleResponse getArticle(@PathVariable UUID id) {
        return knowledgeService.getArticle(id);
    }

    /**
     * Updates an existing article's title and content.
     * The embedding is cleared and will be re-generated on next use in live mode.
     */
    @PutMapping("/{id}")
    public KnowledgeArticleResponse updateArticle(
            @PathVariable UUID id,
            @RequestBody @Valid KnowledgeArticleRequest request) {
        return knowledgeService.updateArticle(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArticle(@PathVariable UUID id) {
        knowledgeService.deleteArticle(id);
    }
}
