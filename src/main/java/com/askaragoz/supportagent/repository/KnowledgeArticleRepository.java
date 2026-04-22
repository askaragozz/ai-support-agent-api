package com.askaragoz.supportagent.repository;

import com.askaragoz.supportagent.domain.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for KnowledgeArticle.
 * Extends the standard CRUD operations with a pgvector similarity search query.
 */
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, UUID> {

    /**
     * Finds the K most semantically similar articles to a given embedding vector.
     * This is the core retrieval step of the RAG pipeline.
     *
     * WHY nativeQuery = true:
     * JPQL (JPA's standard query language) does not support pgvector's <=> operator.
     * We must drop down to raw PostgreSQL SQL for vector operations.
     *
     * <=>                        — pgvector's cosine distance operator. Lower = more similar.
     * CAST(:embedding AS vector) — casts the string "[v1,v2,...]" to the vector column type.
     * WHERE embedding IS NOT NULL — skips seed articles that haven't been embedded yet.
     * ORDER BY ... LIMIT          — returns only the top K closest articles.
     *
     * @param embedding  The query vector as a pgvector string: "[0.12, -0.34, ...]"
     * @param limit      How many articles to return (controlled by app.rag.top-k in yml).
     */
    @Query(value = """
            SELECT * FROM knowledge_articles
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<KnowledgeArticle> findTopKBySimilarity(@Param("embedding") String embedding,
                                                 @Param("limit") int limit);
}
