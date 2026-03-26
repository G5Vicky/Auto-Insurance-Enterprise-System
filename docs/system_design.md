# 🧠 System Design — InsureTrack Auto Insurance Platform

Design decisions, database schema, and engineering rationale.

---

## Design Principles

### 1. Convention Over Configuration
InsureTrack uses Spring Boot's defaults wherever possible. No XML configuration. No manual bean wiring. The codebase is idiomatic for any Spring developer and requires zero framework-specific onboarding.

### 2. Profile-Based Multi-Database Support
The same application code runs against H2 (local), MySQL (staging), and PostgreSQL (production). Spring profiles and Hibernate's dialect abstraction handle all differences transparently. Switching environments requires only an environment variable change.

### 3. Thin Controllers, Rich Services
Controllers do three things only: receive input, call a service, return a view or redirect. All business rules live in services. Services are independently testable without an HTTP server.

### 4. Explicit Audit Trail
Every state change is written to an append-only `AuditLog`. This is non-negotiable for insurance systems where compliance and traceability are regulatory requirements. The audit log is never modified and is visible on each policy's detail page.

### 5. LIC-Style Premium Discipline
The premium system enforces real-world insurance rules: fixed monthly schedule, escalating late fees, and advance payment eligibility gated on current dues being cleared. The business logic lives entirely in `PremiumScheduleService`, keeping it portable and testable.

### 6. Realistic Payment Simulation
The gateway simulation (85% SUCCESS / 10% FAILED / 5% PENDING) produces believable mixed results in demos. It demonstrates the full retry workflow, audit trail, and failed payment report without requiring a real payment provider or test credentials.

---

## Database Schema

### `app_users` Table

```sql
CREATE TABLE app_users (
    user_id       BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(60)  NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    full_name     VARCHAR(120),
    email         VARCHAR(200),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP,
    last_login    TIMESTAMP
);
-- Index: idx_user_username (unique)
```

### `policies` Table

```sql
CREATE TABLE policies (
    policy_id              BIGINT       PRIMARY KEY AUTO_INCREMENT,
    policy_name            VARCHAR(120) NOT NULL,
    policy_holder_name     VARCHAR(150) NOT NULL,
    policy_amount          DOUBLE       NOT NULL,   -- Annual premium
    policy_start_date      DATE         NOT NULL,
    policy_end_date        DATE         NOT NULL,
    policy_status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    vehicle_number         VARCHAR(50),
    vehicle_model          VARCHAR(100),
    vehicle_year           INT,
    coverage_type          VARCHAR(50),
    deductible_amount      DOUBLE,
    holder_email           VARCHAR(200),
    holder_phone           VARCHAR(15),
    holder_address         VARCHAR(300),
    premium_day_of_month   INT,        -- Day of month premium is due (1–28, default 26)
    monthly_premium        DOUBLE,     -- Fixed monthly amount (default: policy_amount / 12)
    created_at             TIMESTAMP,
    updated_at             TIMESTAMP,
    created_by             VARCHAR(100)
);
-- Indexes: idx_pol_status, idx_pol_holder, idx_pol_end
```

### `payments` Table

```sql
CREATE TABLE payments (
    payment_id          BIGINT      PRIMARY KEY AUTO_INCREMENT,
    policy_id           BIGINT      NOT NULL REFERENCES policies(policy_id),
    payment_amount      DOUBLE,
    payment_date        DATE,
    payment_status      VARCHAR(20) NOT NULL,   -- SUCCESS | FAILED | PENDING
    retry_of_payment_id BIGINT,                  -- Self-reference for retries
    retry_count         INT         DEFAULT 0,
    remarks             VARCHAR(300)
);
```

### `notifications` Table

```sql
CREATE TABLE notifications (
    notification_id BIGINT        PRIMARY KEY AUTO_INCREMENT,
    type            VARCHAR(50),   -- POLICY_CREATED | PAYMENT_RECEIVED | PAYMENT_FAILED |
                                   --   EXPIRY_WARNING | SYSTEM
    title           VARCHAR(200),
    message         TEXT,
    entity_id       BIGINT,
    entity_type     VARCHAR(50),
    is_read         BOOLEAN        DEFAULT FALSE,
    created_at      TIMESTAMP
);
```

### `audit_logs` Table

