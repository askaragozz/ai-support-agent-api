# AI Support Agent API

A production-grade AI-powered customer support API built with **Spring Boot 3**, **Spring AI**, and **Retrieval-Augmented Generation (RAG)**. Customers submit tickets via REST; an async pipeline retrieves semantically similar knowledge base articles using pgvector and generates a grounded response with Claude.

> **Mock mode is on by default** — the full async pipeline runs without any API keys. Set `APP_AI_MOCK_ENABLED=false` and supply your keys when you are ready to use real AI.

---

## How It Works

```
POST /api/v1/tickets  →  202 Accepted  (ticket saved, async pipeline starts)
                                │
                    ┌───────────▼─────────────────────────────┐
                    │        Async RAG Pipeline (virtual thread)│
                    │                                          │
                    │  1. Embed ticket via OpenAI              │
                    │     text-embedding-3-small (1536 dims)   │
                    │                                          │
                    │  2. pgvector cosine search               │
                    │     → top-5 knowledge base articles      │
                    │                                          │
                    │  3. Claude claude-haiku-4-5-20251001     │
                    │     generates a grounded response        │
                    │                                          │
                    │  4. Ticket status: PENDING               │
                    │                 → IN_PROGRESS            │
                    │                 → RESOLVED (or FAILED)   │
                    └──────────────────────────────────────────┘

GET /api/v1/tickets/{id}  ←  client polls until RESOLVED
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (virtual threads via Project Loom) |
| Framework | Spring Boot 3.4.1 |
| AI / LLM | Spring AI 1.0.0 — Claude for generation, OpenAI for embeddings |
| Vector search | PostgreSQL + pgvector, cosine similarity (`<=>`) |
| ORM | Hibernate 6, Spring Data JPA, Flyway migrations |
| Mapping | MapStruct 1.6.3 (compile-time, zero reflection) |
| API docs | SpringDoc OpenAPI 3 / Swagger UI |
| Monitoring | Spring Boot Admin 3.3.5 |
| Testing | JUnit 5, Mockito, Testcontainers (real PostgreSQL) |
| Containerisation | Docker multi-stage build, Docker Compose |
| Deployment | Railway |

---

## API Reference

Interactive docs are available at **`/swagger-ui.html`** once the app is running.

### Tickets

| Method | Path | Body | Response | Status |
|---|---|---|---|---|
| `POST` | `/api/v1/tickets` | `TicketCreateRequest` | `TicketDetailResponse` | 202 |
| `GET` | `/api/v1/tickets?email=&page=&size=` | — | `Page<TicketResponse>` | 200 |
| `GET` | `/api/v1/tickets/{id}` | — | `TicketDetailResponse` | 200 |
| `POST` | `/api/v1/tickets/{id}/retry` | — | `TicketDetailResponse` | 202 |
| `POST` | `/api/v1/tickets/{id}/feedback` | `FeedbackRequest` | `FeedbackResponse` | 201 |

### Knowledge Base

| Method | Path | Body | Response | Status |
|---|---|---|---|---|
| `POST` | `/api/v1/knowledge` | `KnowledgeArticleRequest` | `KnowledgeArticleResponse` | 201 |
| `GET` | `/api/v1/knowledge?page=&size=` | — | `Page<KnowledgeArticleResponse>` | 200 |
| `GET` | `/api/v1/knowledge/{id}` | — | `KnowledgeArticleResponse` | 200 |
| `PUT` | `/api/v1/knowledge/{id}` | `KnowledgeArticleRequest` | `KnowledgeArticleResponse` | 200 |
| `DELETE` | `/api/v1/knowledge/{id}` | — | — | 204 |

All errors are returned as [RFC 7807 `ProblemDetail`](https://www.rfc-editor.org/rfc/rfc7807) JSON.

---

## Getting Started

### Prerequisites

- Java 21 ([Eclipse Temurin](https://adoptium.net))
- Maven 3.9+
- Docker Desktop

### 1. Clone and start the infrastructure

```bash
git clone https://github.com/askaragozz/ai-support-agent-api.git
cd ai-support-agent-api

