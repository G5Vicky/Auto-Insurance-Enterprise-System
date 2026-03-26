# 🚗 InsureTrack — Auto Insurance Enterprise Platform

> **Spring Boot 3.3 · Thymeleaf · Spring Security · JPA / Hibernate · PostgreSQL / H2 / MySQL**

A full-featured, production-ready auto insurance management system. Built with a layered MVC architecture, InsureTrack covers the complete policy lifecycle, a LIC-style monthly premium payment engine, simulated payment gateway with retry logic, real-time analytics dashboard, and a public customer self-service portal — all deployable via Docker to Render.com in under 10 minutes.

---

## What's in v2.1

- **Persistent local storage fix** — default profile is now `local` (file-based H2), so data survives restarts. The old `h2` in-memory profile is retained for CI/testing only.
- **LIC-style Premium Payment System** — full monthly premium scheduling per policy, with configurable due day, automatic late fee escalation, and advance payment eligibility enforcement.
- **Premium Schedule per Policy** — each policy carries a configurable monthly due day (default: 26th) and a fixed monthly premium amount (default: annual premium ÷ 12).
- **Advance Payment Guard** — advance payment to next month is blocked if the current month's premium remains unpaid, with a clear per-policy eligibility message.
- **Rich demo seed data** — 17 policies spanning all 6 lifecycle statuses, 8 expiring-soon policies, and 60+ payments with mixed outcomes across 2+ years of history.

---

## Quick Start — Local Development

### Prerequisites

| Tool | Minimum Version |
|------|----------------|
| Java | 17 |
| Maven | 3.8 |
| Git | any |
| MySQL (optional) | 8.0+ |

### Run Locally (zero setup — file-based H2)

```bash
git clone <repo-url>
cd Auto-Insurance-Enterprise-System
mvn spring-boot:run
```

