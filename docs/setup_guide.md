# 🔧 Setup Guide — InsureTrack Auto Insurance Platform

Detailed environment setup for developers new to this project.

---

## 1. System Requirements

### Minimum
- CPU: 2 cores
- RAM: 4 GB (8 GB recommended if running IntelliJ IDEA)
- Disk: 2 GB free
- OS: Windows 10+, macOS 12+, Ubuntu 20.04+

### Required Software

#### Java 17

**macOS (Homebrew):**
```bash
brew install openjdk@17
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@17' >> ~/.zshrc
source ~/.zshrc
java -version  # Should show: openjdk 17.x.x
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
java -version
```

**Windows:**
Download from: https://adoptium.net/temurin/releases/?version=17
Set the `JAVA_HOME` system environment variable to the installation path.

#### Maven (if not using the bundled `mvnw` wrapper)

```bash
# macOS
brew install maven

# Ubuntu
sudo apt install maven

# Verify
mvn -version  # Should show: Apache Maven 3.x.x
```

---

## 2. Cloning and Project Setup

```bash
git clone https://github.com/YOUR_USERNAME/auto-insurance-enterprise-system.git
cd auto-insurance-enterprise-system

# Verify project structure
ls -la
# You should see: src/, pom.xml, Dockerfile, README.md, docs/
```

### IDE Setup — IntelliJ IDEA (Recommended)

1. Open IntelliJ IDEA
2. **File → Open** → select the project folder
3. IntelliJ detects it as a Maven project and imports dependencies automatically
4. Wait for Maven sync to complete (first time: ~2–3 minutes)
5. Run `AutoInsuranceApplication.java` (right-click → Run)

### IDE Setup — VS Code

1. Install extensions: `Extension Pack for Java`, `Spring Boot Extension Pack`
2. Open the project folder
3. `Ctrl+Shift+P` → "Java: Import Maven Projects"
4. Use the Spring Boot Dashboard to run the application

---

## 3. Spring Profiles Explained

The application uses Spring profiles to switch between databases without code changes.

| Profile | Database | Default? | Use Case |
|---------|----------|----------|----------|
| `local` | File-based H2 | ✅ Yes | Local development — data persists |
| `h2` | In-memory H2 | No | CI / automated testing only |
| `mysql` | MySQL 8.0 | No | Local production-like environment |
| `postgres` | PostgreSQL | No | Render.com production deployment |

The active profile is selected in `application.properties`:
```properties
spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}
```

This reads the `SPRING_PROFILES_ACTIVE` environment variable, falling back to `local` if unset.

---

## 4. Local Profile (Default) — File-Based H2

### What Happens on Startup

1. Spring Boot detects the `local` profile
2. H2 opens a file-based database at `~/insurancedb.mv.db`
3. Hibernate runs schema migration (`ddl-auto=update` — adds new columns/tables, never drops)
4. `DataInitializer.run()` checks if policies exist — seeds demo data only on the first run

### H2 Web Console

```
URL:      http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:~/insurancedb
Username: sa
Password: (leave blank)
```

Use this to run SQL queries and inspect database state during development.

---

## 5. MySQL Profile

### Configuration — `application-mysql.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/insurance_db?createDatabaseIfNotExist=true
    &useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD_HERE
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
```

### Running

```bash
SPRING_PROFILES_ACTIVE=mysql mvn spring-boot:run
```

`createDatabaseIfNotExist=true` ensures the `insurance_db` database is created automatically. `ddl-auto=update` adds new columns or tables but never drops existing data.

---

## 6. PostgreSQL Profile

### Configuration — `application-postgres.properties`

```properties
spring.datasource.url=${JDBC_DATABASE_URL}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update

# HikariCP — tuned for Render free tier
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### Running Locally Against a PostgreSQL Instance

```bash
export JDBC_DATABASE_URL=jdbc:postgresql://localhost:5432/insurancedb?user=insureuser&password=yourpassword
export SPRING_PROFILES_ACTIVE=postgres
mvn spring-boot:run
```

---

## 7. Key Configuration Files

### `application.properties` (Master)

```properties
spring.application.name=autoinsurance-platform
spring.profiles.active=${SPRING_PROFILES_ACTIVE:local}

server.port=${PORT:8080}        # Render injects $PORT; locally defaults to 8080
server.error.whitelabel.enabled=false

spring.thymeleaf.cache=false    # Enable in production for performance
spring.thymeleaf.encoding=UTF-8

management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always

logging.level.com.insurance=INFO
logging.level.org.hibernate.SQL=WARN
logging.pattern.console=%d{HH:mm:ss} %-5level %logger{30} - %msg%n
```

### Security Configuration (`SecurityConfig.java`)

Public routes (no login required):
```
/login, /portal/**, /calculator, /css/**, /js/**,
/actuator/health
```

Admin-only routes:
```
/admin/**
```

CSRF-exempt (AJAX endpoints):
```
/payments/api/**, /portal/api/**, /premium/api/**
```

All other routes require authentication.

---

## 8. Thymeleaf Templates Structure

```
templates/
├── auth/login.html                 Login form
├── dashboard.html                  KPI cards, revenue chart, expiry alerts
├── fragments/
│   ├── navbar.html                 Navigation bar — included in all pages
│   └── footer.html                 Footer — included in all pages
├── policies/
│   ├── list.html                   Policy listing with status filter
│   ├── form.html                   Add / Edit policy form
│   └── detail.html                 Policy detail + audit log
├── payments/
│   ├── list.html                   All payments
│   ├── make.html                   Make payment form
│   ├── detail.html                 Single payment detail
│   └── byPolicy.html               Payments for a specific policy
├── premium/
│   ├── current.html                Current-month dues dashboard
│   ├── advance.html                Next-month advance payment view
│   └── pay.html                    Premium pay form (current or advance)
├── reports/
│   ├── expiry.html                 Expiry report (next 30 days)
│   ├── revenue.html                Monthly revenue + Chart.js bar chart
│   └── failed.html                 Failed payments with retry buttons
├── admin/
│   ├── users.html                  User management list
│   └── userform.html               Add / edit user form
├── notifications/list.html         Notification centre
├── portal/index.html               Public customer self-service portal
└── tools/calculator.html           Public premium estimator
```

### Spring Security Integration in Templates

```html
<!-- Visible only to admins -->
<div sec:authorize="hasAuthority('ROLE_ADMIN')">
    <a href="/admin/users">User Management</a>
</div>

<!-- Visible to any authenticated user -->
<div sec:authorize="isAuthenticated()">
    Welcome, <span sec:authentication="name"></span>
</div>
```

---

## 9. Static Assets

| File | Approximate Size | Purpose |
|------|-----------------|---------|
| `static/css/app.css` | ~52 KB | Custom Bootstrap overrides and component styles |
| `static/js/app.js` | ~7 KB | Premium calculator logic, customer portal AJAX, notification badge |

---

## 10. Verifying the Setup

After startup, confirm the following:

```bash
# Health check — should return {"status":"UP"}
curl http://localhost:8080/actuator/health

# Login page — should return 200
curl -I http://localhost:8080/login

# Public portal — should return 200 (no authentication required)
curl -I http://localhost:8080/portal

# Public calculator — should return 200
curl -I http://localhost:8080/calculator

# Protected route — should return 302 redirect to /login
curl -I http://localhost:8080/dashboard
```

If the health check returns `{"status":"DOWN"}`, check the database connection configuration and ensure the correct profile is active.
