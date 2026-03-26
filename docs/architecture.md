# 🏗️ Architecture — InsureTrack Auto Insurance Platform

A deep-dive into the system architecture, component responsibilities, domain model, and design patterns.

---

## System Architecture Overview

InsureTrack follows a **Layered MVC Architecture** — the standard enterprise Java pattern enforced by Spring Boot. Each layer has a single, well-defined responsibility and communicates only with adjacent layers.

```
┌──────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                        │
│        Thymeleaf Templates + Bootstrap 5 + Vanilla JS            │
│   templates/**, static/css/app.css, static/js/app.js            │
└──────────────────────────┬───────────────────────────────────────┘
                           │  HTTP Requests / Model + View
┌──────────────────────────▼───────────────────────────────────────┐
│                         CONTROLLER LAYER                         │
│  DashboardController  │  PolicyController  │  PaymentController  │
│  PremiumController    │  ReportsController │  AdminController    │
│  CustomerPortalController  │  PremiumCalculatorController        │
│  NotificationController    │  AuthController                     │
└──────────────────────────┬───────────────────────────────────────┘
                           │  Service calls
┌──────────────────────────▼───────────────────────────────────────┐
│                          SERVICE LAYER                           │
│  PolicyService  │  PaymentService  │  PremiumScheduleService     │
│  NotificationService  │  AuditService                           │
└──────────────────────────┬───────────────────────────────────────┘
                           │  Repository calls
┌──────────────────────────▼───────────────────────────────────────┐
│                        REPOSITORY LAYER                          │
│  PolicyRepository  │  PaymentRepository  │  AppUserRepository    │
│  NotificationRepository  │  AuditLogRepository                   │
│              Spring Data JPA — Hibernate ORM                     │
└──────────────────────────┬───────────────────────────────────────┘
                           │  JDBC / HikariCP connection pool
┌──────────────────────────▼───────────────────────────────────────┐
│                          DATABASE LAYER                          │
│     H2 (local/ci)  │  MySQL 8 (staging)  │  PostgreSQL (prod)   │
└──────────────────────────────────────────────────────────────────┘
```

**Cross-cutting concerns** (Spring Security, AuditService, NotificationService) operate across all layers.

---

## Component Responsibilities

### Controller Layer
Controllers handle HTTP requests. They receive form parameters, call service methods, populate the Spring `Model`, and return Thymeleaf view names or redirects. Controllers are kept **thin** — no business logic lives here.

### Service Layer
Services contain all business logic. They orchestrate multiple repository calls, manage `@Transactional` boundaries, enforce business rules (e.g., advance payment eligibility, payment retry restrictions), and call `AuditService` to record all state changes.

### Repository Layer
Repositories are Spring Data JPA interfaces extending `JpaRepository<Entity, ID>`. They define custom JPQL/SQL queries via `@Query` and handle all database I/O. They are stateless and contain no logic.

### Security Layer (Cross-Cutting)
Spring Security intercepts every HTTP request before it reaches controllers. `SecurityFilterChain` defines route access rules, `AppUserDetailsService` loads user details from the database, BCrypt verifies passwords, and session management limits concurrent logins to 5 per user.

---

## Domain Model (Entity Relationships)

