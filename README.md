# Online Survey Service

> **Production-ready Spring Boot monolith REST API** — portfolio & learning project.

---

## Tech Stack

| Layer       | Technology                                        |
|-------------|---------------------------------------------------|
| Language    | Java 21                                           |
| Framework   | Spring Boot 3.3                                   |
| Security    | Spring Security + JWT (JJWT)                      |
| Persistence | Spring Data JPA + Hibernate + PostgreSQL 16       |
| Migrations  | Flyway                                            |
| Validation  | Jakarta Bean Validation                           |
| API Docs    | SpringDoc OpenAPI 3 (Swagger UI)                  |
| Testing     | JUnit 5 + Mockito + Testcontainers                |
| Build       | Maven 3.9                                         |
| Container   | Docker + Docker Compose                           |

---

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### 1 — Start the database

```bash
docker compose up postgres -d
```

### 2 — Run the application (dev profile)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3 — Open Swagger UI

```
http://localhost:8080/swagger-ui.html
```

### 4 — Run tests

```bash
# Unit tests only
./mvnw test -Dtest="**/*Test"

# All tests including integration (requires Docker for Testcontainers)
./mvnw verify
```

### 5 — Run everything with Docker Compose

```bash
docker compose up --build
```

---

## API Overview

| Resource        | Base Path                                    |
|-----------------|----------------------------------------------|
| Auth            | `POST /api/v1/auth/{register,login,refresh}` |
| Users           | `/api/v1/users`                              |
| Surveys         | `/api/v1/surveys`                            |
| Questions       | `/api/v1/surveys/{id}/questions`             |
| Responses       | `/api/v1/surveys/{id}/responses`             |
| Analytics       | `GET /api/v1/surveys/{id}/responses/analytics` |
| CSV Export      | `GET /api/v1/surveys/{id}/responses/export/csv` |

---

## Environment Variables (production)

| Variable            | Description                         |
|---------------------|-------------------------------------|
| `DATABASE_URL`      | JDBC URL for PostgreSQL             |
| `DATABASE_USERNAME` | DB username                         |
| `DATABASE_PASSWORD` | DB password                         |
| `JWT_SECRET`        | 256-bit secret for JWT signing      |

---

## Project Structure

```
src/main/java/com/nazir/onlinesurveyservice/
├── config/          # Spring Security, OpenAPI, AppProperties
├── controller/      # REST controllers (Auth, User, Survey, Question, Response)
├── domain/
│   ├── entity/      # JPA entities (User, Survey, Question, Option, Response, Answer)
│   └── enums/       # Role, SurveyStatus, QuestionType
├── dto/
│   ├── request/     # Validated request records
│   └── response/    # Response records + PageResponse wrapper
├── exception/       # Custom exceptions + GlobalExceptionHandler (RFC 7807)
├── repository/      # Spring Data JPA repositories
├── security/        # JWT provider, auth filter, UserDetailsService
├── service/         # Service interfaces
│   └── impl/        # AuthServiceImpl, UserServiceImpl, SurveyServiceImpl, ...
└── util/            # SlugUtil, HashUtil
```

---

## Development Phases

- [ ] **Phase 1** — Foundation (project setup, DB schema, JWT auth)
- [ ] **Phase 2** — Survey CRUD (survey + question + option management)
- [ ] **Phase 3** — Responses (submit, validate, anonymous support)
- [ ] **Phase 4** — Analytics (aggregation, CSV export)
- [ ] **Phase 5** — Polish (rate limiting, Actuator, full test coverage)

## Full Requirements

See [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) for the complete specification including all functional requirements, NFRs, API design, DB schema, and error formats.
