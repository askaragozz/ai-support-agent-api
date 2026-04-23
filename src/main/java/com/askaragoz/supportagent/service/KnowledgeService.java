package com.askaragoz.supportagent.service;

import com.askaragoz.supportagent.domain.KnowledgeArticle;
import com.askaragoz.supportagent.dto.request.KnowledgeArticleRequest;
import com.askaragoz.supportagent.dto.response.KnowledgeArticleResponse;
import com.askaragoz.supportagent.mapper.KnowledgeArticleMapper;
import com.askaragoz.supportagent.repository.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Business logic for knowledge base article CRUD operations.
 *
 * Note on embeddings: when an article is created or updated, the embedding column
 * is left null. In live mode (mock-enabled=false), ClaudeRagService will embed
 * the article on first use via the OpenAI API. In mock mode, embeddings are
 * never needed because MockRagService skips the pgvector search entirely.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeArticleRepository knowledgeRepository;
    private final KnowledgeArticleMapper mapper;

    @Transactional
    public KnowledgeArticleResponse createArticle(KnowledgeArticleRequest request) {
        KnowledgeArticle article = mapper.toEntity(request);
        article = knowledgeRepository.save(article);
        log.info("Created knowledge article {}: {}", article.getId(), article.getTitle());
        return mapper.toResponse(article);
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeArticleResponse> getArticles(Pageable pageable) {
        return knowledgeRepository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public KnowledgeArticleResponse getArticle(UUID id) {
        return knowledgeRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Knowledge article not found: " + id));
    }

    /**
     * Updates an existing article's title and content.
     * Clears the embedding so it will be re-generated on next use in live mode.
     */
    @Transactional
    public KnowledgeArticleResponse updateArticle(UUID id, KnowledgeArticleRequest request) {
        KnowledgeArticle article = knowledgeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Knowledge article not found: " + id));

        mapper.updateEntity(request, article);
        article.setEmbedding(null); // content changed — invalidate the old embedding
        article = knowledgeRepository.save(article);
        log.info("Updated knowledge article {}", id);
        return mapper.toResponse(article);
    }

    @Transactional
    public void deleteArticle(UUID id) {
        if (!knowledgeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Knowledge article not found: " + id);
        }
        knowledgeRepository.deleteById(id);
        log.info("Deleted knowledge article {}", id);
    }
}
