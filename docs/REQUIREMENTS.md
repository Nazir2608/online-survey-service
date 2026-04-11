# Online Survey Service — Full Requirements

## 1. Project Overview

| Item | Detail |
|------|--------|
| **Name** | online-survey-service |
| **Package** | com.nazir.onlinesurveyservice |
| **Type** | Spring Boot Monolith — REST API |
| **Goal** | Learning + Portfolio (production-quality code) |
| **Audience** | Survey creators (admins/authors) & survey takers (public/authenticated respondents) |

---

## 2. Technology Stack

| Layer | Choice | Reason |
|-------|--------|--------|
| Language | Java 21 | LTS, Records, Virtual Threads |
| Framework | Spring Boot 3.3 | Production standard |
| Security | Spring Security + JWT | Stateless REST auth |
| ORM | Spring Data JPA + Hibernate | Standard persistence |
| Database | PostgreSQL 16 | Production RDBMS |
| Migration | Flyway | Schema version control |
| Validation | Jakarta Bean Validation | Input safety |
| Docs | SpringDoc OpenAPI 3 (Swagger UI) | API discoverability |
| Testing | JUnit 5 + Mockito + Testcontainers | Unit + Integration |
| Build | Maven 3.9 | Standard Java build |
| Containerization | Docker + Docker Compose | Local dev parity |
| Logging | SLF4J + Logback | Structured JSON logs (prod) |
| Auditing | Spring Data Auditing | createdAt/updatedAt auto |

---

## 3. Core Domain Model

```
User ──────────────────────── Survey
 |                               |
 | (CREATOR/RESPONDENT)          |── Question (ordered, typed)
 |                               |      |── Option (for MCQ)
 └── Response ──────────────────-┘
          |── Answer ─────── Question
```

### Entities

#### User
- `id` (UUID), `email` (unique), `password` (bcrypt), `displayName`
- `role`: ADMIN | CREATOR | RESPONDENT
- `createdAt`, `updatedAt`, `enabled`

#### Survey
- `id` (UUID), `title`, `description`, `slug` (unique, URL-safe)
- `status`: DRAFT | PUBLISHED | CLOSED | ARCHIVED
- `creator` (→ User)
- `startDate`, `endDate` (nullable — open-ended surveys allowed)
- `isAnonymous` (bool — allow unauthenticated responses)
- `maxResponses` (nullable — cap entries)
- `createdAt`, `updatedAt`

#### Question
- `id` (UUID), `surveyId`, `text`, `helpText`
- `type`: TEXT | PARAGRAPH | SINGLE_CHOICE | MULTIPLE_CHOICE | RATING | DATE | EMAIL | NUMBER
- `orderIndex` (display order), `isRequired` (bool)
- `minValue` / `maxValue` (for RATING/NUMBER)
- `createdAt`, `updatedAt`

#### Option  *(only for SINGLE_CHOICE / MULTIPLE_CHOICE)*
- `id` (UUID), `questionId`, `text`, `orderIndex`

#### Response  *(one per survey submission)*
- `id` (UUID), `surveyId`
- `respondent` (→ User, nullable for anonymous)
- `submittedAt`, `ipAddress` (hashed), `userAgent`

#### Answer  *(one per question per response)*
- `id` (UUID), `responseId`, `questionId`
- `textValue` (for TEXT/PARAGRAPH/DATE/EMAIL/NUMBER)
- `selectedOptions` (→ Option, many-to-many, for SINGLE/MULTI choice)
- `ratingValue` (Integer, for RATING)

---

## 4. Functional Requirements

### 4.1 Authentication & Authorization

| # | Requirement |
|---|------------|
| FR-AUTH-01 | User can register with email + password |
| FR-AUTH-02 | User can login and receive JWT access token + refresh token |
| FR-AUTH-03 | Access token expires in 15 minutes; refresh token in 7 days |
| FR-AUTH-04 | Refresh token endpoint issues new access token |
| FR-AUTH-05 | Logout invalidates refresh token (stored server-side) |
| FR-AUTH-06 | ADMIN can manage all users (list, deactivate, change role) |

