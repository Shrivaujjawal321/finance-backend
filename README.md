# Finance Data Processing and Access Control Backend

A full-stack finance dashboard application built with **Java 17, Spring Boot 3, and H2 Database**. Features JWT authentication, role-based access control (RBAC), financial records management, dashboard analytics with caching, rate limiting, and a classic HTML/CSS/JS frontend — all served from a single deployable unit.

> **Quick Start:** Clone, run `./mvnw spring-boot:run`, open `http://localhost:8080`. No database setup needed.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Default Users](#default-users)
- [API Endpoints](#api-endpoints)
- [Access Control Matrix](#access-control-matrix)
- [Features Implemented](#features-implemented)
- [API Usage Examples](#api-usage-examples)
- [Frontend Pages](#frontend-pages)
- [Security Measures](#security-measures)
- [Design Decisions & Assumptions](#design-decisions--assumptions)
- [What I Would Do Differently in Production](#what-i-would-do-differently-in-production)
- [Project Structure](#project-structure)
- [Running Tests](#running-tests)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Database | H2 (embedded, file-based, zero-setup) |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation |
| Caching | Spring Cache + Caffeine (in-memory) |
| API Docs | Springdoc OpenAPI / Swagger UI |
| Build | Maven + Maven Wrapper |
| CI/CD | GitHub Actions |
| Frontend | Vanilla HTML5 + CSS3 + JavaScript + Chart.js |

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                     Client (Browser)                     │
│         index.html / dashboard.html / records.html       │
└────────────────────────┬─────────────────────────────────┘
                         │  HTTP + JWT Bearer Token
                         ▼
┌──────────────────────────────────────────────────────────┐
│                   Spring Boot Application                │
│                                                          │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────┐  │
│  │ Rate Limit  │→ │  JWT Auth    │→ │  Security      │  │
│  │ Filter      │  │  Filter      │  │  Config (RBAC) │  │
│  └─────────────┘  └──────────────┘  └────────────────┘  │
│                         │                                │
│  ┌──────────────────────▼───────────────────────────┐    │
│  │              REST Controllers                     │    │
│  │  AuthController │ RecordController │ Dashboard    │    │
│  │  UserController │                  │ Controller   │    │
│  └──────────────────────┬───────────────────────────┘    │
│                         │                                │
│  ┌──────────────────────▼───────────────────────────┐    │
│  │              Service Layer                        │    │
│  │  AuthService │ RecordService │ DashboardService   │    │
│  │  UserService │               │ (@Cacheable 60s)   │    │
│  └──────────────────────┬───────────────────────────┘    │
│                         │                                │
│  ┌──────────────────────▼───────────────────────────┐    │
│  │           Spring Data JPA Repositories            │    │
│  │    UserRepo │ FinancialRecordRepo │ ResetTokenRepo│    │
│  └──────────────────────┬───────────────────────────┘    │
│                         │                                │
│  ┌──────────────────────▼───────────────────────────┐    │
│  │            H2 Database (file-based)               │    │
│  │     users │ financial_records │ reset_tokens       │    │
│  └──────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────┘
```

**Separation of Concerns:**
- **Controllers** — thin, handle HTTP request/response mapping only
- **Services** — all business logic, validation rules, caching
- **Repositories** — database access via Spring Data JPA (JPQL queries)
- **DTOs** — separate request/response objects, never expose entities directly
- **Filters** — rate limiting and JWT authentication as cross-cutting concerns

---

## Getting Started

### Prerequisites

- **Java 17+** (verify: `java -version`)
- **Maven 3.8+** (or use the included `./mvnw` wrapper)

No external database, Docker, or additional services needed.

### Run the Application

```bash
git clone https://github.com/Shrivaujjawal321/finance-backend.git
cd finance-backend
./mvnw spring-boot:run
```

The app starts on **http://localhost:8080** in ~6 seconds.

### Available URLs

| URL | Description |
|-----|-------------|
| http://localhost:8080 | Frontend (Login page) |
| http://localhost:8080/swagger-ui.html | Interactive API documentation |
| http://localhost:8080/h2-console | Database browser (JDBC URL: `jdbc:h2:file:./data/finance_db`, user: `sa`, no password) |

---

## Default Users

Three users are seeded on first startup for immediate testing:

| Username | Password | Role | Permissions |
|----------|----------|------|-------------|
| `admin` | `admin123` | ADMIN | Full access — manage records, users, view dashboard |
| `analyst` | `analyst123` | ANALYST | Read records + dashboard summaries |
| `viewer` | `viewer123` | VIEWER | Read records only |

---

## API Endpoints

### Authentication (`/api/auth`) — Public, Rate Limited (10 req/min)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user (always assigned VIEWER role) |
| POST | `/api/auth/login` | Login, receive JWT token |
| POST | `/api/auth/forgot-password` | Request password reset (token sent via email/logs) |
| POST | `/api/auth/reset-password` | Reset password using token |

### Financial Records (`/api/records`)

| Method | Endpoint | Allowed Roles | Description |
|--------|----------|---------------|-------------|
| GET | `/api/records` | VIEWER, ANALYST, ADMIN | List records with filters and pagination |
| GET | `/api/records/{id}` | VIEWER, ANALYST, ADMIN | Get single record |
| POST | `/api/records` | ADMIN | Create record |
| PUT | `/api/records/{id}` | ADMIN | Update record |
| DELETE | `/api/records/{id}` | ADMIN | Soft-delete record |

**Query Parameters:** `type` (INCOME/EXPENSE), `category`, `startDate`, `endDate`, `page`, `size`, `sort`

### Dashboard (`/api/dashboard`) — Analyst & Admin

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dashboard/summary` | Aggregated summary with totals, category breakdown, monthly trends, recent activity |

**Response includes:** totalIncome, totalExpenses, netBalance, categoryWiseTotals, recentActivity (last 10), monthlyTrends (last 6 months)

### User Management (`/api/users`) — Admin Only

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users` | List all users |
| GET | `/api/users/{id}` | Get user by ID |
| PUT | `/api/users/{id}` | Update user (role, status, name, email) |
| DELETE | `/api/users/{id}` | Deactivate user (soft delete) |

---

## Access Control Matrix

| Action | VIEWER | ANALYST | ADMIN |
|--------|--------|---------|-------|
| View financial records | Yes | Yes | Yes |
| Filter/paginate records | Yes | Yes | Yes |
| View dashboard summary | No | Yes | Yes |
| Create/update/delete records | No | No | Yes |
| Manage users (list, edit, deactivate) | No | No | Yes |
| Register new account | Public (always VIEWER) | — | — |
| Elevate user roles | No | No | Yes (via PUT /api/users/{id}) |

---

## Features Implemented

### Core Requirements

| # | Requirement | Status | Implementation Details |
|---|------------|--------|----------------------|
| 1 | User and Role Management | Done | Three roles (VIEWER, ANALYST, ADMIN), user CRUD, status management, role assignment by admin |
| 2 | Financial Records Management | Done | Full CRUD with filtering by type/category/date range, pagination, soft delete |
| 3 | Dashboard Summary APIs | Done | Total income/expenses, net balance, category-wise totals, recent activity, 6-month trends |
| 4 | Access Control Logic | Done | Spring Security filter chain with role-based URL authorization |
| 5 | Validation and Error Handling | Done | Bean Validation on all DTOs, GlobalExceptionHandler with proper HTTP status codes |
| 6 | Data Persistence | Done | H2 file-based database, JPA entities with indexes, BigDecimal for monetary values |

### Optional Enhancements

| Enhancement | Status | Details |
|------------|--------|---------|
| Token Authentication | Done | JWT with 24-hour expiry, Bearer token in Authorization header |
| Pagination | Done | Spring Data Pageable — `page`, `size`, `sort` query params |
| Soft Delete | Done | `deleted` flag on records, `active` flag on users |
| Rate Limiting | Done | 10 requests/minute per IP on auth endpoints (HTTP 429) |
| Tests | Done | 15 integration tests covering auth, RBAC, and role enforcement |
| API Documentation | Done | Swagger UI with JWT auth support at `/swagger-ui.html` |
| Password Reset | Done | Token-based reset flow with expiry (15 min) and single-use tokens |
| Caching | Done | Caffeine cache on dashboard (60s TTL), auto-evict on record changes |
| CI/CD Pipeline | Done | GitHub Actions — build, test, Docker image on every push |
| Frontend | Done | Login/Register, Dashboard with charts, Records CRUD, User Management |
| Security Hardening | Done | See [Security Measures](#security-measures) below |

---

## API Usage Examples

### 1. Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "username": "admin",
  "role": "ADMIN"
}
```

### 2. Register (Always Assigns VIEWER)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@test.com","password":"pass123","fullName":"John Doe"}'
```

### 3. Create Financial Record (Admin Only)

```bash
curl -X POST http://localhost:8080/api/records \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"amount":5000,"type":"INCOME","category":"Salary","date":"2026-04-01","description":"April salary"}'
```

### 4. Filter Records with Pagination

```bash
curl "http://localhost:8080/api/records?type=EXPENSE&category=Food&startDate=2026-01-01&endDate=2026-12-31&page=0&size=10&sort=date,desc" \
  -H "Authorization: Bearer <token>"
```

### 5. Dashboard Summary

```bash
curl http://localhost:8080/api/dashboard/summary \
  -H "Authorization: Bearer <token>"
```

Response:
```json
{
  "totalIncome": 8000.00,
  "totalExpenses": 2200.00,
  "netBalance": 5800.00,
  "categoryWiseTotals": { "Salary": 5000, "Freelance": 3000, "Rent": 1200, "Food": 800, "Transport": 200 },
  "recentActivity": [ ... ],
  "monthlyTrends": [
    { "month": "2026-03", "income": 3000, "expenses": 800, "net": 2200 },
    { "month": "2026-04", "income": 5000, "expenses": 1400, "net": 3600 }
  ]
}
```

### 6. Forgot Password

```bash
curl -X POST http://localhost:8080/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@finance.com"}'
# Token appears in server console logs (simulates email delivery)
```

---

## Frontend Pages

The frontend is served directly by Spring Boot from `/src/main/resources/static/`. No separate build step needed.

| Page | URL | Description |
|------|-----|-------------|
| Login / Register | `/index.html` | Authentication with forgot password flow |
| Dashboard | `/dashboard.html` | Summary cards, bar chart (monthly trends), doughnut chart (categories), recent activity table |
| Records | `/records.html` | Full CRUD table with type/category/date filters, pagination, add/edit modals |
| Users | `/users.html` | Admin-only user management — edit roles, status, deactivate |

**Tech:** Vanilla HTML/CSS/JS + Chart.js (CDN). Role-based UI — navigation items and action buttons are shown/hidden based on the logged-in user's role.

---

## Security Measures

I performed a self-directed security review of the codebase and fixed the following issues. Full details with OWASP categories are documented in [`SECURITY_FIXES.md`](SECURITY_FIXES.md).

| # | Severity | Fix | OWASP Category |
|---|----------|-----|----------------|
| 1 | **P0** | Registration locked to VIEWER role — prevents privilege escalation | A01: Broken Access Control |
| 2 | **P0** | Password reset token removed from API response — prevents token leakage | A07: Auth Failures |
| 3 | **P0** | JWT secret externalized via `APP_JWT_SECRET` env var — prevents credential exposure | A02: Cryptographic Failures |
| 4 | **P1** | Rate limiting on auth endpoints — 10 req/min per IP | A07: Auth Failures |
| 5 | **P1** | Dev/Prod Spring profiles — H2 console and Swagger disabled in production | A05: Security Misconfiguration |
| 6 | **P1** | Forgot password returns generic message for unknown emails — prevents email enumeration | A07: Auth Failures |

---

## Design Decisions & Assumptions

| Decision | Reasoning |
|----------|-----------|
| **H2 file-based database** | Zero-setup for evaluators — just clone and run. Production profile (`application-prod.yml`) is ready for PostgreSQL/MySQL. |
| **Single role per user (enum)** | Three well-defined roles don't need a many-to-many join table. Easy to extend if needed. |
| **Public registration → always VIEWER** | Principle of Least Privilege. Role elevation is an admin-only action. |
| **Soft delete on records and users** | Financial data has audit implications — hard deletes are a one-way door. |
| **BigDecimal for monetary amounts** | IEEE 754 floating-point (`double`) causes rounding errors with money. `BigDecimal(15,2)` is precise. |
| **JWT stateless auth** | No server-side session storage. Tokens expire in 24 hours. Simpler to scale horizontally. |
| **Password reset token via server logs** | No email server available in local dev. Token is logged to console (simulates email delivery). API response never contains the token — same contract as production. |
| **Dashboard cached for 60 seconds** | Read-heavy endpoint. Cache evicts automatically when records are created, updated, or deleted. Tradeoff: dashboard may be up to 60s stale, which is acceptable for analytics. |
| **Rate limiting (10/min on auth)** | Prevents brute-force attacks without impacting normal usage. Configurable via `app.rate-limit.max-requests`. |
| **Frontend served from Spring Boot** | Single deployable JAR. For a larger team, the frontend would be a separate React/Next.js app. |

---

## What I Would Do Differently in Production

| Area | Current (Assessment) | Production Approach |
|------|---------------------|-------------------|
| Database | H2 (embedded) | PostgreSQL on RDS/Cloud SQL |
| Schema Migrations | `ddl-auto: update` | Flyway with versioned SQL scripts |
| Email | Server console logging | Spring Mail + SendGrid/SES |
| Auth Tokens | localStorage | HttpOnly Secure cookies (SameSite=Strict) |
| Caching | Caffeine (in-memory) | Redis (distributed, shared across instances) |
| Rate Limiting | In-memory ConcurrentHashMap | Redis-backed (bucket4j) for horizontal scaling |
| Frontend | Vanilla JS served by Spring | React/Next.js as separate deployment |
| Monitoring | None | Micrometer + Prometheus + Grafana |
| Audit Logging | `createdBy` field only | Full audit trail (who changed what, when) |
| API Versioning | None | `/api/v1/` prefix with backward compatibility |

---

## Project Structure

```
finance-backend/
├── .github/workflows/ci.yml          # CI/CD pipeline (build + test + Docker)
├── Dockerfile                         # Multi-stage Docker build (runs tests)
├── pom.xml                            # Maven dependencies
├── SECURITY_FIXES.md                  # Detailed security review documentation
├── src/
│   ├── main/
│   │   ├── java/com/finance/
│   │   │   ├── FinanceApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java        # RBAC rules, filter chain
│   │   │   │   ├── JwtAuthFilter.java         # JWT token extraction/validation
│   │   │   │   ├── RateLimitFilter.java       # IP-based rate limiting
│   │   │   │   ├── PasswordEncoderConfig.java # BCrypt encoder bean
│   │   │   │   ├── OpenApiConfig.java         # Swagger/JWT setup
│   │   │   │   ├── WebConfig.java             # Root URL redirect
│   │   │   │   └── DataSeeder.java            # Seed default users
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java        # Register, Login, Forgot/Reset Password
│   │   │   │   ├── FinancialRecordController.java # CRUD + Filters
│   │   │   │   ├── DashboardController.java   # Summary analytics
│   │   │   │   └── UserController.java        # Admin user management
│   │   │   ├── dto/
│   │   │   │   ├── request/                   # RegisterRequest, LoginRequest, etc.
│   │   │   │   └── response/                  # AuthResponse, DashboardSummary, etc.
│   │   │   ├── entity/
│   │   │   │   ├── User.java                  # Implements UserDetails
│   │   │   │   ├── FinancialRecord.java       # Indexed, soft-deletable
│   │   │   │   └── PasswordResetToken.java    # Single-use, 15-min expiry
│   │   │   ├── enums/                         # Role, RecordType
│   │   │   ├── exception/                     # GlobalExceptionHandler + custom exceptions
│   │   │   ├── repository/                    # JPA repositories with JPQL queries
│   │   │   ├── security/
│   │   │   │   └── JwtService.java            # Token generation/validation + secret check
│   │   │   └── service/                       # Business logic layer
│   │   └── resources/
│   │       ├── application.yml                # Main config (env-var driven)
│   │       ├── application-prod.yml           # Production overrides
│   │       └── static/                        # Frontend (HTML + CSS + JS)
│   └── test/
│       └── java/com/finance/
│           ├── FinanceApplicationTests.java   # Context load test
│           ├── AuthControllerTest.java        # Auth flow tests (5 tests)
│           └── AccessControlTest.java         # RBAC enforcement (9 tests)
└── render.yaml                                # Render.com deployment config
```

---

## Running Tests

```bash
./mvnw test
```

**15 tests** covering:
- User registration (success, duplicate, validation)
- Login (success, invalid credentials)
- Role-based access control for all 3 roles across all endpoint types
- **Role enforcement regression test** — verifies that sending `"role":"ADMIN"` in registration payload still assigns VIEWER

Tests use H2 in-memory database and elevated rate limits — no external dependencies needed.

---

## Links

- **Repository:** https://github.com/Shrivaujjawal321/finance-backend
- **API Docs (local):** http://localhost:8080/swagger-ui.html
- **Security Review:** [`SECURITY_FIXES.md`](SECURITY_FIXES.md)