Open: [http://localhost:8080](http://localhost:8080)

Data is stored at `~/insurancedb.mv.db` and **persists across restarts**. No database installation required.

### Run with MySQL

```bash
# 1. Edit src/main/resources/application-mysql.properties
#    Set spring.datasource.password=YOUR_MYSQL_PASSWORD_HERE

# 2. Start with MySQL profile
SPRING_PROFILES_ACTIVE=mysql mvn spring-boot:run
```

### H2 Web Console (local profile only)

```
URL:      http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:~/insurancedb
Username: sa
Password: (leave blank)
```

---

## Default Login Credentials

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | Administrator — full access |
| `manager` | `manager123` | Staff |
| `agent` | `agent123` | Staff |
| `Vicky` | `Vicky@123` | Staff |

All passwords are stored as BCrypt hashes. Never plain text.

---

## Feature Overview

### Policy Management — `/policies`
Full CRUD for insurance policies. Supports 6 lifecycle statuses: `DRAFT`, `ACTIVE`, `EXPIRED`, `CANCELLED`, `RENEWED`, `SUSPENDED`. Every status change is written to an immutable audit log visible on the policy detail page.

### LIC-Style Premium Payment System

#### Current Month Dues — `/premium`
Lists all active policies with their current-month premium status. Displays policy ID, holder name, due date, base premium amount, applicable late fee, and paid/overdue status. One-click payment recording.

#### Advance Payment — `/premium/advance`
Shows next-month premium schedule. Advance payment is blocked per-policy if the current month's dues are unpaid. A clear eligibility message is shown for each blocked policy.

#### Premium Pay Form — `/premium/pay`
Two-step guided form: policy lookup (with live AJAX detail fetch) → payment confirmation showing base premium + late fee + total. Supports both current-month and advance payment modes.

#### Late Fee Schedule

| Condition | Late Fee |
|-----------|----------|
| Paid on or before due date | ₹0 |
| 1–30 days overdue | ₹30 |
| More than 30 days overdue | ₹100 |

### Payment Processing — `/payments/make`
Simulated payment gateway with realistic outcome distribution: SUCCESS (85%), FAILED (10%), PENDING (5%). Failed payments are retried from `/reports/failed`. Each retry creates a new linked payment record with an incremented retry count.

### Customer Self-Service Portal — `/portal` *(No Login Required)*
Public portal for policyholders to look up their own policy by ID, view payment history, and download a formatted PDF report — all without an account.

### Premium Calculator — `/calculator` *(No Login Required)*
Real-time premium estimator. Inputs: vehicle model year, coverage type, deductible amount. Result updates instantly in the browser via JavaScript — no server round-trip.

### Dashboard — `/dashboard`
Central analytics hub: total policies, active/expired breakdown, total revenue, 12-month revenue bar chart (Chart.js), and policies expiring within the next 30 days.

### Reports
- **Expiry Report** `/reports/expiry` — policies expiring within 30 days, sorted by end date
- **Revenue Report** `/reports/revenue` — monthly revenue breakdown with bar chart and data table
- **Failed Payments** `/reports/failed` — all failed payments with one-click retry

### Notifications — `/notifications`
Every significant event (policy created, payment made, status changed) generates a notification. The navbar bell icon shows the live unread count badge.

### User Management — `/admin/users` *(Admin Only)*
Create users, modify roles (`ROLE_ADMIN` / `ROLE_USER`), and enable/disable accounts.

### Audit Trail
Every state-changing operation (policy create/update/delete, payment, status change) is recorded in an append-only `AuditLog`. Viewable on each policy's detail page.

### Health Check — `/actuator/health`
Public monitoring endpoint. Returns `{"status":"UP"}` with database and disk space component breakdown.

---

## Architecture Overview

```
Browser → Spring MVC Controllers → Service Layer → JPA Repositories → Database
                 ↑                       ↓
          Thymeleaf Templates      AuditService (cross-cutting)
          Spring Security          NotificationService
```

### Spring Profiles

| Profile | Database | Default? | Use Case |
|---------|----------|----------|----------|
| `local` | File-based H2 | ✅ Yes | Local development — data persists |
| `h2` | In-memory H2 | No | CI / automated testing only |
| `mysql` | MySQL 8.0 | No | Local production-like environment |
| `postgres` | PostgreSQL | No | Render.com production deployment |

---

## Render.com Deployment

### Required Environment Variables

```
SPRING_PROFILES_ACTIVE = postgres
JDBC_DATABASE_URL       = jdbc:postgresql://<host>/<db>?sslmode=require&user=<user>&password=<pass>
```

### Cold Start Behaviour

Render free tier spins down inactive services after approximately 15 minutes. On first access after inactivity, expect **approximately 3 minutes** for the service to restart and the JVM to initialise.

**Mitigation:** Use [UptimeRobot](https://uptimerobot.com) (free plan) to ping `/actuator/health` every 10 minutes — this keeps the service warm during working hours.

---

## Project Structure

```
src/main/java/com/insurance/autoinsurance/
├── config/          SecurityConfig.java, DataInitializer.java
├── controller/      Dashboard, Policy, Payment, Premium, CustomerPortal,
│                    PremiumCalculator, Reports, Notifications, Admin, Auth
├── dto/             DashboardStats, PaymentDetailsView, PremiumDueDTO
├── model/           Policy, Payment, AppUser, Notification, AuditLog
├── repository/      JPA repositories for all entities
├── service/         PolicyService, PaymentService, PremiumScheduleService,
│                    NotificationService
├── audit/           AuditService
├── exception/       GlobalExceptionHandler
└── security/        AppUserDetailsService

src/main/resources/
├── application.properties                 Active profile selector (default: local)
├── application-local.properties           File-based H2 — data persists
├── application-h2.properties             In-memory H2 — testing only
├── application-mysql.properties          MySQL configuration
├── application-postgres.properties       PostgreSQL / Render deployment
└── templates/
    ├── auth/login.html
    ├── dashboard.html
    ├── fragments/navbar.html, footer.html
    ├── policies/list, form, detail
    ├── payments/list, make, detail, byPolicy
    ├── premium/current, advance, pay        ← LIC premium system
    ├── reports/expiry, revenue, failed
    ├── admin/users, userform
    ├── notifications/list
    ├── portal/index                         ← Public customer portal
    └── tools/calculator                     ← Public premium calculator
```

---

## Validation Checklist

- [x] App starts → DB connects → seed data inserted (first run only)
- [x] Add policy → restart → policy still present (local profile)
- [x] Premium dues page lists all active policies for current month
- [x] Late fee calculated and displayed automatically
- [x] Advance payment blocked when current month dues are unpaid
- [x] Advance payment succeeds after current month is cleared
- [x] Failed payment appears in `/reports/failed` with Retry button
- [x] Retry creates a new linked payment record
- [x] Customer portal accessible without login
- [x] PDF download generates correctly from portal
- [x] `/admin/users` returns 403 for non-admin users
- [x] `/actuator/health` returns `{"status":"UP"}`
- [x] Audit log entries appear on policy detail page
