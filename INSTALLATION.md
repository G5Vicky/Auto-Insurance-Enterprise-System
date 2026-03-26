# ⚙️ Installation Guide — InsureTrack v2.1

Complete setup instructions for every supported environment.

---

## Prerequisites

| Tool | Version | Verify |
|------|---------|--------|
| Java | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| Git | any | `git --version` |
| MySQL *(optional)* | 8.0+ | Only needed for `mysql` profile |

---

## Option A — Quickest Start (File-based H2, zero setup)

No database installation required. Data persists across restarts.

```bash
# 1. Clone
git clone <your-repo-url>
cd Auto-Insurance-Enterprise-System

# 2. Build and run
mvn clean install
mvn spring-boot:run

# 3. Open
# http://localhost:8080
# Login: admin / admin123
```

Data is stored at `~/insurancedb.mv.db`. Restarting the app will not lose any data.

---

## Option B — MySQL (local production-like)

```bash
# 1. Open src/main/resources/application-mysql.properties
#    Change: spring.datasource.password=YOUR_MYSQL_PASSWORD_HERE
#    to your actual MySQL root password.

# 2. Run with mysql profile
SPRING_PROFILES_ACTIVE=mysql mvn spring-boot:run
```

The `insurance_db` database is created automatically if it does not exist (`createDatabaseIfNotExist=true`). Schema is maintained with `ddl-auto=update` — existing data is never dropped.

---

## Option C — In-Memory H2 (testing only)

```bash
SPRING_PROFILES_ACTIVE=h2 mvn spring-boot:run
```

> ⚠️ **Do NOT use for development.** All data is wiped on every restart. This profile is intended for CI pipelines and automated testing only.

---

## Render.com Deployment

### Step 1: Push to GitHub

```bash
git init
git add .
git commit -m "feat: initial InsureTrack deployment"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/auto-insurance-enterprise-system.git
git push -u origin main
```

### Step 2: Create PostgreSQL Database on Render

1. Log in to [Render.com](https://render.com)
2. Click **New +** → **PostgreSQL**
3. Name: `insuretrack-db` | Plan: Free
4. Click **Create Database**
5. Copy the **Internal Database URL** — you will need it in Step 4

### Step 3: Create Web Service

1. Click **New +** → **Web Service**
2. Connect your GitHub repository
3. Set Runtime: **Docker** | Plan: Free

### Step 4: Set Environment Variables

| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `postgres` |
| `JDBC_DATABASE_URL` | *(Render Internal Database URL)* |

> Use the **Internal** database URL — it is faster and does not count against bandwidth limits.

### Step 5: Deploy

Click **Create Web Service**. Render pulls the code, builds the Docker image, and starts the container. Monitor logs for: `Started AutoInsuranceApplication`.

### ⚠️ Cold Start Warning

Render free tier spins down inactive services after approximately 15 minutes. On first access after inactivity, expect **approximately 3 minutes** for the application to fully start up.

**Mitigation:** Configure [UptimeRobot](https://uptimerobot.com) (free plan) to ping your health endpoint every 10 minutes:

```
https://<your-render-url>/actuator/health
```

This keeps the service warm during active working hours but cannot prevent cold starts entirely on the free tier.

---

## H2 Web Console

Available on `local` and `h2` profiles only:

| Field | Value |
|-------|-------|
| URL | `http://localhost:8080/h2-console` |
| JDBC URL (local) | `jdbc:h2:file:~/insurancedb` |
| JDBC URL (h2) | `jdbc:h2:mem:insurancedb` |
| Username | `sa` |
| Password | *(leave blank)* |

---

## Resetting Demo Data

### File-based H2 (local profile)

```bash
rm ~/insurancedb.mv.db ~/insurancedb.trace.db
mvn spring-boot:run
# DataInitializer re-seeds on next startup
```

### MySQL

```bash
mysql -u root -p -e "DROP DATABASE insurance_db;"
SPRING_PROFILES_ACTIVE=mysql mvn spring-boot:run
```

### PostgreSQL (Render)

```sql
-- Run in your Render PostgreSQL console:
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
-- Restart the Render web service — DataInitializer re-seeds automatically
```

---

## Post-Installation Verification

```bash
# Health check — should return {"status":"UP"}
curl http://localhost:8080/actuator/health

# Login page — should return 200
curl -I http://localhost:8080/login

# Public portal — should return 200 (no auth required)
curl -I http://localhost:8080/portal

# Protected route — should return 302 redirect to login
curl -I http://localhost:8080/dashboard
```
