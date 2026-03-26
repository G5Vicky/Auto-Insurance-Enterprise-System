# 🔄 Process Flow — InsureTrack Auto Insurance Platform

All major business flows in the system, documented with step-by-step diagrams.

---

## 1. User Authentication Flow

```
Browser                  Spring Security              Database
   │                          │                          │
   │──── POST /login ─────────▶│                          │
   │    (username, password)   │                          │
   │                          │──── loadUserByUsername ──▶│
   │                          │◀─── AppUser entity ───────│
   │                          │                          │
   │                          │  BCrypt.matches()         │
   │                          │  (password vs stored hash)│
   │                          │                          │
   │◀─── Redirect /dashboard ──│  (match: SUCCESS)        │
   │  OR Redirect /login?error │  (no match: FAIL)        │
   │                          │                          │
   │  Session cookie (JSESSIONID) stored in browser       │
```

**Key Points:**
- Passwords stored exclusively as BCrypt hashes — plain text is never stored or logged
- Maximum 5 concurrent sessions per user
- Session invalidated on logout; `JSESSIONID` cookie deleted
- CSRF protection enabled for all state-changing requests
- `/premium/api/**` and `/portal/api/**` are CSRF-exempt (AJAX endpoints)

---

## 2. Policy Lifecycle Flow

```
          ┌──────────┐
          │  DRAFT   │  ◄── Initial state (agent saves before activating)
          └────┬─────┘
               │ Activate
          ┌────▼─────┐
          │  ACTIVE  │  ◄── Normal running state; premium dues are tracked here
          └────┬─────┘
               │
     ┌─────────┼──────────┬───────────────┐
     ▼         ▼          ▼               ▼
┌─────────┐ ┌──────────┐ ┌────────────┐ ┌──────────┐
│ EXPIRED │ │CANCELLED │ │ SUSPENDED  │ │ RENEWED  │
└─────────┘ └──────────┘ └────────────┘ └──────────┘
 end date    admin        temporary       new date
 reached     action       hold            range set
```

**State Transitions:**
1. Policy created → `DRAFT` or `ACTIVE` depending on start date and agent choice
2. Active policy reaches end date → system marks `EXPIRED`
3. Admin manually sets status via the UI status dropdown → `CANCELLED` or `SUSPENDED`
4. Renewal updates date range → `RENEWED`
5. Every status change is written to the **AuditLog** (append-only)

**Premium Dues:** Only policies in `ACTIVE` status appear in the current-month and advance-payment premium views.

---

## 3. LIC-Style Premium Payment Flow

```
User visits /premium
        │
        ▼
PremiumScheduleService.getCurrentMonthDues()
        │
        ├── PolicyRepository.findByPolicyStatus(ACTIVE)
        │   (filters to policies active during current month)
        │
        ├── For each policy:
        │     dueDate = policy.premiumDayOfMonth in current month
        │     premium = policy.effectiveMonthlyPremium()
        │     lateFee = calculateLateFee(dueDate, today)
        │       → today ≤ dueDate:               ₹0
        │       → today > dueDate, < dueDate+30: ₹30
        │       → today ≥ dueDate+30:            ₹100
        │     paid = any SUCCESS payment in current year-month?
        │
        ▼
PremiumDueDTO list rendered as table
        │
   ─────┴──────────────────────────────────────────
   │                                              │
   ▼ [Pay] clicked                               ▼ [Advance] clicked
POST /premium/pay                         GET /premium/advance
?policyId=X                               │
        │                                 ▼
        ▼                         isAdvancePaymentAllowed(policyId)
recordPremiumPayment(id, false)     │
Creates SUCCESS payment             ├── current month paid? → ALLOWED
amount = premium + lateFee          └── current month unpaid? → BLOCKED
Audit log entry created                   (clear error message shown)
        │
        ▼
Redirect /premium (flash success message)
```

---

## 4. Payment Processing Flow (Gateway Simulation)

```
User selects Policy + enters amount
        │
        ▼
PaymentService.makePayment()
        │
        ▼
  simulateGateway()
  ┌───────────────────────────┐
  │  Random number [0, 1)     │
  │  < 0.85  →  SUCCESS       │
  │  < 0.95  →  FAILED        │
  │  else    →  PENDING       │
  └───────────────────────────┘
        │
        ▼
Payment saved to DB (status + retryCount=0)
AuditLog entry created
Notification pushed
        │
   ─────┴──────────────────────────────────
   │                                      │
   ▼ SUCCESS                             ▼ FAILED
Payment credited                  Retry button shown
to revenue report                 at /reports/failed
                                         │
                                         ▼
                                 POST /payments/{id}/retry
                                         │
                                         ▼
                                 New Payment record:
                                 retryOfPaymentId = original ID
                                 retryCount = original + 1
                                 status = simulateGateway()
```

---

## 5. Customer Self-Service Portal Flow (Public)