# Start PostgreSQL (port 5433) and Spring Boot Admin (port 8090)
docker compose up postgres admin-server
```

### 2. Run the application

```bash
mvn spring-boot:run
```

The app starts on **http://localhost:8080** with mock AI mode active.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health
- Admin dashboard: http://localhost:8090 (user: `admin`, password: `changeme`)

### 3. Try it out

```bash
# Submit a support ticket
curl -s -X POST http://localhost:8080/api/v1/tickets \
  -H "Content-Type: application/json" \
  -d '{"userEmail":"user@example.com","subject":"Cannot log in","description":"I get a 401 when I try to log in."}' \
  | jq .

# Poll until RESOLVED (MockRagService resolves in ~1.5 s)
curl -s http://localhost:8080/api/v1/tickets/{id} | jq .
```

---

## Running with Real AI

1. Copy `.env.example` to `.env` and fill in your API keys:
   ```
   APP_AI_MOCK_ENABLED=false
   ANTHROPIC_API_KEY=sk-ant-...
   OPENAI_API_KEY=sk-...
   ```
2. Load the `.env` file and restart:
   ```bash
   # On Linux / macOS
   export $(cat .env | xargs) && mvn spring-boot:run

   # Or use IntelliJ's run configuration "EnvFile" plugin
   ```

Get your keys at:
- Anthropic: https://console.anthropic.com
- OpenAI: https://platform.openai.com/api-keys

---

## Running the Full Stack in Docker

```bash
docker compose up --build
```

Builds the app from the local `Dockerfile` and starts all three services. Identical to what runs in production.

---

## Running Tests

```bash
mvn test
```

Integration tests spin up a real PostgreSQL container via Testcontainers (`pgvector/pgvector:pg16`), run all Flyway migrations, and test the full HTTP stack with `@SpringBootTest`. No mocking of the database.

```
Tests run: 23
  ├── TicketServiceTest           (6 unit tests — Mockito)
  ├── KnowledgeServiceTest        (6 unit tests — Mockito)
  ├── TicketControllerIntegrationTest   (6 integration tests — Testcontainers)
  └── KnowledgeControllerIntegrationTest (5 integration tests — Testcontainers)
```

To generate a JaCoCo coverage report:

```bash
mvn test
open target/site/jacoco/index.html
```

---

## Deployment on Railway

1. Push this repository to GitHub.
2. Create a new project on [Railway](https://railway.app) and connect the repository.
3. Add a **PostgreSQL** service from the Railway dashboard. Copy the connection variables (`PGHOST`, `PGPORT`, etc.) into your app service as `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.
4. Set the remaining environment variables from `.env.example`.
5. Deploy — Railway builds the `Dockerfile` and polls `/actuator/health` until Flyway finishes.

The `railway.toml` in the repository root handles the rest.

---

## Project Structure