### 4.2 Survey Management (Creator)

| # | Requirement |
|---|------------|
| FR-SRV-01 | Creator can create a new survey (starts as DRAFT) |
| FR-SRV-02 | Creator can add, edit, reorder, and delete questions |
| FR-SRV-03 | Creator can add options to MCQ questions |
| FR-SRV-04 | Creator can publish a survey (DRAFT → PUBLISHED) |
| FR-SRV-05 | Creator can close a survey (PUBLISHED → CLOSED) |
| FR-SRV-06 | Creator can archive a survey (CLOSED → ARCHIVED) |
| FR-SRV-07 | Creator can view all their own surveys with status filter |
| FR-SRV-08 | Creator can preview survey before publish |
| FR-SRV-09 | Creator can duplicate an existing survey |
| FR-SRV-10 | ADMIN can view and manage all surveys |

### 4.3 Survey Discovery (Public)

| # | Requirement |
|---|------------|
| FR-DSC-01 | List all PUBLISHED surveys (paginated) |
| FR-DSC-02 | Get survey details by ID or slug |
| FR-DSC-03 | Search surveys by title/description keyword |

### 4.4 Survey Response (Respondent)

| # | Requirement |
|---|------------|
| FR-RSP-01 | Authenticated user can submit a response to a PUBLISHED survey |
| FR-RSP-02 | If survey is anonymous, unauthenticated users can also submit |
| FR-RSP-03 | System validates all required questions are answered |
| FR-RSP-04 | System validates answer types match question types |
| FR-RSP-05 | One response per user per survey (enforced) |
| FR-RSP-06 | If maxResponses is set, reject submission after cap is reached |

### 4.5 Results & Analytics (Creator / Admin)

| # | Requirement |
|---|------------|
| FR-ANL-01 | Creator can view list of all responses for their survey |
| FR-ANL-02 | Creator can view individual response detail |
| FR-ANL-03 | Creator can get aggregated stats per question (counts, percentages for MCQ; avg/min/max for RATING/NUMBER) |
| FR-ANL-04 | Creator can export responses as CSV |
| FR-ANL-05 | Creator can see total response count, completion rate, response trend by day |

---

## 5. Non-Functional Requirements

| # | Category | Requirement |
|---|----------|------------|
| NFR-01 | Security | All endpoints except public survey list + survey submission (anonymous) require JWT |
| NFR-02 | Security | Passwords stored as BCrypt hash (strength 12) |
| NFR-03 | Security | IP addresses in responses stored as SHA-256 hash |
| NFR-04 | Security | Rate limiting on auth endpoints (login: 5 req/min per IP) |
| NFR-05 | Validation | All inputs validated with Bean Validation; errors return structured JSON |
| NFR-06 | Error Handling | Global exception handler returns RFC 7807 Problem Details format |
| NFR-07 | Observability | Request/response logging via filter (exclude sensitive fields) |
| NFR-08 | Observability | Spring Actuator exposed (health, info, metrics) |
| NFR-09 | Performance | Pagination enforced on all list endpoints (max page size: 50) |
| NFR-10 | DB | Flyway manages all schema changes; no `ddl-auto: create` in prod |
| NFR-11 | API Docs | Swagger UI available at `/swagger-ui.html` (disabled in prod profile) |
| NFR-12 | Testing | Unit tests for all service methods; integration tests for all API endpoints |
| NFR-13 | Testing | Testcontainers for real PostgreSQL in integration tests |
| NFR-14 | Config | Profile-based config: `dev`, `prod` |
| NFR-15 | Container | Docker Compose file for local dev (app + postgres + pgadmin) |

---

## 6. API Endpoint Design

### Auth  `/api/v1/auth`
```
POST   /register              → 201 Created
POST   /login                 → 200 OK (tokens)
POST   /refresh               → 200 OK (new access token)
POST   /logout                → 204 No Content
```