```
Customer visits /portal  (no login required)
        │
        ▼
Enters Policy ID
        │
        ▼
AJAX GET /portal/api/lookup?policyId=X
        │
        ▼
PolicyRepository.findById(X)
        │
   ─────┴──────────────────────
   │                          │
   ▼ Found                   ▼ Not Found
Policy details             Error: "No policy found"
+ payment history shown
+ total paid calculated
        │
        ▼
[Download PDF] clicked
        │
        ▼
GET /portal/download-pdf?policyId=X
        │
        ▼
iText generates PDF in-memory:
 - Policy header (name, holder, dates, vehicle)
 - Payment history table
 - Footer with generation timestamp
        │
        ▼
HTTP Response:
Content-Type: application/pdf
Content-Disposition: attachment; filename="policy-X-payments.pdf"
```

---

## 6. Dashboard Analytics Flow

```
User visits /dashboard
        │
        ▼
DashboardController.dashboard()
        │
        ├── PolicyRepository.count()                → totalPolicies
        ├── PolicyRepository.countByStatus(ACTIVE)  → activeCount
        ├── PolicyRepository.countByStatus(EXPIRED) → expiredCount
        ├── PaymentRepository.sumSuccessful()        → totalRevenue
        ├── PaymentRepository.countFailed()          → failedPayments
        ├── PaymentRepository.monthlyRevenueLast12() → 12-month chart data
        └── PolicyRepository.findExpiringInDays(30)  → expiryAlerts
        │
        ▼
Model populated → dashboard.html
        │
        ▼
Thymeleaf renders KPI stat cards
Chart.js renders 12-month revenue bar chart
Bootstrap renders expiring-policies alert table
```

---

## 7. Notification Flow

```
Event occurs (policy created, payment made, status changed)
        │
        ▼
NotificationService.push(type, title, message, entityId, entityType)
        │
        ▼
Notification saved to DB
(type, title, message, read=false, createdAt=now)
        │
        ▼
Navbar bell icon shows live unread count badge
        │
        ▼
User visits /notifications
        │
        ▼
List rendered (newest first)
        │
        ▼
User clicks "Mark all read"
        │
        ▼
POST /notifications/mark-read
All notifications → read=true
Badge disappears
```

---

## 8. Report Generation Flows

### Expiry Report — `/reports/expiry`

```
GET /reports/expiry?days=30  (default: 30)
        │
        ▼
PolicyRepository.findExpiringWithin(today, today+30)
        │
        ▼
Sorted by end date ascending
Rendered as sortable table with days-remaining column
```

### Revenue Report — `/reports/revenue`

```
GET /reports/revenue
        │
        ▼
PaymentRepository.monthlyRevenueLast12()
        │
        ▼
Grouped by year-month, summed (SUCCESS payments only)
Rendered as bar chart (Chart.js) + data table
```

### Failed Payments Report — `/reports/failed`

```
GET /reports/failed
        │
        ▼
PaymentRepository.findByStatus(FAILED)
        │
        ▼
Table: Policy name, amount, date, retry count, remarks
Each row has [Retry] → POST /payments/{id}/retry
```

---

## 9. Admin User Management Flow

```
Admin visits /admin/users      (ROLE_ADMIN only — enforced by SecurityConfig)
        │
        ▼
AppUserRepository.findAll()
        │
        ▼
Table: username, full name, role, enabled status, created date
        │
        ├── [Add User]    → GET /admin/users/add
        │                    POST: BCrypt-hashes password, saves user
        │
        ├── [Edit]        → GET /admin/users/{id}/edit
        │                    POST: update role, name, email
        │
        └── [Toggle]      → POST /admin/users/{id}/toggle
                             Flips enabled flag (true ↔ false)
                             Disabled users cannot log in
```

---

## 10. Audit Trail Flow

```
Any state-changing operation
(policy create/update/delete, payment, status change, retry)
        │
        ▼
AuditService.log(actionType, entityType, entityId, description)
        │
        ▼
AuditLog record saved:
  actionType:   POLICY_CREATED | PAYMENT_MADE | STATUS_CHANGED | etc.
  entityType:   "Policy" or "Payment"
  entityId:     primary key of the affected record
  description:  human-readable summary
  performedBy:  "system" (current implementation)
  actionTime:   LocalDateTime.now()
        │
        ▼
Viewable on Policy detail page (/policies/{id})
Shows full history of all changes to that policy
```

---

## 11. Premium Calculator Flow (Public)

```
User visits /calculator  (no login required)
        │
        ▼
User selects:
  - Vehicle model year
  - Coverage type (Basic / Comprehensive / Third-Party / EV Comprehensive / Zero Dep)
  - Deductible amount
        │
        ▼
JavaScript computes premium in real time:
  base_rate × age_factor × coverage_multiplier − deductible_discount
        │
        ▼
Result displayed instantly — no server round-trip
"Estimated Annual Premium: ₹XX,XXX"
```

---

## 12. Data Seed Flow (First Startup)

```
Application starts
        │
        ▼
DataInitializer.run() [implements CommandLineRunner]
        │
        ▼
PolicyRepository.count() > 0?
   │
   ├── YES → skip seeding (data already present)
   │
   └── NO  → seed:
               4 users     (admin, manager, agent, Vicky)
               17 policies (ALL 6 statuses: ACTIVE, DRAFT, EXPIRED,
                             CANCELLED, RENEWED, SUSPENDED)
               8 policies  expiring within the next 30–220 days
               60+ payments across 2+ years of history
                           (mixed SUCCESS / FAILED / PENDING)
               3 notifications (expiry warning, payment failed, system init)
```