```
AppUser
  ├── userId (PK)
  ├── username (unique, max 60 chars)
  ├── passwordHash (BCrypt, max 120 chars)
  ├── role (ROLE_ADMIN | ROLE_USER)
  ├── fullName, email
  ├── enabled (boolean — disabled users cannot log in)
  └── createdAt, lastLogin

Policy
  ├── policyId (PK)
  ├── policyName, policyHolderName
  ├── policyAmount (annual premium — basis for monthly calculation)
  ├── policyStartDate, policyEndDate
  ├── policyStatus (DRAFT | ACTIVE | EXPIRED | CANCELLED | RENEWED | SUSPENDED)
  ├── vehicleNumber, vehicleModel, vehicleYear
  ├── coverageType, deductibleAmount
  ├── holderEmail, holderPhone, holderAddress
  ├── premiumDayOfMonth (1–28, default 26)  ← LIC-style schedule field
  ├── monthlyPremium (default: policyAmount / 12)  ← LIC-style schedule field
  ├── createdAt, updatedAt, createdBy
  └── payments → List<Payment>  (OneToMany, LAZY)

Payment
  ├── paymentId (PK)
  ├── policy → Policy  (ManyToOne)
  ├── paymentAmount, paymentDate
  ├── paymentStatus (SUCCESS | FAILED | PENDING)
  ├── retryOfPaymentId (self-reference FK — links retry to original)
  ├── retryCount (increments on each retry attempt)
  └── remarks

Notification
  ├── notificationId (PK)
  ├── type (POLICY_CREATED | PAYMENT_RECEIVED | PAYMENT_FAILED |
  │         EXPIRY_WARNING | SYSTEM)
  ├── title, message
  ├── entityId, entityType (for deep-linking back to source record)
  ├── read (boolean)
  └── createdAt

AuditLog
  ├── logId (PK)
  ├── actionType (POLICY_CREATED | POLICY_UPDATED | STATUS_CHANGED |
  │               PAYMENT_MADE | PAYMENT_RETRIED | ...)
  ├── entityType, entityId
  ├── description (human-readable summary)
  ├── performedBy
  └── actionTime
```

---

## Security Architecture

### Authentication Chain

```
Request → DelegatingFilterProxy
              └── SecurityFilterChain
                    └── UsernamePasswordAuthenticationFilter (POST /login)
                          └── DaoAuthenticationProvider
                                └── AppUserDetailsService.loadUserByUsername()
                                      └── AppUserRepository.findByUsername()
                                            └── BCryptPasswordEncoder.matches()
```

### Route Authorization Rules

```java
// From SecurityConfig.java:
.requestMatchers(
    "/login", "/portal", "/portal/", "/portal/index",
    "/portal/api/**", "/portal/download-pdf",
    "/css/**", "/js/**", "/actuator/health",
    "/calculator"
).permitAll()
.requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
.anyRequest().authenticated()
```

### Session Management
- Server-side sessions (Spring default — no JWT needed for MVC apps)
- Maximum 5 concurrent sessions per user (`maximumSessions(5)`)
- Session invalidated on logout; `JSESSIONID` cookie deleted

### CSRF Configuration
CSRF protection is enabled globally. Three endpoint groups are exempt:
- `/payments/api/**` — AJAX payment endpoints (same-origin requests)
- `/portal/api/**` — Public policy lookup API (no authentication, CSRF irrelevant)
- `/premium/api/**` — Premium eligibility check API (AJAX, same-origin)

---

## LIC-Style Premium Schedule — Design Detail

`PremiumScheduleService` implements the monthly premium logic:

```
Policy
  premiumDayOfMonth = 26 (configurable, 1–28)
  monthlyPremium    = policyAmount / 12 (configurable override)

For each active policy in the target month:
  dueDate = target month's day matching premiumDayOfMonth
  premium = effectiveMonthlyPremium()
  lateFee:
    today ≤ dueDate            → ₹0
    today in (dueDate, +30d)   → ₹30   (LATE_30 tier)
    today ≥ dueDate + 30 days  → ₹100  (LATE_100 tier)
  paid = any SUCCESS payment in that year-month?

Advance payment guard:
  isAdvancePaymentAllowed(policyId) → current month fully paid?
  If not: IllegalStateException with descriptive message
```

---

## Profile-Based Configuration

```
application.properties
  spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}
        │
        ├── application-local.properties
        │     datasource.url=jdbc:h2:file:~/insurancedb
        │     ddl-auto=update   ← data PERSISTS (default local profile)
        │     h2-console=true
        │
        ├── application-h2.properties
        │     datasource.url=jdbc:h2:mem:insurancedb
        │     ddl-auto=create-drop  ← wiped on restart (CI/testing only)
        │
        ├── application-mysql.properties
        │     datasource.url=jdbc:mysql://localhost:3306/insurance_db
        │     ddl-auto=update
        │
        └── application-postgres.properties
              datasource.url=${JDBC_DATABASE_URL}
              ddl-auto=update
              HikariCP tuned for Render free tier (max pool=5)
```