### Users  `/api/v1/users`
```
GET    /me                    → 200 (own profile)         [AUTHENTICATED]
PUT    /me                    → 200 (update profile)      [AUTHENTICATED]
PUT    /me/password           → 204 (change password)     [AUTHENTICATED]
GET    /                      → 200 (paginated)           [ADMIN]
GET    /{id}                  → 200                       [ADMIN]
PATCH  /{id}/role             → 200                       [ADMIN]
PATCH  /{id}/status           → 200 (enable/disable)      [ADMIN]
```

### Surveys  `/api/v1/surveys`
```
POST   /                      → 201           [CREATOR, ADMIN]
GET    /                      → 200 paginated [PUBLIC - only PUBLISHED]
GET    /my                    → 200 paginated [CREATOR, ADMIN - own surveys]
GET    /{id}                  → 200           [PUBLIC if PUBLISHED, else CREATOR/ADMIN]
GET    /slug/{slug}           → 200           [PUBLIC if PUBLISHED]
PUT    /{id}                  → 200           [CREATOR (owner), ADMIN]
DELETE /{id}                  → 204           [CREATOR (owner), ADMIN]
PATCH  /{id}/status           → 200           [CREATOR (owner), ADMIN]
POST   /{id}/duplicate        → 201           [CREATOR, ADMIN]
```

### Questions  `/api/v1/surveys/{surveyId}/questions`
```
POST   /                      → 201           [CREATOR (owner), ADMIN]
GET    /                      → 200 (ordered) [PUBLIC if survey PUBLISHED]
GET    /{questionId}          → 200           [PUBLIC if survey PUBLISHED]
PUT    /{questionId}          → 200           [CREATOR (owner), ADMIN]
DELETE /{questionId}          → 204           [CREATOR (owner), ADMIN]
PUT    /reorder               → 200           [CREATOR (owner), ADMIN]
```

### Responses  `/api/v1/surveys/{surveyId}/responses`
```
POST   /                      → 201           [AUTHENTICATED or ANONYMOUS if survey.isAnonymous]
GET    /                      → 200 paginated [CREATOR (owner), ADMIN]
GET    /{responseId}          → 200           [CREATOR (owner), ADMIN, or own respondent]
GET    /analytics             → 200           [CREATOR (owner), ADMIN]
GET    /export/csv            → 200 CSV file  [CREATOR (owner), ADMIN]
```

---

## 7. Error Response Format (RFC 7807)

```json
{
  "type": "https://onlinesurveyservice.com/errors/validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "One or more fields have invalid values",
  "instance": "/api/v1/surveys",
  "timestamp": "2025-01-15T10:30:00Z",
  "errors": [
    { "field": "title", "message": "must not be blank" }
  ]
}
```

---

## 8. Database Schema (Flyway V1)

Key tables: `users`, `surveys`, `questions`, `options`, `responses`, `answers`, `answer_selected_options`, `refresh_tokens`

---

## 9. Project Phases

| Phase | Features | Status |
|-------|---------|--------|
| **Phase 1 — Foundation** | Project setup, DB, Auth (register/login/JWT), User profile | 🔲 TODO |
| **Phase 2 — Survey CRUD** | Survey + Question + Option management | 🔲 TODO |
| **Phase 3 — Responses** | Survey submission, validation, anonymous support | 🔲 TODO |
| **Phase 4 — Analytics** | Aggregation, CSV export | 🔲 TODO |
| **Phase 5 — Polish** | Rate limiting, Actuator, Integration tests, Docker | 🔲 TODO |

---

## 10. Git Branching Strategy

```
main        ← stable, tagged releases
develop     ← integration branch
feature/*   ← new features  e.g. feature/auth-module
fix/*       ← bug fixes
chore/*     ← infra, docs, deps
```

Commit format: `[scope] short description`  
Examples: `[auth] add JWT token provider`, `[survey] implement question reorder`, `[docs] update requirements`
