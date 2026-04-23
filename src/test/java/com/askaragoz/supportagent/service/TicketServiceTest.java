package com.askaragoz.supportagent.service;

import com.askaragoz.supportagent.domain.AiResponse;
import com.askaragoz.supportagent.domain.Feedback;
import com.askaragoz.supportagent.domain.SupportTicket;
import com.askaragoz.supportagent.domain.TicketStatus;
import com.askaragoz.supportagent.dto.request.FeedbackRequest;
import com.askaragoz.supportagent.dto.request.TicketCreateRequest;
import com.askaragoz.supportagent.dto.response.FeedbackResponse;
import com.askaragoz.supportagent.dto.response.TicketDetailResponse;
import com.askaragoz.supportagent.mapper.FeedbackMapper;
import com.askaragoz.supportagent.mapper.TicketMapper;
import com.askaragoz.supportagent.repository.FeedbackRepository;
import com.askaragoz.supportagent.repository.SupportTicketRepository;
import com.askaragoz.supportagent.service.ai.RagService;
import com.askaragoz.supportagent.domain.FeedbackRating;
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

/**
 * Unit tests for TicketService.
 *
 * @ExtendWith(MockitoExtension.class) — activates Mockito for this test class.
 *   Mockito creates mock objects (fake implementations) for each @Mock field.
 *   This lets us test TicketService in complete isolation — no Spring context,
 *   no database, no real HTTP calls.
 *
 * @Mock — creates a mock (fake) bean. All methods return null/0/false by default.
 *   Use when(...).thenReturn(...) to define what a mock returns for a given input.
 *
 * @InjectMocks — creates a real TicketService instance and injects the @Mock
 *   fields into it via constructor injection. This is the class being tested.
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private SupportTicketRepository ticketRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private RagService ragService;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private FeedbackMapper feedbackMapper;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void createTicket_savesTicketWithPendingStatus_andTriggersAsync() {
        // Arrange — prepare inputs and define mock behaviour
        TicketCreateRequest request = new TicketCreateRequest();
        request.setUserEmail("user@example.com");
        request.setSubject("Login issue");
        request.setDescription("Cannot log in");

        SupportTicket savedTicket = SupportTicket.builder()
                .id(UUID.randomUUID())
                .userEmail("user@example.com")
                .subject("Login issue")
                .description("Cannot log in")
                .status(TicketStatus.PENDING)
                .build();

        TicketDetailResponse expectedResponse = new TicketDetailResponse();
        expectedResponse.setId(savedTicket.getId());
        expectedResponse.setStatus(TicketStatus.PENDING);

        // when(mock.method(args)).thenReturn(value) — define what the mock returns
        when(ticketRepository.save(any(SupportTicket.class))).thenReturn(savedTicket);
        when(ticketMapper.toTicketDetailResponse(savedTicket)).thenReturn(expectedResponse);

        // Act — call the method under test
        TicketDetailResponse result = ticketService.createTicket(request);

        // Assert — verify the outcome
        assertThat(result.getStatus()).isEqualTo(TicketStatus.PENDING);

        // verify() checks that the mock method was called with expected arguments
        verify(ticketRepository).save(any(SupportTicket.class));
        verify(ragService).processTicketAsync(savedTicket.getId());
    }

    @Test
    void getTicket_whenNotFound_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(ticketRepository.findById(id)).thenReturn(Optional.empty());

        // assertThatThrownBy — asserts that the lambda throws the expected exception
        assertThatThrownBy(() -> ticketService.getTicket(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Ticket not found");
    }

    @Test
    void retryTicket_whenNotFailed_throwsConflict() {
        UUID id = UUID.randomUUID();
        // Ticket is PENDING, not FAILED — retry should be rejected
        when(ticketRepository.existsByIdAndStatus(id, TicketStatus.FAILED)).thenReturn(false);

        assertThatThrownBy(() -> ticketService.retryTicket(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot be retried");
    }

    @Test
    void retryTicket_whenFailed_resetsStatusAndTriggersAsync() {
        UUID id = UUID.randomUUID();
        SupportTicket failedTicket = SupportTicket.builder()
                .id(id)
                .status(TicketStatus.FAILED)
                .errorMessage("Timeout")
                .build();

        SupportTicket savedTicket = SupportTicket.builder()
                .id(id)
                .status(TicketStatus.PENDING)
                .build();

        TicketDetailResponse expectedResponse = new TicketDetailResponse();
        expectedResponse.setStatus(TicketStatus.PENDING);

        when(ticketRepository.existsByIdAndStatus(id, TicketStatus.FAILED)).thenReturn(true);
        when(ticketRepository.findById(id)).thenReturn(Optional.of(failedTicket));
        when(ticketRepository.save(any())).thenReturn(savedTicket);
        when(ticketMapper.toTicketDetailResponse(savedTicket)).thenReturn(expectedResponse);

        TicketDetailResponse result = ticketService.retryTicket(id);

        assertThat(result.getStatus()).isEqualTo(TicketStatus.PENDING);
        verify(ragService).processTicketAsync(id);
    }

    @Test
    void addFeedback_whenNoAiResponse_throwsConflict() {
        UUID ticketId = UUID.randomUUID();
        SupportTicket ticket = SupportTicket.builder()
                .id(ticketId)
                .status(TicketStatus.IN_PROGRESS)
                .aiResponse(null) // no AI response yet
                .build();

        FeedbackRequest request = new FeedbackRequest();
        request.setRating(FeedbackRating.POSITIVE);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.addFeedback(ticketId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("no AI response");
    }

    @Test
    void addFeedback_whenResolved_savesFeedbackAndReturnsResponse() {
        UUID ticketId = UUID.randomUUID();
        AiResponse aiResponse = AiResponse.builder()
                .id(UUID.randomUUID())
                .responseText("Here is your answer")
                .build();

        SupportTicket ticket = SupportTicket.builder()
                .id(ticketId)
                .status(TicketStatus.RESOLVED)
                .aiResponse(aiResponse)
                .build();

        FeedbackRequest request = new FeedbackRequest();
        request.setRating(FeedbackRating.POSITIVE);
        request.setComment("Very helpful!");

        Feedback savedFeedback = Feedback.builder()
                .id(UUID.randomUUID())
                .aiResponse(aiResponse)
                .rating(FeedbackRating.POSITIVE)
                .comment("Very helpful!")
                .build();

        FeedbackResponse expectedResponse = new FeedbackResponse();
        expectedResponse.setRating(FeedbackRating.POSITIVE);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(savedFeedback);
        when(feedbackMapper.toResponse(savedFeedback)).thenReturn(expectedResponse);

        FeedbackResponse result = ticketService.addFeedback(ticketId, request);

        assertThat(result.getRating()).isEqualTo(FeedbackRating.POSITIVE);
        verify(feedbackRepository).save(any(Feedback.class));
    }
}
