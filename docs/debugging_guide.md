# 🐛 Debugging Guide — InsureTrack Auto Insurance Platform

A comprehensive troubleshooting reference for every common issue you might encounter.

---

## Startup Issues

### Issue: Port 8080 Already in Use

**Error:**
```
Web server failed to start. Port 8080 was already in use.
```

**Fix:**
```bash
# Linux / macOS — find and kill the process:
lsof -i :8080
kill -9 <PID>

# Windows:
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Or just change the port in application.properties:
server.port=9090
```

---

### Issue: Java Version Mismatch

**Error:**
```
UnsupportedClassVersionError: Unsupported major.minor version 61.0
```

**Fix:**
```bash
java -version
# Must show Java 17 or higher

# macOS — switch to Java 17:
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Ubuntu:
sudo update-alternatives --config java
# Select Java 17 from the list
```

---

### Issue: Maven Build Fails — Cannot Find Symbol

**Error:**
```
[ERROR] cannot find symbol
[ERROR] symbol: class SomeClass
```

**Fix:**
```bash
# Clean and rebuild
mvn clean install -DskipTests

# If still failing, clear local Maven cache for this project:
rm -rf ~/.m2/repository/com/insurance
mvn clean install -DskipTests
```

---

### Issue: H2 Console Returns 404

**Symptom:** `http://localhost:8080/h2-console` returns 404.

**Cause:** Either not running the `local` or `h2` profile, or H2 console is disabled.

**Fix:**
```bash
# Confirm you are running the local profile (default) or h2:
SPRING_PROFILES_ACTIVE=local mvn spring-boot:run

# Verify application-local.properties has:
# spring.h2.console.enabled=true
# spring.h2.console.path=/h2-console
```

The H2 console is **not available** on the `mysql` or `postgres` profiles.

---

### Issue: Data Lost After Restart

**Symptom:** All policies and payments disappear when the app restarts.

**Cause:** Running on the `h2` profile (in-memory), not the `local` profile (file-based).

**Fix:**
```bash
# Ensure you are running the default 'local' profile:
mvn spring-boot:run
# (no SPRING_PROFILES_ACTIVE set — defaults to 'local')

# Verify your data file exists:
ls ~/insurancedb.mv.db
```

If the file is missing, the database was wiped. The app will re-seed on next startup.

---

## Database Issues

### Issue: MySQL Connection Refused

**Error:**
```
Communications link failure. The last packet sent to the server was X ms ago.
```

**Fix:**
```bash
# Check MySQL is running:
sudo service mysql status        # Linux
brew services list | grep mysql  # macOS

# Start MySQL:
sudo service mysql start         # Linux
brew services start mysql        # macOS

# Verify the password in application-mysql.properties matches your MySQL root password
```

---

### Issue: PostgreSQL — Relation Does Not Exist

**Error:**
```
ERROR: relation "policies" does not exist
```

**Cause:** `ddl-auto` is set to `none`, so Hibernate has not created the tables.

**Fix:**
```properties
# In application-postgres.properties:
spring.jpa.hibernate.ddl-auto=update
# Hibernate will create tables if they don't exist and update schema safely
```

---

### Issue: DataIntegrityViolationException on Startup

**Error:**
```
DataIntegrityViolationException: could not execute statement
ConstraintViolationException: Duplicate entry 'admin' for key 'username'
```

**Cause:** `DataInitializer` attempted to re-seed an already-populated database. This normally cannot happen because the guard `if (polRepo.count() > 0) return` prevents it — but could occur if the policies table was manually cleared while the users table was not.

**Fix — Wipe the database completely and let it re-seed:**

```sql
-- MySQL:
DROP DATABASE insurance_db;
CREATE DATABASE insurance_db;

-- PostgreSQL:
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

-- File-based H2:
```
```bash
rm ~/insurancedb.mv.db ~/insurancedb.trace.db
```

Then restart the application — `DataInitializer` will re-seed from scratch.

---

## Authentication Issues

### Issue: Login Always Fails — "Bad credentials"

**Diagnosis steps:**
1. Check the username is exact — `Vicky` is case-sensitive
2. Check the password is exact — `admin123`, `manager123`, `agent123`, `Vicky@123`
3. Check the user's `enabled` flag is `true`

```sql
-- In H2 console or your DB client:
SELECT username, enabled, role FROM app_users;
```

**If the password hash is corrupted:**
```java
// Generate a fresh BCrypt hash in your IDE:
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
System.out.println(encoder.encode("admin123"));
// Update the database with the output
```

---

### Issue: 403 Forbidden on Admin Pages

**Cause:** The logged-in user's role is `ROLE_USER`, not `ROLE_ADMIN`.

**Fix:**
```sql
SELECT username, role FROM app_users WHERE username = 'yourusername';
-- Should show ROLE_ADMIN for admin access

UPDATE app_users SET role = 'ROLE_ADMIN' WHERE username = 'yourusername';
```

---

### Issue: 403 on POST After App Restart

**Cause:** CSRF token mismatch — the browser's form still holds a CSRF token from the old session, which is now invalid.

**Fix:** Log out and log back in. This clears the old session and generates a fresh CSRF token.

To prevent this in custom templates, always use `th:action` on forms — Thymeleaf automatically injects the current CSRF token:
```html
<form th:action="@{/some/path}" method="post">
    <!-- th:action injects CSRF token automatically -->
</form>
```

---

## Premium System Issues

### Issue: Policy Not Appearing in Current Month Dues

**Symptom:** An active policy does not show in `/premium`.

**Cause:** The policy's `policyStartDate` is after the current month, or `policyEndDate` is before the current month — both cases are filtered out by `PremiumScheduleService`.