```
src/main/java/com/askaragoz/supportagent/
├── config/
│   └── AsyncConfig.java            # Virtual-thread executor for @Async
├── controller/
│   ├── TicketController.java
│   └── KnowledgeController.java
├── domain/
│   ├── SupportTicket.java          # JPA entity, PENDING→RESOLVED state machine
│   ├── KnowledgeArticle.java       # Entity with vector(1536) embedding column
│   ├── AiResponse.java             # @OneToOne with SupportTicket, JSONB article IDs
│   ├── Feedback.java               # @OneToOne with AiResponse
│   └── converter/
│       └── FloatArrayToVectorConverter.java  # float[] ↔ pgvector string format
├── dto/
│   ├── request/                    # TicketCreateRequest, FeedbackRequest, …
│   └── response/                   # TicketDetailResponse, AiResponseDto, …
├── exception/
│   └── GlobalExceptionHandler.java # RFC 7807 ProblemDetail for all errors
├── mapper/
│   ├── TicketMapper.java           # MapStruct entity ↔ DTO (compile-time)
│   ├── KnowledgeArticleMapper.java
│   └── FeedbackMapper.java
├── repository/
│   ├── SupportTicketRepository.java  # @EntityGraph to prevent N+1
│   ├── KnowledgeArticleRepository.java  # native pgvector similarity query
│   ├── AiResponseRepository.java
│   └── FeedbackRepository.java
└── service/
    ├── TicketService.java
    ├── KnowledgeService.java
    └── ai/
        ├── RagService.java          # interface — strategy pattern
        ├── MockRagService.java      # active when APP_AI_MOCK_ENABLED=true
        └── ClaudeRagService.java    # active when APP_AI_MOCK_ENABLED=false

src/main/resources/
├── application.yml
└── db/migration/
    ├── V1__create_support_tickets.sql
    ├── V2__create_knowledge_articles.sql
    ├── V3__create_ai_responses.sql
    ├── V4__create_feedback.sql
    ├── V5__add_pgvector_extension_and_embedding.sql
    └── V6__seed_knowledge_articles.sql   # 6 seed articles for the Nexus Platform SaaS
```

---

## Key Design Decisions

### Async + polling instead of WebSockets or SSE
`POST /tickets` returns `202 Accepted` immediately so the HTTP connection is never held open for the duration of an AI call (which can take several seconds). The client polls `GET /tickets/{id}`. This is simpler to implement, easier to cache, and requires no special infrastructure.

### `@Async` must be on a separate bean
Spring AOP proxies work by intercepting calls from *outside* a bean. If the method calling `processTicketAsync()` and the method *being* `@Async` were in the same class, the call would bypass the proxy and run synchronously. `MockRagService` and `ClaudeRagService` are separate `@Service` beans called by `TicketService` to ensure the proxy intercept fires.

### No Open Session in View (`open-in-view: false`)
The OSIV pattern keeps a Hibernate session open for the entire duration of an HTTP request, which means lazy associations can be loaded anywhere — including in view templates or Jackson serialisers — silently triggering extra DB queries. With OSIV disabled, every DB access must happen inside an explicit `@Transactional` boundary in the service layer. This is harder to get wrong and prevents N+1 surprises in production.

### `@EntityGraph` on list queries
`SupportTicket.aiResponse` is `FetchType.LAZY`. Without a hint, loading 20 tickets would trigger 20 extra queries to load each `AiResponse` (the N+1 problem). `@EntityGraph(attributePaths = {"aiResponse"})` on `findByUserEmail` issues a single LEFT JOIN instead.

### pgvector null embedding with `@ColumnTransformer`
The `FloatArrayToVectorConverter` outputs `String` as its JDBC type. When the embedding is `null`, Hibernate binds a `null::varchar` parameter. PostgreSQL has no implicit cast from `varchar` to `vector` and rejects it. Adding `@ColumnTransformer(write = "?::vector")` wraps every INSERT/UPDATE parameter in an explicit cast, making null safe (`NULL::vector` is accepted for any column type).

### `orphanRemoval` and bidirectional association maintenance
`SupportTicket.aiResponse` has `orphanRemoval = true`. When the async service calls `ticketRepository.save(ticket)` to mark a ticket RESOLVED, the detached `ticket` entity still has `aiResponse = null` (it was loaded before the `AiResponse` was created). JPA's `merge()` loads the managed entity — which now has an `AiResponse` — then copies the detached state onto it. Hibernate sees `aiResponse` change from a proxy to `null` and schedules a DELETE. The fix: set `ticket.setAiResponse(response)` before saving to keep the bidirectional association consistent.

### Virtual threads (`spring.threads.virtual.enabled: true`)
One line enables Java 21 Project Loom across the entire application: Tomcat's request threads, Spring's `@Async` executor, and HikariCP's connection pool all use virtual threads. Blocking calls (DB queries, HTTP calls to Claude and OpenAI) park the virtual thread instead of blocking an OS thread, allowing thousands of concurrent requests without tuning a thread pool.