---

## PDF Generation Architecture

The Customer Portal's PDF export uses **iText 5.5.13**:

```java
Document doc = new Document(PageSize.A4);
PdfWriter.getInstance(doc, response.getOutputStream());

// Styled table with policy details and payment history
PdfPTable table = new PdfPTable(4);
// ... populate cells ...

response.setContentType("application/pdf");
response.setHeader("Content-Disposition",
    "attachment; filename=policy-" + policyId + "-payments.pdf");
```

The PDF is generated in-memory and streamed directly to the HTTP response. No temporary files are written to disk. Generation time is typically under 100ms.

---

## Data Initialisation Architecture

`DataInitializer` implements `CommandLineRunner` and executes once after the full application context loads:

```java
@Override
public void run(String... args) {
    if (polRepo.count() > 0) return;  // Skip if data exists — safe for all profiles

    seedUsers();       // 4 accounts with BCrypt-hashed passwords
    seedPolicies();    // 17 policies — all 6 statuses, 60+ payments, 2+ years of history
    seedNotifications(); // 3 initial notifications
}
```

The guard `if (polRepo.count() > 0) return` ensures idempotent startup — re-seeding never occurs on a populated database, regardless of profile.

---

## Audit System Architecture

`AuditService` is a lightweight `@Service` called explicitly from service methods:

```java
// Called after every state-changing operation:
audit.log(AuditLog.ActionType.POLICY_CREATED, "Policy", savedId, description);
audit.log(AuditLog.ActionType.PAYMENT_MADE, "Payment", paymentId, description);
audit.log(AuditLog.ActionType.STATUS_CHANGED, "Policy", policyId, description);
```

Audit entries are **append-only** — never updated or deleted. They are displayed on each Policy detail page, providing a complete, ordered history of changes. `getRecentLogs()` returns the 10 most recent entries across all entities.

---

## Frontend Architecture

The frontend uses no JavaScript frameworks. The technology choices are deliberate:

| Technology | Purpose |
|-----------|---------|
| **Bootstrap 5** | Responsive grid and UI components |
| **Thymeleaf** | Server-side HTML rendering with Spring Security integration |
| **Chart.js** | Dashboard revenue bar chart |
| **Vanilla JS** (`app.js`, ~7KB) | Customer portal AJAX policy lookup, premium calculator real-time computation, notification badge updates |

This approach keeps the stack fast, easy to maintain, and easy to understand. The server renders full HTML pages — no SPA complexity, no separate API contract to maintain, no frontend build pipeline.

---

## Database Indexes

Three indexes on the `policies` table support the application's most frequent query patterns:

| Index | Column | Supports |
|-------|--------|---------|
| `idx_pol_status` | `policy_status` | Filtering by status (most common query) |
| `idx_pol_holder` | `policy_holder_name` | Name-based search |
| `idx_pol_end` | `policy_end_date` | Expiry report range queries |

One unique index on `app_users`:

| Index | Column | Supports |
|-------|--------|---------|
| `idx_user_username` | `username` (unique) | Login lookups |

---

## JVM Tuning (Docker / Render)

```dockerfile
ENTRYPOINT ["java",
  "-Xms256m",                                    # Min heap — prevents lazy allocation
  "-Xmx400m",                                    # Max heap — stays within Render 512MB RAM limit
  "-Djava.security.egd=file:/dev/./urandom",     # Faster SecureRandom — reduces startup time
  "-jar", "app.jar"]
```

---

## Future Architecture Improvements

Planned enhancements for a production V2:

1. **Email notifications** — Spring Mail for expiry warnings and payment receipts
2. **Scheduled jobs** — `@Scheduled` to auto-expire policies and send daily summaries
3. **REST API layer** — dedicated REST API for mobile app or third-party integration
4. **Role granularity** — read-only vs read-write agent permissions
5. **Document upload** — vehicle registration and licence document attachment per policy
6. **Multi-tenancy** — per-branch data isolation for SaaS deployment
