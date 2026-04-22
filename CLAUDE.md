# CLAUDE.md — AI Support Agent API

This file is read automatically by Claude Code at the start of every session.
It is the single source of truth for project context, decisions, and current progress.

---

## Who the User Is

- Learning Java from scratch — no prior Java experience, but has general programming background
- Goal: break into AI Java Developer roles; this project is a portfolio piece for GitHub and job interviews
- Treat every concept (annotation, pattern, Spring idiom) as new — add a short comment block explaining WHAT it does and WHY it is used here
- Pretend a senior engineer reviews every PR: naming, structure, and code quality must be professional
- One phase at a time: finish a phase fully, then pause and check for questions before proceeding

---

## Non-Negotiable Rules (from the project brief)

1. **One phase at a time.** After each phase, pause and ask if the user has questions or blockers.
2. **Explain everything.** Every new annotation, pattern, or Spring concept gets a short comment block.
3. **No deprecated patterns.** Spring Boot 3 idioms only. Never use `WebSecurityConfigurerAdapter`, deprecated `RestTemplate` patterns, or Spring Boot 2 config styles.
4. **Git discipline.** Suggest a conventional commit message (`feat:`, `fix:`, `chore:`, `docs:`) after each phase.
5. **Test coverage.** Unit tests for every service class (JUnit 5 + Mockito). At least one integration test per controller (`@SpringBootTest` + Testcontainers).
6. **No scope creep.** Do not suggest new technologies mid-phase. Flag optional improvements separately.
7. **Code quality.** Every class, method, and variable named as if a senior engineer is reviewing it.
8. **DTOs are mandatory.** All API requests and responses use DTOs. Never expose raw JPA entities in controllers or API responses.
9. **MapStruct for all mapping.** Use MapStruct for every entity↔DTO mapping. Configure Lombok before MapStruct in `annotationProcessorPaths` — wrong order causes `NullPointerException` at runtime.

---

## All Locked Decisions

| Topic | Decision |
|---|---|
| OS | Windows 11 |
| Java | 21 (Temurin LTS) — virtual threads enabled (`spring.threads.virtual.enabled: true`) |
| Build | Maven |
| Base package | `com.askaragoz.supportagent` |
| Claude model | `claude-haiku-4-5-20251001` (generation) |
| Embedding model | OpenAI `text-embedding-3-small` — 1536-dim vectors (embeddings only; Claude does generation) |
| RAG retrieval | pgvector cosine similarity (`<=>` operator) — semantic search, not keyword |
| AI response mode | Async + polling — `POST /tickets` → 202; client polls `GET /tickets/{id}` |
| Async mechanism | Spring `@Async` on a separate `@Service` bean (not the same class as the caller) |
| API keys | User has neither key yet — `app.ai.mock-enabled: true` in `application.yml` activates `MockRagService` |
| Auth (main API) | None — public API |
| Auth (admin UI) | Basic auth on Spring Boot Admin UI via `ADMIN_PASSWORD` env var |
| Ticket statuses | `PENDING` → `IN_PROGRESS` → `RESOLVED` or `FAILED` |
| AI failure | Status → `FAILED` + `errorMessage` field set on `SupportTicket` |
| Retry | `POST /tickets/{id}/retry` — only when `status = FAILED`; returns 409 otherwise |
| Ticket list filter | `GET /tickets?email=` — users see only their own tickets |
| Pagination | Spring Data `Pageable` on all list endpoints |
| Knowledge CRUD | Full: POST, GET, GET/{id}, PUT/{id} (re-embeds), DELETE/{id} |
| Error format | RFC 7807 `ProblemDetail` — Spring Boot 3 built-in |
| Integration tests | Testcontainers + `pgvector/pgvector:pg16` real PostgreSQL DB |
| Test coverage | JaCoCo HTML report only — no build gate threshold |
| Seed data | `V6__seed_knowledge_articles.sql` — 5–8 fictional SaaS articles pre-loaded |
| Spring Boot Admin | Pre-built Docker image (codecentric) — no custom admin code in this repo |
| Deploy | Railway |
| GitHub | Public from day 1 |

---

## Key Library Versions

| Library | Version |
|---|---|
| Spring Boot | 3.4.1 |
| Spring AI | 1.0.0 (via BOM) |
| Spring Boot Admin client | 3.3.5 |
| MapStruct | 1.6.3 |
| SpringDoc OpenAPI | 2.7.0 |
| pgvector Java client | 0.1.6 |
| JaCoCo | 0.8.12 |

---

## Current Phase Status

| Phase | Status |
|---|---|
| **Phase 1 — Project Setup** | ✅ COMPLETE |
| **Phase 2 — Data Layer** | ✅ COMPLETE |
| **Phase 3 — AI Integration** | ⬜ NEXT |
| Phase 4 — REST API Layer | ⬜ Pending |
| Phase 5 — Production Readiness | ⬜ Pending |
| Phase 6 — README | ⬜ Pending |

**Environment status (as of Phase 1):**
- Java 21 and Maven: being installed via `winget install EclipseAdoptium.Temurin.21.JDK` and `winget install Apache.Maven`
- Docker Desktop: to be installed
- IntelliJ IDEA Community: to be installed

---

## Phase 1 — What Was Built

