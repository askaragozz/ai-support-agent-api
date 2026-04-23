package com.askaragoz.supportagent.controller;

import com.askaragoz.supportagent.dto.request.KnowledgeArticleRequest;
import com.askaragoz.supportagent.dto.response.KnowledgeArticleResponse;
import com.askaragoz.supportagent.repository.KnowledgeArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for KnowledgeController.
 * Uses the same Testcontainers setup as TicketControllerIntegrationTest.
 *
 * Note: V6 seed data (6 articles) is present in the test DB after Flyway runs.
 * Tests that create articles should be aware of this pre-existing data.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KnowledgeControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private KnowledgeArticleRepository knowledgeRepository;

    @Test
    void createArticle_returns201WithArticle() {
        KnowledgeArticleRequest request = new KnowledgeArticleRequest();
        request.setTitle("Test Article");
        request.setContent("This is test content for the knowledge base.");

        ResponseEntity<KnowledgeArticleResponse> response = restTemplate.postForEntity(
                "/api/v1/knowledge", request, KnowledgeArticleResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Test Article");
    }

    @Test
    void createArticle_withBlankTitle_returns400() {
        KnowledgeArticleRequest request = new KnowledgeArticleRequest();
        request.setTitle("");
        request.setContent("Content");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/knowledge", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getArticle_whenNotFound_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/knowledge/00000000-0000-0000-0000-000000000000", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateArticle_updatesContentAndClearsEmbedding() {
        // Create an article
        KnowledgeArticleRequest createRequest = new KnowledgeArticleRequest();
        createRequest.setTitle("Original Title");
        createRequest.setContent("Original content");

        KnowledgeArticleResponse created = restTemplate.postForObject(
                "/api/v1/knowledge", createRequest, KnowledgeArticleResponse.class);

        // Update it
        KnowledgeArticleRequest updateRequest = new KnowledgeArticleRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setContent("Updated content");

        ResponseEntity<KnowledgeArticleResponse> response = restTemplate.exchange(
                "/api/v1/knowledge/" + created.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                KnowledgeArticleResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTitle()).isEqualTo("Updated Title");

        // Verify embedding was cleared in DB
        knowledgeRepository.findById(created.getId()).ifPresent(article ->
                assertThat(article.getEmbedding()).isNull());
    }

    @Test
    void deleteArticle_returns204AndRemovesArticle() {
        // Create an article to delete
        KnowledgeArticleRequest request = new KnowledgeArticleRequest();
        request.setTitle("Article to Delete");
        request.setContent("This will be deleted.");

        KnowledgeArticleResponse created = restTemplate.postForObject(
                "/api/v1/knowledge", request, KnowledgeArticleResponse.class);

        // Delete it
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/v1/knowledge/" + created.getId(),
                HttpMethod.DELETE,
                null,
                Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify it no longer exists
        assertThat(knowledgeRepository.existsById(created.getId())).isFalse();
    }
}
