package com.askaragoz.supportagent.controller;

import com.askaragoz.supportagent.domain.TicketStatus;
import com.askaragoz.supportagent.dto.request.TicketCreateRequest;
import com.askaragoz.supportagent.dto.response.TicketDetailResponse;
import com.askaragoz.supportagent.repository.AiResponseRepository;
import com.askaragoz.supportagent.repository.FeedbackRepository;
import com.askaragoz.supportagent.repository.SupportTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TicketController.
 *
 * @SpringBootTest(RANDOM_PORT) — starts the full Spring application context on a random
 *   HTTP port. This tests the entire stack: HTTP → Controller → Service → Repository → DB.
 *   Unlike @WebMvcTest (which only loads the web layer), this test hits a real database.
 *
 * @Testcontainers — activates the Testcontainers JUnit 5 extension.
 *   It manages the lifecycle of @Container fields: starts before the first test,
 *   stops after the last test.
 *
 * @Container — declares the PostgreSQL Docker container.
 *   static: one container instance is shared across all tests in this class (faster).
 *
 * @ServiceConnection — Spring Boot 3.1+ automatically reads the container's JDBC URL,
 *   username, and password and overwrites the datasource configuration from application.yml.
 *   No manual @DynamicPropertySource needed.
 *
 * pgvector/pgvector:pg16 image: required for Flyway V5 which creates the vector extension.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TicketControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SupportTicketRepository ticketRepository;

    @Autowired
    private AiResponseRepository aiResponseRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    /** Clean up all ticket-related data before each test to ensure test isolation. */
    @BeforeEach
    void cleanUp() {
        feedbackRepository.deleteAll();
        aiResponseRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    @Test
    void createTicket_returns202AndPendingStatus() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setUserEmail("user@example.com");
        request.setSubject("Cannot log in");
        request.setDescription("I get a 401 error when I try to log in.");

        ResponseEntity<TicketDetailResponse> response = restTemplate.postForEntity(
                "/api/v1/tickets", request, TicketDetailResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(TicketStatus.PENDING);
        assertThat(response.getBody().getId()).isNotNull();
    }

    @Test
    void createTicket_withInvalidEmail_returns400() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setUserEmail("not-an-email");
        request.setSubject("Subject");
        request.setDescription("Description");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/tickets", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getTicket_afterCreation_returnsTicket() {
        // Create a ticket first
        TicketCreateRequest request = new TicketCreateRequest();
        request.setUserEmail("user@example.com");
        request.setSubject("Billing question");
        request.setDescription("What plan am I on?");

        TicketDetailResponse created = restTemplate.postForObject(
                "/api/v1/tickets", request, TicketDetailResponse.class);

        // Fetch it by ID
        ResponseEntity<TicketDetailResponse> response = restTemplate.getForEntity(
                "/api/v1/tickets/" + created.getId(), TicketDetailResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(created.getId());
        assertThat(response.getBody().getSubject()).isEqualTo("Billing question");
    }

    @Test
    void getTicket_whenNotFound_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/tickets/00000000-0000-0000-0000-000000000000", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createTicket_asyncProcessing_eventuallyResolvesTicket() throws InterruptedException {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setUserEmail("async@example.com");
        request.setSubject("Async test");
        request.setDescription("Testing that MockRagService resolves the ticket.");

        TicketDetailResponse created = restTemplate.postForObject(
                "/api/v1/tickets", request, TicketDetailResponse.class);

        // MockRagService sleeps for 1500ms then resolves the ticket.
        // Poll with a timeout to wait for processing to complete.
        TicketDetailResponse polled = null;
        for (int i = 0; i < 10; i++) {
            Thread.sleep(500);
            polled = restTemplate.getForObject(
                    "/api/v1/tickets/" + created.getId(), TicketDetailResponse.class);
            if (polled.getStatus() == TicketStatus.RESOLVED || polled.getStatus() == TicketStatus.FAILED) {
                break;
            }
        }

        assertThat(polled).isNotNull();
        assertThat(polled.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(polled.getAiResponse()).isNotNull();
        assertThat(polled.getAiResponse().getResponseText()).isNotBlank();
    }

    @Test
    void retryTicket_whenNotFailed_returns409() {
        // Create a ticket (starts as PENDING)
        TicketCreateRequest request = new TicketCreateRequest();
        request.setUserEmail("retry@example.com");
        request.setSubject("Retry test");
        request.setDescription("This ticket is PENDING, not FAILED.");

        TicketDetailResponse created = restTemplate.postForObject(
                "/api/v1/tickets", request, TicketDetailResponse.class);

        // Immediately retry — ticket is PENDING, not FAILED → should return 409
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/tickets/" + created.getId() + "/retry", null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
