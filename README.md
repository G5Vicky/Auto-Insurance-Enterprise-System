# 🛡️ InsureTrack — Auto Insurance Enterprise Platform

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?logo=springboot)
![Java](https://img.shields.io/badge/Java-17-orange?logo=java)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supported-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Ready-blue?logo=docker)
![Deployed](https://img.shields.io/badge/Status-Live%20%26%20Deployed-success)
![License](https://img.shields.io/badge/License-MIT-lightgrey)

**A production-grade Auto Insurance Management System built with Spring Boot 3, Spring Security, Thymeleaf, and multi-database support. Features a LIC-style premium payment engine, simulated payment gateway, real-time analytics, and a public customer portal. Fully deployed and live on Render.com.**

[🚀 Live Demo](#-live-demo) • [📖 Docs](#-documentation) • [⚙️ Setup](INSTALLATION.md) • [🔄 Process Flow](PROCESS_FLOW.md)

</div>

---

## 🌐 Live Demo

> **Application URL:** [https://insuretrack-enterprise.onrender.com/login](https://insuretrack-enterprise.onrender.com/login)
> **Customer Portal (Public — No Login):** [https://insuretrack-enterprise.onrender.com/portal](https://insuretrack-enterprise.onrender.com/portal)

> ⚠️ **Cold Start Notice:** Hosted on Render.com free tier. If the service has been inactive for ~15 minutes, the first request will take approximately **3 minutes** to start up. This is expected platform behaviour — not a bug. Simply wait for the page to load.

| Role | Username | Password | Access |
|------|----------|----------|--------|
| 🔴 Administrator | `admin` | `admin123` | Full access + User Management |
| 🟠 Ops Manager | `manager` | `manager123` | Policies, Payments, Reports |
| 🟡 Insurance Agent | `agent` | `agent123` | Policies, Payments |
| 🟢 Custom User | `Vicky` | `Vicky@123` | Policies, Payments |

---

## 🎯 What This Project Does

InsureTrack is a **full-stack enterprise insurance management system** that handles the complete lifecycle of auto insurance policies — from creation and premium scheduling to payment processing, expiry reporting, and customer self-service. It mirrors the workflows used by real mid-size insurance companies, including a LIC-style monthly premium engine with late fee escalation and advance payment eligibility enforcement.

### Key Highlights

- 🏢 **Multi-role RBAC** with Spring Security — Admin, Manager, Agent, and User roles with route-level enforcement
- 💳 **LIC-style Premium Payment Engine** — monthly dues, configurable due day per policy, automatic late fees (₹30 / ₹100), advance payment guard
- 💰 **Simulated Payment Gateway** — realistic outcome distribution (85% SUCCESS / 10% FAILED / 5% PENDING) with full retry workflow
- 📊 **Live Analytics Dashboard** — KPI cards, 12-month revenue bar chart, and expiry alerts powered by Chart.js
- 🌐 **Public Customer Portal** — policy lookup and PDF download with zero login required
- 🧮 **Premium Calculator** — real-time premium estimation with no server round-trip
- 📋 **Immutable Audit Trail** — every state-changing action logged with timestamp and description
- 🔔 **Notification Center** — bell icon with live unread count badge for policy and payment events
- 📄 **PDF Generation** via iText 5 — downloadable payment history reports streamed directly to browser
- 🐳 **Docker + Render.com** — multi-stage Docker build with non-root container security, deployed on Render free tier
- 💾 **Persistent Local Storage** — file-based H2 default profile; data survives restarts with zero database setup

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENT BROWSER                          │
│         Thymeleaf + Bootstrap 5 + Vanilla JS + Chart.js      │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/HTTPS
┌────────────────────────▼────────────────────────────────────┐
│                   SPRING BOOT APPLICATION                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │  Controllers  │  │   Services   │  │   Repositories   │  │
│  │  (10 modules) │  │  (Business   │  │  (Spring Data    │  │
│  │              │  │   Logic)     │  │     JPA)         │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐   │
│  │            Spring Security (BCrypt + Sessions)        │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │     AuditService + NotificationService (Cross-Cut)    │   │
│  └──────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         │ JPA / Hibernate / HikariCP
┌────────────────────────▼────────────────────────────────────┐
│              DATABASE LAYER (Profile-Based)                  │
│  H2 file (Local) │ H2 mem (CI) │ MySQL (Staging) │ PG (Prod) │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend Framework | Spring Boot 3.3.5 |
| Language | Java 17 |
| Security | Spring Security 6 + BCrypt (cost factor 10) |
| Template Engine | Thymeleaf 3 + Thymeleaf Security Extras |
| ORM | Spring Data JPA / Hibernate |
| PDF Generation | iText 5.5.13 |
| Databases | H2 file (local), H2 mem (CI), MySQL 8 (staging), PostgreSQL (prod) |
| Build Tool | Maven 3.9 |
| Containerization | Docker multi-stage build (Maven → JRE-only image) |
| Deployment | Render.com (PaaS — free tier) |
| Frontend | Bootstrap 5 + Vanilla JS + Chart.js |
| Connection Pool | HikariCP (tuned per profile) |

---

## 🗂️ Project Structure

```
autoinsurance/
├── src/main/java/com/insurance/autoinsurance/
│   ├── AutoInsuranceApplication.java           # Spring Boot entry point
│   ├── audit/
│   │   └── AuditService.java                   # Append-only action logger
│   ├── config/
│   │   ├── DataInitializer.java                # Demo data seeder (CommandLineRunner)
│   │   └── SecurityConfig.java                 # Auth, route protection, CSRF config
│   ├── controller/
│   │   ├── AdminController.java                # User management (ROLE_ADMIN only)
│   │   ├── AuthController.java                 # Login / logout
│   │   ├── CustomerPortalController.java       # Public portal + PDF download
│   │   ├── DashboardController.java            # Analytics dashboard
│   │   ├── NotificationController.java         # Notification centre
│   │   ├── PaymentController.java              # Payment CRUD + retry
│   │   ├── PolicyController.java               # Policy CRUD + status transitions
│   │   ├── PremiumCalculatorController.java    # Public premium estimator
│   │   ├── PremiumController.java              # LIC-style premium payment pages
│   │   └── ReportsController.java              # Expiry, revenue, failed reports
│   ├── dto/
│   │   ├── DashboardStats.java                 # Analytics DTO
│   │   ├── PaymentDetailsView.java             # Payment projection
│   │   └── PremiumDueDTO.java                  # Monthly premium due record
│   ├── exception/
│   │   └── GlobalExceptionHandler.java         # Centralised error handling
│   ├── model/
│   │   ├── AppUser.java                        # User entity (ROLE_ADMIN / ROLE_USER)
│   │   ├── AuditLog.java                       # Audit trail entity
│   │   ├── Notification.java                   # Notification entity
│   │   ├── Payment.java                        # Payment entity + retry chain
│   │   └── Policy.java                         # Core policy entity + premium schedule fields
│   ├── repository/                             # Spring Data JPA interfaces (5 repositories)
│   ├── security/
│   │   └── AppUserDetailsService.java          # Spring Security UserDetailsService
│   └── service/
│       ├── NotificationService.java            # Notification push + read tracking
│       ├── PaymentService.java                 # Gateway simulation + retry logic
│       ├── PolicyService.java                  # Policy lifecycle management
│       └── PremiumScheduleService.java         # LIC-style premium engine + late fees
├── src/main/resources/
│   ├── application.properties                  # Master config — default profile: local
│   ├── application-local.properties            # File-based H2 (data persists — default)
│   ├── application-h2.properties               # In-memory H2 (CI / testing only)
│   ├── application-mysql.properties            # MySQL 8 staging profile
│   ├── application-postgres.properties         # PostgreSQL production profile (Render)
│   ├── static/css/app.css                      # Custom stylesheet (~52 KB)
│   ├── static/js/app.js                        # Portal AJAX, calculator, notification badge
│   └── templates/
│       ├── admin/                              # User management views
│       ├── auth/                               # Login page
│       ├── fragments/                          # Navbar + footer partials
│       ├── notifications/                      # Notification centre list
│       ├── payments/                           # Payment CRUD views
│       ├── policies/                           # Policy CRUD + detail + audit log
│       ├── portal/                             # Public customer self-service portal
│       ├── premium/                            # Current dues, advance payment, pay form
│       ├── reports/                            # Expiry, revenue, failed reports
│       ├── tools/                              # Premium calculator
│       └── dashboard.html                      # Main analytics dashboard
├── Dockerfile                                  # Multi-stage production Docker build
├── pom.xml                                     # Maven dependencies
├── README.md
├── INSTALLATION.md
├── LIVE_DEMO.md
├── PROCESS_FLOW.md
└── docs/
    ├── architecture.md
    ├── deployment_guide.md
    ├── debugging_guide.md
    ├── project_workflow.md
    ├── setup_guide.md
    └── system_design.md
```

---

## ✨ Feature Modules

### 1. Policy Management — `/policies`

Full CRUD with 6 lifecycle states: `DRAFT → ACTIVE → EXPIRED / CANCELLED / SUSPENDED / RENEWED`. Stores vehicle details (number, model, year), coverage type, deductible, and holder contact info. Every status transition is written to the immutable audit log. Policy form includes configurable **monthly premium due day** and **monthly premium amount** for the LIC schedule.

### 2. LIC-Style Premium Payment System — `/premium`

A complete monthly premium management engine modelled on real insurance workflows.

- **Current Month Dues** (`/premium`) — lists all active policies with due date, base premium, applicable late fee, and paid/overdue status. One-click payment recording.
- **Advance Payment** (`/premium/advance`) — shows next-month schedule. Advance payment is blocked per-policy if the current month's dues are unpaid, with a clear eligibility message.
- **Premium Pay Form** (`/premium/pay`) — two-step guided form with live AJAX policy lookup, showing base premium + late fee + total payable.

**Late Fee Schedule (applied automatically):**

| Condition | Late Fee |
|-----------|----------|
| Paid on or before due date | ₹0 |
| 1–30 days overdue | ₹30 |
| More than 30 days overdue | ₹100 |

### 3. Payment Processing — `/payments`

Simulated payment gateway with realistic probability distribution. Supports full payment CRUD, payment history per policy, and one-click retry of failed payments. Each retry creates a new linked payment record with an incremented `retryCount` and a `retryOfPaymentId` reference to the original.

**Gateway outcome distribution:** SUCCESS 85% · FAILED 10% · PENDING 5%

### 4. Analytics Dashboard — `/dashboard`

Real-time operational overview with live data:
- Total policies, active / expired / failed payment counts
- Total revenue collected
- 12-month revenue bar chart (Chart.js)
- Policies expiring within the next 30 days (alert table)

### 5. Reports — `/reports`

| Report | URL | Description |
|--------|-----|-------------|
| Expiry Report | `/reports/expiry` | Policies expiring within next 30 days, sorted by end date |
| Revenue Report | `/reports/revenue` | Monthly revenue breakdown with bar chart and data table |
| Failed Payments | `/reports/failed` | All failed payments with one-click retry |

### 6. Customer Self-Service Portal — `/portal` *(No Login Required)*

Public-facing portal for policyholders. Enter any Policy ID to view full policy details, complete payment history, and total amount paid. One-click **Download PDF** generates a professionally formatted iText report streamed directly to the browser — no temporary files.

### 7. Premium Calculator — `/calculator` *(No Login Required)*

Dynamic premium estimation tool. Select vehicle model year, coverage type (Basic / Comprehensive / Third-Party / EV Comprehensive / Zero Dep), and deductible amount. Result updates in real time via JavaScript — no server round-trip required.

### 8. Notification Center — `/notifications`

System-wide event notifications for policy creation, renewal, payment receipt, payment failure, and expiry warnings. The navbar bell icon shows the live unread count badge. Mark-all-read clears the badge in one click.

### 9. Audit Trail

Every create, update, delete, status change, payment, and retry action is written to an append-only `AuditLog` table. Entries are visible on each Policy detail page, providing a complete ordered history of all changes to that policy.

### 10. User Management — `/admin/users` *(Admin Only)*

Admin-only CRUD for application users. Create users, assign roles (`ROLE_ADMIN` / `ROLE_USER`), and enable or disable accounts. Disabled accounts cannot log in.

---

## 💾 Demo Seed Data

On first startup, `DataInitializer` automatically seeds the database — no manual setup required:

| Data | Count | Details |
|------|-------|---------|
| Users | 4 | admin, manager, agent, Vicky — all with BCrypt-hashed passwords |
| Policies | 17 | All 6 statuses: ACTIVE, DRAFT, EXPIRED, CANCELLED, RENEWED, SUSPENDED |
| Expiring Soon | 8 | Active policies expiring within the next 30–220 days |
| Payments | 60+ | Mixed SUCCESS / FAILED / PENDING across 2+ years of history |
| Notifications | 3 | Expiry warning, payment failed alert, system init message |

Seeding is idempotent — it is skipped automatically if the database already contains data.

---

## 🚀 Quick Start (Local)

```bash
# 1. Clone
git clone https://github.com/G5Vicky/Auto-Insurance-Enterprise-System
cd auto-insurance-enterprise-system

# 2. Build and run (default: file-based H2 — data persists across restarts)
mvn clean install
mvn spring-boot:run

# 3. Open
# http://localhost:8080
# Login: admin / admin123
```

Data is stored at `~/insurancedb.mv.db`. Restarting the app will not lose any data.

> Full installation guide with MySQL, PostgreSQL, and Render deployment steps: [INSTALLATION.md](INSTALLATION.md)

---

## 🐳 Docker

```bash
# Build image
docker build -t insuretrack .

# Run with local H2 (default)
docker run -p 8080:8080 insuretrack

# Run with postgres profile
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=postgres \
  -e JDBC_DATABASE_URL=jdbc:postgresql://<host>/<db>?sslmode=require&user=<u>&password=<p> \
  insuretrack
```

The Docker image uses a **multi-stage build** (Maven → JRE-only) and runs as a **non-root user** (`appuser`) for security. JVM is tuned with `-Xms256m -Xmx400m` for Render's free tier RAM limits.

---

## ☁️ Render.com Deployment

Set these environment variables in your Render web service:

| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `postgres` |
| `JDBC_DATABASE_URL` | *(Render Internal PostgreSQL URL)* |

> ⚠️ **Cold Start:** Render free tier spins down after ~15 minutes of inactivity. First access after inactivity takes **approximately 3 minutes**. Use [UptimeRobot](https://uptimerobot.com) to ping `/actuator/health` every 10 minutes to keep the service warm during working hours.

Full deployment walkthrough: [docs/deployment_guide.md](docs/deployment_guide.md)

---

## 🔑 Spring Profiles

| Profile | Database | Default? | Use Case |
|---------|----------|----------|----------|
| `local` | File-based H2 | ✅ Yes | Local development — data persists |
| `h2` | In-memory H2 | No | CI / automated testing only |
| `mysql` | MySQL 8.0 | No | Local production-like staging |
| `postgres` | PostgreSQL | No | Render.com production deployment |

---

## 🔐 Security Design

- **BCrypt** password hashing (cost factor 10) — plain text never stored or logged
- **Session-based auth** — appropriate for server-rendered MVC (no JWT complexity)
- **CSRF protection** — enabled globally; exempt only for AJAX endpoints (`/payments/api/**`, `/portal/api/**`, `/premium/api/**`)
- **Route authorization** — `/admin/**` restricted to `ROLE_ADMIN`; all other routes require authentication; `/portal/**`, `/calculator`, `/actuator/health` are public
- **Max 5 concurrent sessions** per user; session invalidated and cookie deleted on logout
- **Non-root Docker container** — application runs as `appuser` inside the container

---

## 🏥 Health Check

```bash
curl https://insuretrack-enterprise.onrender.com/actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [INSTALLATION.md](INSTALLATION.md) | Local setup for all profiles (H2, MySQL, PostgreSQL) |
| [PROCESS_FLOW.md](PROCESS_FLOW.md) | All 12 business process flow diagrams |
| [LIVE_DEMO.md](LIVE_DEMO.md) | Deployed URL, credentials, and feature walkthrough |
| [docs/setup_guide.md](docs/setup_guide.md) | Detailed developer environment setup |
| [docs/deployment_guide.md](docs/deployment_guide.md) | Docker + Render.com deployment guide |
| [docs/architecture.md](docs/architecture.md) | Layered architecture and component deep-dive |
| [docs/system_design.md](docs/system_design.md) | Database schema and design decision rationale |
| [docs/project_workflow.md](docs/project_workflow.md) | Git workflow, coding patterns, and feature extension guide |
| [docs/debugging_guide.md](docs/debugging_guide.md) | Comprehensive troubleshooting reference |

---

## 👤 Author

**Gurrapu Vigneshwar**
Full-Stack Java Developer
📧 gurrapuvigneshwar0056@gmail.com
🔗 [LinkedIn](https://www.linkedin.com/in/gurrapu-vigneshwar-95b26b265/) | [GitHub](https://github.com/G5Vicky)

---

<div align="center">
⭐ If this project helped you, please give it a star!
</div>
