# HelloInterview Backend

![CI](https://github.com/yhhsieh82/hellointerview-backend/workflows/CI/badge.svg)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)

Backend service for HelloInterview system design practice platform.

## Project Status

**Completed Steps (1-4 of the MVP Plan):**

✅ **Step 1: Project Setup** - Spring Boot 3.2.2 project with Maven, Java 21
✅ **Step 2: Database Configuration** - PostgreSQL with Flyway migrations
✅ **Step 3: Entity Classes** - JPA entities (QuestionMain, Question, QuestionType)
✅ **Step 4: Repository Layer** - Spring Data JPA repositories

## Technology Stack

- **Framework**: Spring Boot 3.2.2
- **Language**: Java 21
- **Database**: PostgreSQL 14+
- **Build Tool**: Maven
- **Migration Tool**: Flyway
- **Dependencies**: Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok

## Project Structure

```
hellointerview-backend/
├── pom.xml                           # Maven configuration
├── mvnw / mvnw.cmd                   # Maven wrapper scripts
├── src/
│   ├── main/
│   │   ├── java/com/hellointerview/backend/
│   │   │   ├── BackendApplication.java           # Main Spring Boot application
│   │   │   ├── entity/
│   │   │   │   ├── QuestionType.java            # Enum for question types
│   │   │   │   ├── QuestionMain.java            # QuestionMain entity
│   │   │   │   └── Question.java                # Question entity
│   │   │   └── repository/
│   │   │       ├── QuestionMainRepository.java  # Repository with JOIN FETCH
│   │   │       └── QuestionRepository.java       # Question repository
│   │   └── resources/
│   │       ├── application.yml                   # Application configuration
│   │       └── db/migration/
│   │           └── V1__Create_schema.sql         # Database schema migration
│   └── test/
│       └── java/com/hellointerview/backend/     # Test directory (ready for TDD)
└── resource/
    └── prds/                                     # Product requirement documents
```

## Database Schema

### question_main table
- `question_main_id` (BIGSERIAL, PK)
- `name` (VARCHAR(200), NOT NULL)
- `description` (TEXT)
- `write_up` (TEXT, NOT NULL)
- `created_at` (TIMESTAMP, NOT NULL)
- `updated_at` (TIMESTAMP, NOT NULL)

### question table
- `question_id` (BIGSERIAL, PK)
- `question_main_id` (BIGINT, FK, NOT NULL)
- `order` (INTEGER, NOT NULL)
- `type` (VARCHAR(50), NOT NULL) - Enum: FUNCTIONAL_REQ, NON_FUNCTIONAL_REQ, ENTITIES, API, HIGH_LEVEL_DESIGN, DEEP_DIVE
- `name` (VARCHAR(200), NOT NULL)
- `description` (TEXT, NOT NULL)
- `whiteboard_section` (INTEGER, 1-5, NOT NULL)
- `requires_recording` (BOOLEAN, DEFAULT false)
- `created_at` (TIMESTAMP, NOT NULL)

**Relationship**: One-to-Many (QuestionMain → Questions)

## Setup Instructions

### Prerequisites
- Java 21
- PostgreSQL 14+
- Maven (or use included wrapper)

### 1. Start PostgreSQL

```bash
docker run --name postgres-hellointerview \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=hellointerview \
  -p 5432:5432 \
  -d postgres:14
```

### 2. Build the Project

```bash
./mvnw clean install
```

### 3. Run Migrations (Flyway)

Migrations run automatically on application startup. The schema will be created in the `hellointerview` database.

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

The application will start on port `8000`.

## Configuration

Configuration is in `src/main/resources/application.yml`.

### Environment Variables

You can override database settings using environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `hellointerview` | Database name |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | `password` | Database password |

**Example**:
```bash
export DB_HOST=production.db.com
export DB_PASSWORD=secure_password
./mvnw spring-boot:run
```

### Other Settings
- **Server Port**: `8000`

## Next Steps (To Be Implemented)

Following the TDD approach from the plan:

- [ ] **Step 5**: Create seed data migration (V2__Insert_seed_data.sql)
- [ ] **Step 6-7**: Service Layer (TDD)
  - Write `QuestionMainServiceTest` first
  - Implement `QuestionMainService`
- [ ] **Step 8-9**: Controller Layer (TDD)
  - Write `QuestionMainControllerTest` first
  - Implement `QuestionMainController`
- [ ] **Step 10**: Exception Handling
  - `ResourceNotFoundException`
  - `GlobalExceptionHandler`
  - `ErrorResponse` DTO
- [ ] **Step 11**: Run all tests and verify
- [ ] **Step 12**: Manual testing with curl/Postman

## API Specification (To Be Implemented)

**Endpoint**: `GET /api/v1/question-mains/{id}`

**Response (200 OK)**:
```json
{
  "question_main_id": 1,
  "name": "Design Twitter",
  "description": "...",
  "write_up": "...",
  "created_at": "2026-02-13T09:00:00Z",
  "updated_at": "2026-02-13T09:00:00Z",
  "questions": [...]
}
```

**Error Response (404 Not Found)**:
```json
{
  "error": "Resource not found",
  "message": "QuestionMain with id {id} does not exist"
}
```

## Development Principles

This project follows TDD (Test-Driven Development) as specified in `.cursor/rules/rule.md`:

1. **Infrastructure First**: Database, entities, repositories (Steps 1-4) ✅
2. **Tests Before Code**: Write failing tests that define behavior
3. **Implement to Pass**: Write minimal code to make tests pass
4. **Refactor**: Clean up while keeping tests green

## Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

## References

- Foundation PRD: [`resource/prds/00-foundation.md`](resource/prds/00-foundation.md)
- Practice Session Management PRD: [`resource/prds/01-practice-session-management.md`](resource/prds/01-practice-session-management.md)
- Project Rules: [`.cursor/rules/rule.md`](.cursor/rules/rule.md)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