**Fix:**
```sql
SELECT policy_id, policy_name, policy_start_date, policy_end_date, policy_status
FROM policies
WHERE policy_id = <your_id>;
-- Check dates encompass the current month and status = 'ACTIVE'
```

---

### Issue: Advance Payment Button Blocked Unexpectedly

**Symptom:** Advance payment is blocked but the current month should be paid.

**Cause:** `isAdvancePaymentAllowed()` checks for a `SUCCESS` payment in the **current year-month**. A `PENDING` payment does not satisfy the eligibility check.

**Fix:** Make a successful current-month payment first (either via `/premium` or `/payments/make`), then return to `/premium/advance`.

---

### Issue: Late Fee Not Appearing

**Symptom:** Policy is overdue but late fee shows ₹0.

**Cause:** The due date is today or in the future, so `calculateLateFee()` correctly returns ₹0. Check the actual `premiumDayOfMonth` value for that policy.

```sql
SELECT policy_id, policy_name, premium_day_of_month, monthly_premium
FROM policies WHERE policy_id = <id>;
```

---

## Payment Issues

### Issue: All Payments Showing FAILED

**Cause:** By design, ~10% of simulated gateway payments fail. If 100% are failing, check the simulation logic.

```java
// In PaymentService.simulateGateway():
// r < 0.85  → SUCCESS  (85%)
// r < 0.95  → FAILED   (10%)
// else      → PENDING  (5%)
```

Run 10 or more payments to observe the statistical distribution.

---

### Issue: Retry Throws "Only FAILED Payments Can Be Retried"

**Cause:** Attempting to retry a `SUCCESS` or `PENDING` payment.

**Fix:** Only `FAILED` payments show the Retry button. Navigate to `/reports/failed` and use the Retry button there — it only appears for eligible payments.

---

## Customer Portal Issues

### Issue: PDF Download Fails or Is Blank

**Possible causes:**
1. Policy has no payments — the table will be empty but the PDF should still generate
2. iText version conflict in `pom.xml`

**Fix:**
```bash
grep -A 3 "itextpdf" pom.xml
# Should show version 5.5.13.x
```

Try downloading for a policy with known payments (e.g., policy ID 1, 2, or 3 from the seeded data).

---

### Issue: Policy Lookup Returns "No Policy Found"

**Cause:** The entered policy ID does not exist, or the database was reset and re-seeded (new IDs assigned).

**Fix:**
```sql
-- Get valid policy IDs:
SELECT policy_id, policy_name FROM policies ORDER BY policy_id;
-- Use one of these IDs in the portal
```

---

## Render.com Deployment Issues

### Issue: Application Fails to Start — Out of Memory

**Error in logs:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Cause:** JVM heap exceeds Render free tier RAM limit (512 MB).

**Fix:** The `Dockerfile` already sets `-Xmx400m`. If OOM still occurs, reduce further:
```dockerfile
ENTRYPOINT ["java", "-Xms256m", "-Xmx350m", ...]
```

---

### Issue: Database Connection Timeout on Render

**Error:**
```
HikariPool-1 - Connection attempt failed:
FATAL: password authentication failed for user "..."
```

**Fix:**
1. In Render Dashboard → Web Service → Environment, verify `SPRING_PROFILES_ACTIVE=postgres`
2. Verify `JDBC_DATABASE_URL` is set and is the **Internal** database URL, not the External URL
3. Confirm `SPRING_PROFILES_ACTIVE` is not accidentally set to `local` or `h2`

---

### Issue: Application Health Shows DOWN After Deployment

**Fix:**
1. Check Render logs: **Dashboard → Web Service → Logs**
2. Look for the actual exception — common causes:
   - Database not ready when app starts → wait a minute and check again; HikariCP retries
   - Wrong `JDBC_DATABASE_URL` format → verify against Render PostgreSQL connection strings
   - `ddl-auto` issue → confirm `spring.jpa.hibernate.ddl-auto=update` in `application-postgres.properties`

---

### Issue: Cold Start — Application Takes ~3 Minutes to Respond

**This is expected behaviour** on Render's free tier, not a bug.

Render spins down inactive services after approximately 15 minutes. The first request after inactivity triggers a full container restart plus JVM initialisation, which takes approximately 3 minutes.

**Mitigation:** Use [UptimeRobot](https://uptimerobot.com) (free) to ping `/actuator/health` every 10 minutes during working hours to keep the service warm.

---

## Logging Configuration

### Enable SQL Query Logging (Temporary)

```properties
# Add to application.properties temporarily — remove before committing:
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

Logs every SQL statement and its bound parameters.

### Enable Spring Security Debug Logging

```properties
logging.level.org.springframework.security=DEBUG
```

Logs every security decision — useful for diagnosing auth and role issues.

### Enable Application-Level Debug Logging

```properties
logging.level.com.insurance=DEBUG
```

Shows debug output from all classes in the `com.insurance` package, including service method calls.

---

## Quick Reference — Common HTTP Status Codes

| Code | Meaning | Common Cause in InsureTrack |
|------|---------|---------------------------|
| 200 | OK | Successful GET request |
| 302 | Redirect | After successful POST; or redirect to `/login` for unauthenticated request |
| 400 | Bad Request | Validation error on form submission (`@Valid` failed) |
| 403 | Forbidden | Wrong role (e.g., non-admin accessing `/admin/**`); or CSRF token mismatch |
| 404 | Not Found | Wrong URL; or entity with that ID does not exist |
| 500 | Internal Server Error | Uncaught exception — check application logs for stack trace |
