# HelloInterview Backend

![CI](https://github.com/yhhsieh82/hellointerview-backend/workflows/CI/badge.svg)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)

Backend service for HelloInterview system design practice platform.

## Project Status

**Completed Steps (1-10 of the MVP Plan):**

✅ **Step 1: Project Setup** - Spring Boot 3.2.2 project with Maven, Java 21
✅ **Step 2: Database Configuration** - PostgreSQL with Flyway migrations
✅ **Step 3: Entity Classes** - JPA entities (QuestionMain, Question, QuestionType)
✅ **Step 4: Repository Layer** - Spring Data JPA repositories
✅ **Step 5: Seed Data** - Sample data for 2 QuestionMains (Design Twitter, Design URL Shortener)
✅ **Step 6-7: Service Layer (TDD)** - QuestionMainService with 3 passing unit tests
✅ **Step 8-9: Controller Layer (TDD)** - QuestionMainController with 4 passing integration tests
✅ **Step 10: Exception Handling** - GlobalExceptionHandler with proper error responses

**Test Results:** ✅ 7/7 tests passing

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
│   │   │   ├── controller/
│   │   │   │   └── QuestionMainController.java   # REST controller for question-mains
│   │   │   ├── service/
│   │   │   │   └── QuestionMainService.java      # Business logic service
│   │   │   ├── repository/
│   │   │   │   ├── QuestionMainRepository.java   # Repository with JOIN FETCH
│   │   │   │   └── QuestionRepository.java       # Question repository
│   │   │   ├── entity/
│   │   │   │   ├── QuestionType.java            # Enum for question types
│   │   │   │   ├── QuestionMain.java            # QuestionMain entity
│   │   │   │   └── Question.java                # Question entity
│   │   │   └── exception/
│   │   │       ├── ResourceNotFoundException.java # Custom exception
│   │   │       ├── ErrorResponse.java             # Error response DTO
│   │   │       └── GlobalExceptionHandler.java    # Global exception handler
│   │   └── resources/
│   │       ├── application.yml                   # Application configuration
│   │       └── db/migration/
│   │           ├── V1__Create_schema.sql         # Database schema migration
│   │           └── V2__Insert_seed_data.sql      # Sample seed data
│   └── test/
│       └── java/com/hellointerview/backend/
│           ├── controller/
│           │   └── QuestionMainControllerTest.java # Controller integration tests (4 tests)
│           └── service/
│               └── QuestionMainServiceTest.java     # Service unit tests (3 tests)
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

### 4. Run Tests

```bash
./mvnw test
```

Expected: 7 tests pass (3 service tests + 4 controller tests)

### 5. Run the Application

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

## Next Steps (Manual Testing Required)

The MVP implementation is complete! Next steps:

- [ ] **Step 11**: Start PostgreSQL and run the application
- [ ] **Step 12**: Manual testing with curl/Postman to verify endpoints work end-to-end

## API Specification

**Endpoint**: `GET /api/v1/question-mains/{id}` ✅ **IMPLEMENTED**

**Response (200 OK)**:
```json
{
  "question_main_id": 1,
  "name": "Design Twitter",
  "description": "Design a social media platform similar to Twitter...",
  "write_up": "# Sample Answer...",
  "created_at": "2026-02-13T09:00:00Z",
  "updated_at": "2026-02-13T09:00:00Z",
  "questions": [
    {
      "question_id": 1,
      "question_main_id": 1,
      "order": 1,
      "type": "Functional Req",
      "name": "Define Functional Requirements",
      "description": "What are the core features?",
      "whiteboard_section": 1,
      "requires_recording": false,
      "created_at": "2026-02-13T09:00:00Z"
    }
  ]
}
```

**Error Response (404 Not Found)**:
```json
{
  "error": "Resource not found",
  "message": "QuestionMain with id 999 does not exist"
}
```

**Error Response (400 Bad Request)** - Invalid ID format:
```json
{
  "error": "Bad request",
  "message": "Invalid value 'invalid' for parameter 'id'"
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