```sql
CREATE TABLE audit_logs (
    log_id        BIGINT      PRIMARY KEY AUTO_INCREMENT,
    action_type   VARCHAR(50), -- POLICY_CREATED | POLICY_UPDATED | STATUS_CHANGED |
                               --   PAYMENT_MADE | PAYMENT_RETRIED | ...
    entity_type   VARCHAR(50),
    entity_id     BIGINT,
    description   TEXT,
    performed_by  VARCHAR(100),
    action_time   TIMESTAMP
);
```

---

## Key Design Decisions

### Why File-Based H2 as the Default Local Profile?
The previous default was in-memory H2 (`ddl-auto=create-drop`), which wiped all data on every restart. This made local development painful. The `local` profile now uses `jdbc:h2:file:~/insurancedb` with `ddl-auto=update` — data persists indefinitely between restarts, matching the behaviour developers expect.

### Why H2 for Development at All?
H2 embedded database requires zero installation. New developers can clone and run in under 2 minutes. The `local` (file-based) profile provides persistence while keeping the zero-setup advantage. The `h2` (in-memory) profile remains for CI pipelines where a clean database on each run is desirable.

### Why PostgreSQL for Production?
PostgreSQL is the industry standard for production Java applications on modern PaaS platforms. Render.com offers managed PostgreSQL on the free tier. It supports all Hibernate features used by the project, is ACID-compliant, and handles concurrent connections robustly.

### Why Thymeleaf Instead of React or Vue?
For a CRUD enterprise application with server-managed authentication:
- Server-side rendering is faster to build, simpler to secure, and easier to reason about
- Thymeleaf integrates natively with Spring Security via `sec:authorize` and `th:action` (automatic CSRF token injection)
- No separate REST API layer is needed — controllers return models directly to templates
- Single deployable JAR — no frontend build pipeline or CDN required
- The target audience (insurance staff on desktop browsers) has no need for SPA behaviour

### Why a Simulated Payment Gateway?
Integrating a real payment provider (Stripe, Razorpay) would require account setup, API keys, webhook handlers, test card numbers, and async status reconciliation. For an enterprise portfolio project, the simulated gateway demonstrates identical concepts — status handling, retry logic, audit trail, revenue reporting — without the integration overhead.

### Why iText for PDF?
iText 5.5.13 is the most mature Java PDF library. It provides full programmatic layout control, table support for payment histories, and direct HTTP response streaming with no temporary files. PDF generation completes in under 100ms for typical policy histories.

### Why BCrypt for Passwords?
BCrypt is Spring Security's default and the industry standard:
- Adaptive cost factor — can be increased as hardware improves
- Built-in per-password salt — immune to rainbow table attacks
- `BCryptPasswordEncoder` uses cost factor 10 by default (computationally expensive to brute-force)

### Why Session Cookies Instead of JWT?
This is a server-rendered MVC application, not a stateless REST API. JWT is designed for stateless APIs consumed by mobile apps or SPAs. For Thymeleaf:
- Session cookies are simpler and equally secure
- CSRF protection integrates naturally with session-based auth
- No token refresh logic or client-side token storage to manage
- `sec:authorize` in Thymeleaf works natively with the session security context

### Why the Builder Pattern in Entities?
Domain models (`Policy`, `AppUser`, `Payment`, `Notification`, `AuditLog`) implement static inner `Builder` classes. This makes `DataInitializer` code readable (`Policy.builder().policyName("X").coverageType("Comprehensive").build()`), eliminates constructor parameter ordering errors, and makes test data construction expressive.

### Why `premiumDayOfMonth` and `monthlyPremium` on Policy?
Rather than calculating the schedule at query time from the annual premium and a fixed global rule, these fields are stored per-policy. This supports real-world scenarios where:
- Different policyholders have different payment dates set at enrollment
- A policy's monthly amount may differ from `policyAmount / 12` (e.g., after a policy amendment)
- `effectiveMonthlyPremium()` provides a null-safe fallback to `policyAmount / 12` if the field is unset

---

## Performance Considerations

### HikariCP Connection Pool Tuning

Production (PostgreSQL on Render free tier — 512MB RAM):
```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

Local (file-based H2):
```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=20000
```

### Lazy Loading
`Policy.payments` uses `FetchType.LAZY`. Payments are loaded only when explicitly accessed (e.g., on the policy detail page or in the portal), not when listing all policies.

### JVM Heap Limits
Docker container is tuned with `-Xms256m -Xmx400m` to stay within Render's 512MB free tier RAM limit while leaving headroom for the OS and Metaspace.

### Static Asset Caching
`spring.thymeleaf.cache=false` in development (set in `application.properties`). In production this should be set to `true` to enable template caching and reduce render overhead.