```
ai-support-agent-api/
├── CLAUDE.md                          ← this file
├── pom.xml                            ← full Maven config; all 6 phases of deps declared
├── docker-compose.yml                 ← pgvector/pgvector:pg16 PostgreSQL service
├── .gitignore                         ← covers IntelliJ, Maven target/, application-local.yml
└── src/
    ├── main/
    │   ├── java/com/askaragoz/supportagent/
    │   │   ├── SupportAgentApplication.java    ← @SpringBootApplication + @EnableAsync
    │   │   ├── config/                         ← empty; AsyncConfig + others added in Phase 3
    │   │   ├── domain/                         ← empty; entities added in Phase 2
    │   │   ├── repository/                     ← empty; repositories added in Phase 2
    │   │   ├── dto/request/                    ← empty; DTOs added in Phase 4
    │   │   ├── dto/response/                   ← empty; DTOs added in Phase 4
    │   │   ├── mapper/                         ← empty; MapStruct mappers added in Phase 4
    │   │   ├── service/                        ← empty; services added in Phase 3 + 4
    │   │   ├── service/ai/                     ← empty; RagService + impls added in Phase 3
    │   │   ├── controller/                     ← empty; controllers added in Phase 4
    │   │   └── exception/                      ← empty; GlobalExceptionHandler added in Phase 4
    │   └── resources/
    │       ├── application.yml                 ← full base config; mock mode ON by default
    │       ├── application-local.yml           ← GITIGNORED; template for real API keys
    │       └── db/migration/                   ← empty; Flyway V1–V6 added in Phase 2
    └── test/
        └── java/com/askaragoz/supportagent/
            ├── service/                        ← empty; unit tests added in Phase 4
            └── controller/                     ← empty; integration tests added in Phase 4
```

---

## Phase 2 — What to Build Next (Data Layer)

Files to create:
- `domain/SupportTicket.java` — entity with `TicketStatus` enum (PENDING/IN_PROGRESS/RESOLVED/FAILED)
- `domain/KnowledgeArticle.java` — entity with `float[]` embedding column (`vector(1536)`)
- `domain/AiResponse.java` — @OneToOne with SupportTicket; stores `retrievedArticleIds` as JSON
- `domain/Feedback.java` — @OneToOne with AiResponse; `FeedbackRating` enum (POSITIVE/NEGATIVE)
- `repository/SupportTicketRepository.java` — `findByUserEmail(email, Pageable)` with `@EntityGraph`
- `repository/KnowledgeArticleRepository.java` — native pgvector similarity query
- `repository/AiResponseRepository.java`
- `repository/FeedbackRepository.java`
- `db/migration/V1__create_support_tickets.sql`
- `db/migration/V2__create_knowledge_articles.sql`
- `db/migration/V3__create_ai_responses.sql`
- `db/migration/V4__create_feedback.sql`
- `db/migration/V5__add_pgvector_extension_and_embedding.sql`
- `db/migration/V6__seed_knowledge_articles.sql`

Key concepts to explain: `@Entity`, `@OneToOne`, `@Enumerated`, `@CreationTimestamp`, Hibernate N+1, `@EntityGraph`, Flyway migration naming rules.

---

## Full API Endpoint Reference

### Tickets
| Method | Path | Request DTO | Response DTO | HTTP Status |
|---|---|---|---|---|
| `POST` | `/api/v1/tickets` | `TicketCreateRequest` | `TicketDetailResponse` | 202 |
| `GET` | `/api/v1/tickets?email=&page=&size=` | — | `Page<TicketResponse>` | 200 |
| `GET` | `/api/v1/tickets/{id}` | — | `TicketDetailResponse` | 200 |
| `POST` | `/api/v1/tickets/{id}/retry` | — | `TicketDetailResponse` | 202 |
| `POST` | `/api/v1/tickets/{id}/feedback` | `FeedbackRequest` | `FeedbackResponse` | 201 |

### Knowledge Base
| Method | Path | Request DTO | Response DTO | HTTP Status |
|---|---|---|---|---|
| `POST` | `/api/v1/knowledge` | `KnowledgeArticleRequest` | `KnowledgeArticleResponse` | 201 |
| `GET` | `/api/v1/knowledge?page=&size=` | — | `Page<KnowledgeArticleResponse>` | 200 |
| `GET` | `/api/v1/knowledge/{id}` | — | `KnowledgeArticleResponse` | 200 |
| `PUT` | `/api/v1/knowledge/{id}` | `KnowledgeArticleRequest` | `KnowledgeArticleResponse` | 200 |
| `DELETE` | `/api/v1/knowledge/{id}` | — | — | 204 |

### Infrastructure
| Method | Path | Notes |
|---|---|---|
| `GET` | `/actuator/health` | Polled by Spring Boot Admin |
| `GET` | `/actuator/info` | App version metadata |
| `GET` | `/swagger-ui.html` | Interactive API docs |

---

## Mock Mode

`app.ai.mock-enabled: true` in `application.yml` (default) → `MockRagService` is active.
`app.ai.mock-enabled: false` in `application-local.yml` → `ClaudeRagService` is active.

`MockRagService` runs the full async flow (status transitions, DB writes) but skips embedding, pgvector search, and Claude calls. No API keys needed.

To use real AI (Phase 3+): copy `application-local.yml`, set real keys, add `--spring.profiles.active=local` to IntelliJ run config.

---

## Critical Risks to Remember

| Risk | Mitigation |
|---|---|
| MapStruct + Lombok ordering | Lombok first, then `lombok-mapstruct-binding`, then MapStruct in `annotationProcessorPaths` |
| pgvector Docker image | Must use `pgvector/pgvector:pg16`, not plain `postgres:16` |
| Vector dimension lock | `text-embedding-3-small` = 1536 dims; changing models invalidates all stored vectors |
| `@Async` transaction boundary | Async method must be on a **separate `@Service` bean** — `@Transactional` does not cross thread boundaries within the same bean |
| Flyway immutability | Never edit a migration file after it has run — always add a new `V{n}__` file |
| Hibernate N+1 | Use `@EntityGraph` on list queries that join `AiResponse` |
