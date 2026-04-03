# Finance Data Processing and Access Control Backend

A Spring Boot REST API for managing financial records with role-based access control, dashboard analytics, and JWT authentication.

## Tech Stack

- **Java 17** + **Spring Boot 3.2.5**
- **Spring Security** with JWT (jjwt)
- **Spring Data JPA** + **H2 Database** (embedded, zero-setup, file-based persistence)
- **Bean Validation** (Jakarta Validation)
- **Springdoc OpenAPI** (Swagger UI)
- **Lombok**
- **Maven**

## Architecture

```
src/main/java/com/finance/
├── config/          # Security, JWT filter, OpenAPI, data seeder
├── controller/      # REST endpoints
├── dto/
│   ├── request/     # Incoming payloads with validation
│   └── response/    # Outgoing response shapes
├── entity/          # JPA entities
├── enums/           # Role, RecordType
├── exception/       # Global exception handler + custom exceptions
├── repository/      # Spring Data JPA repositories
├── security/        # JWT token service
└── service/         # Business logic layer
```

## Setup

### Prerequisites

- Java 17+
- Maven 3.8+

No external database needed. The app uses **H2 embedded database** with file-based storage (`./data/finance_db`). Data persists across restarts automatically.

### Run

```bash
cd finance-backend
./mvnw spring-boot:run
```

The app starts on **http://localhost:8080**.

### Run Tests

```bash
./mvnw test
```

Tests use H2 in-memory database.

### H2 Console

While the app is running, you can browse the database at **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:file:./data/finance_db`
- Username: `sa`
- Password: *(leave empty)*

## API Documentation

Once running, open **http://localhost:8080/swagger-ui.html** for interactive Swagger UI.

## Default Users (Seeded on First Run)

| Username  | Password    | Role    |
|-----------|-------------|---------|
| admin     | admin123    | ADMIN   |
| analyst   | analyst123  | ANALYST |
| viewer    | viewer123   | VIEWER  |

## API Endpoints

### Authentication (`/api/auth`) — Public

| Method | Endpoint            | Description          |
|--------|---------------------|----------------------|
| POST   | `/api/auth/register` | Register a new user  |
| POST   | `/api/auth/login`    | Login, receive JWT   |

### Users (`/api/users`) — Admin Only

| Method | Endpoint          | Description                    |
|--------|-------------------|--------------------------------|
| GET    | `/api/users`       | List all users                 |
| GET    | `/api/users/{id}`  | Get user by ID                 |
| PUT    | `/api/users/{id}`  | Update user (role, status, etc)|
| DELETE | `/api/users/{id}`  | Deactivate user (soft delete)  |

### Financial Records (`/api/records`)

| Method | Endpoint            | Roles              | Description                          |
|--------|---------------------|---------------------|--------------------------------------|
| GET    | `/api/records`       | VIEWER, ANALYST, ADMIN | List with filters & pagination   |
| GET    | `/api/records/{id}`  | VIEWER, ANALYST, ADMIN | Get single record                |
| POST   | `/api/records`       | ADMIN               | Create record                        |
| PUT    | `/api/records/{id}`  | ADMIN               | Update record                        |
| DELETE | `/api/records/{id}`  | ADMIN               | Soft-delete record                   |

**Filter parameters:** `type`, `category`, `startDate`, `endDate`, `page`, `size`, `sort`

### Dashboard (`/api/dashboard`) — Analyst & Admin

| Method | Endpoint               | Description                                |
|--------|------------------------|--------------------------------------------|
| GET    | `/api/dashboard/summary` | Aggregated summary (income, expenses, trends) |

## Access Control Matrix

| Action                 | VIEWER | ANALYST | ADMIN |
|------------------------|--------|---------|-------|
| View records           | Yes    | Yes     | Yes   |
| View dashboard summary | No     | Yes     | Yes   |
| Create/Update/Delete records | No | No   | Yes   |
| Manage users           | No     | No      | Yes   |

## Example Usage

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@test.com","password":"pass123","fullName":"John Doe","role":"VIEWER"}'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### Create Record (Admin)
```bash
curl -X POST http://localhost:8080/api/records \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"amount":5000,"type":"INCOME","category":"Salary","date":"2026-04-01","description":"April salary"}'
```

### Filter Records
```bash
curl "http://localhost:8080/api/records?type=EXPENSE&category=Food&startDate=2026-01-01&endDate=2026-12-31&page=0&size=10" \
  -H "Authorization: Bearer <token>"
```

### Dashboard Summary
```bash
curl http://localhost:8080/api/dashboard/summary \
  -H "Authorization: Bearer <token>"
```

## Design Decisions & Assumptions

1. **Soft delete** for both users (deactivation) and financial records — no data is permanently lost.
2. **JWT stateless auth** — no server-side session storage; tokens expire in 24 hours.
3. **Role is stored as a single enum on User** rather than a many-to-many roles table. This keeps the model simple for three well-defined roles. Easy to extend to a join table if needed.
4. **Dashboard queries use JPQL aggregations** pushed down to the database for efficiency, rather than loading all records into memory.
5. **Monthly trends** default to the last 6 months.
6. **Pagination** is supported on record listing via Spring Data's `Pageable` (page, size, sort query params).
7. **Any user can register** — in production, admin-created registration or invite flows would be more appropriate.
8. **The JWT secret in application.yml is a placeholder** — in production, this should be externalized via environment variables.

## Tradeoffs

- **No refresh token flow** — kept auth simple with a single access token. A production system would need refresh tokens.
- **No audit trail** — `createdBy` is tracked on records, but a full audit log (who changed what, when) is not implemented.
- **No rate limiting** — could be added via Spring's `@RateLimiter` or a filter.
- **Category is a free-text field** — a separate categories table with predefined options would be cleaner in production.
