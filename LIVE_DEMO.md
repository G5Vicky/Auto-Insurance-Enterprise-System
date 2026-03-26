# 🌐 Live Demo — InsureTrack Auto Insurance Platform

## Application URL

> 🚀 **Live Application:** [https://insuretrack-enterprise.onrender.com/login](https://insuretrack-enterprise.onrender.com/login)

---

## ⚠️ Cold Start Notice

InsureTrack is hosted on **Render.com Free Tier**. When the service has been inactive for approximately **15 minutes**, Render automatically spins down the container to conserve resources.

**On first visit after a period of inactivity, expect approximately 3 minutes for the application to fully start up.**

This is expected platform behaviour — not a bug. Once the service is warm, all subsequent requests respond at normal speed.

### What to do
Simply open the URL and wait. The browser will display a loading state while the JVM initialises and the application context boots. The page will load fully once the service is ready — no action required on your part.

### Keeping the Service Warm (Optional)
To prevent cold starts during a scheduled demo or working session, configure a free [UptimeRobot](https://uptimerobot.com) monitor to ping the health endpoint every **10 minutes**:

```
https://<your-render-url>/actuator/health
```

> **Note:** This reduces cold starts during active hours but cannot eliminate them entirely on the free tier.

---

## Demo Credentials

All accounts are seeded automatically on first startup. No manual setup required.

| Role | Username | Password | Access Level |
|------|----------|----------|--------------|
| 🔴 **Admin** | `admin` | `admin123` | Full access — including User Management |
| 🟠 **Manager** | `manager` | `manager123` | Policies, Payments, Reports |
| 🟡 **Agent** | `agent` | `agent123` | Policies, Payments |
| 🟢 **User** | `Vicky` | `Vicky@123` | Policies, Payments |

---

## Feature Walkthrough

### 🏠 Dashboard — `/dashboard`

The central analytics hub. Provides a real-time operational overview including:

- Total policy count with active / expired breakdown
- Total revenue collected to date
- 12-month revenue bar chart (Chart.js)
- Policies expiring within the next 30 days

**Try this:** Log in as `admin` and observe the live KPI cards and revenue chart.

---

### 📋 Policy Management — `/policies`

Full CRUD lifecycle management for insurance policies.

**Try this:**
1. Navigate to `/policies/add`
2. Fill in holder name, vehicle details, coverage type, premium amount, and policy dates
3. Click **Save** — you will be redirected to the policy detail page
4. Scroll to the **Audit Log** at the bottom — your creation event is recorded automatically
5. Use the status dropdown to transition the policy through its lifecycle states

> Pre-seeded demo policies are available immediately after login — click any policy to view full details.

---

### 💳 Premium Payment System — `/premium`

A full LIC-style monthly premium management system.

#### Current Month Dues — `/premium`
Lists all active policies with premiums due this month. Displays policy ID, holder name, due date, base amount, applicable late fee, and current status (Paid / Overdue).

#### Advance Payment — `/premium/advance`
Shows next month's premium schedule. **Advance payment is blocked if the current month's dues remain unpaid** — a clear eligibility message is shown per policy.

#### Premium Pay Form — `/premium/pay`
Two-step guided payment form: policy lookup → payment confirmation. Includes a full breakdown of base premium, late fee, and total payable. Policy details are fetched live via AJAX.

#### Late Fee Schedule (Applied Automatically)

| Condition | Late Fee |
|-----------|----------|
| Paid on or before due date | ₹0 |
| 1–30 days overdue | ₹30 |
| More than 30 days overdue | ₹100 |

---

### 💰 Payment Processing — `/payments/make`

Simulated payment gateway with realistic outcome distribution.

**Try this:**
1. Navigate to `/payments/make`
2. Select a policy, enter an amount, and submit
3. The gateway randomly assigns an outcome: **SUCCESS** (85%) · **FAILED** (10%) · **PENDING** (5%)
4. If a payment fails, navigate to `/reports/failed` and click **[Retry]** to reprocess it

---

### 🌐 Customer Self-Service Portal — `/portal` *(No Login Required)*

A public-facing portal for policyholders to access their own policy information without an internal account.

**Try this:**
1. Open `/portal` in an incognito / private window (no credentials needed)
2. Enter any policy ID (e.g., `1`, `2`, `3`)
3. View full policy details and complete payment history
4. Click **Download PDF** to generate a formatted PDF report of the policy

---

### 🧮 Premium Calculator — `/calculator` *(No Login Required)*

Interactive premium estimation tool — no login required.

**Try this:**
1. Navigate to `/calculator`
2. Select vehicle model year, coverage type (Basic / Comprehensive / Third-Party), and deductible amount
3. Observe the estimated annual premium update in real time — no server round-trip

---

### 📊 Reports

| Report | URL | Description |
|--------|-----|-------------|
| Expiry Report | `/reports/expiry` | Policies expiring within the next 30 days, sorted by end date |
| Revenue Report | `/reports/revenue` | Monthly revenue breakdown with bar chart and data table |
| Failed Payments | `/reports/failed` | All failed payments with one-click retry functionality |

---

### 🔔 Notifications — `/notifications`

Every significant system event (policy created, payment processed, status changed) generates a notification. The navbar bell icon displays the live unread count badge.

---

### 👥 User Management — `/admin/users` *(Admin Only)*

**Try this:**
1. Log in as `admin`
2. Navigate to `/admin/users`
3. Create a new user, modify a role, or toggle an account's enabled status

---

### ❤️ Health Check Endpoint — `/actuator/health`

Public monitoring endpoint for deployment health checks and uptime monitors:

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

## Deployment Details

| Property | Value |
|----------|-------|
| Platform | Render.com (PaaS — Free Tier) |
| Database | PostgreSQL (Render Managed) |
| Spring Profile | `postgres` |
| Port | Auto-injected by Render (`$PORT`) |
| Memory | 256 MB min / 400 MB max (JVM tuned) |
| Container | Docker — multi-stage build (Maven → JRE) |
| Security | Non-root user (`appuser`) inside container |

---

## Demo Seed Data

On first startup, `DataInitializer.java` automatically seeds the following data — the demo is immediately usable with no manual entry required:

- **4 user accounts** — admin, manager, agent, Vicky
- **Multiple demo policies** — covering all lifecycle statuses (Active, Expired, Cancelled)
- **Sample payments** — a mix of SUCCESS, FAILED, and PENDING outcomes
- **Sample notifications** — pre-populated for immediate review

---

*InsureTrack v2.1 — Built with Spring Boot 3.3 · Thymeleaf · JPA · PostgreSQL*
