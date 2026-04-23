package com.askaragoz.supportagent.service;

import com.askaragoz.supportagent.domain.KnowledgeArticle;
import com.askaragoz.supportagent.dto.request.KnowledgeArticleRequest;
import com.askaragoz.supportagent.dto.response.KnowledgeArticleResponse;
import com.askaragoz.supportagent.mapper.KnowledgeArticleMapper;
import com.askaragoz.supportagent.repository.KnowledgeArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeArticleRepository knowledgeRepository;

    @Mock
    private KnowledgeArticleMapper mapper;

    @InjectMocks
    private KnowledgeService knowledgeService;

    @Test
    void createArticle_mapsRequestSavesAndReturnsResponse() {
        KnowledgeArticleRequest request = new KnowledgeArticleRequest();
        request.setTitle("How to reset password");
        request.setContent("Go to the login page...");

        KnowledgeArticle entity = KnowledgeArticle.builder()
                .id(UUID.randomUUID())
                .title("How to reset password")
                .content("Go to the login page...")
                .build();

        KnowledgeArticleResponse response = new KnowledgeArticleResponse();
        response.setTitle("How to reset password");

        when(mapper.toEntity(request)).thenReturn(entity);
        when(knowledgeRepository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(response);

        KnowledgeArticleResponse result = knowledgeService.createArticle(request);

        assertThat(result.getTitle()).isEqualTo("How to reset password");
        verify(knowledgeRepository).save(entity);
    }

    @Test
    void getArticle_whenNotFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(knowledgeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeService.getArticle(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Knowledge article not found");
    }

    @Test
    void updateArticle_whenNotFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(knowledgeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeService.updateArticle(id, new KnowledgeArticleRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Knowledge article not found");
    }

    @Test
    void updateArticle_clearsEmbeddingOnSave() {
        UUID id = UUID.randomUUID();
        KnowledgeArticle existing = KnowledgeArticle.builder()
                .id(id)
                .title("Old title")
                .content("Old content")
                .embedding(new float[]{0.1f, 0.2f}) // has an existing embedding
                .build();

        KnowledgeArticleRequest request = new KnowledgeArticleRequest();
        request.setTitle("New title");
        request.setContent("New content");

        KnowledgeArticleResponse response = new KnowledgeArticleResponse();
        response.setTitle("New title");

        when(knowledgeRepository.findById(id)).thenReturn(Optional.of(existing));
        when(knowledgeRepository.save(any())).thenReturn(existing);
        when(mapper.toResponse(any())).thenReturn(response);

        knowledgeService.updateArticle(id, request);

        // Verify the embedding was cleared before saving
        assertThat(existing.getEmbedding()).isNull();
        verify(knowledgeRepository).save(existing);
    }

    @Test
    void deleteArticle_whenNotFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(knowledgeRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> knowledgeService.deleteArticle(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Knowledge article not found");
    }

    @Test
    void deleteArticle_whenFound_deletesById() {
        UUID id = UUID.randomUUID();
        when(knowledgeRepository.existsById(id)).thenReturn(true);

        knowledgeService.deleteArticle(id);

        verify(knowledgeRepository).deleteById(id);
    }
}
