# 🚀 Deployment Guide — InsureTrack Auto Insurance Platform

Complete deployment instructions for Render.com (current production), VPS/bare metal, and CI/CD pipeline setup.

---

## Part 1: Render.com Deployment (Current Production)

Render.com is a modern PaaS that supports Docker deployments with managed PostgreSQL hosting.

### Prerequisites
- GitHub account with the project pushed
- Render.com account (free tier is sufficient)

---

### Step 1: Push Code to GitHub

```bash
git init
git add .
git commit -m "feat: initial InsureTrack production build"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/auto-insurance-enterprise-system.git
git push -u origin main
```

---

### Step 2: Create PostgreSQL Database on Render

1. Log in to [Render.com](https://render.com)
2. Click **New +** → **PostgreSQL**
3. Configure:
   - **Name:** `insuretrack-db`
   - **Database:** `insurancedb`
   - **User:** `insureuser`
   - **Region:** Choose the region closest to your users
   - **Plan:** Free
4. Click **Create Database**
5. From the database detail page, copy the **Internal Database URL** — you will need it in Step 4

---

### Step 3: Create Web Service on Render

1. Click **New +** → **Web Service**
2. Connect your GitHub repository
3. Configure:
   - **Name:** `insuretrack`
   - **Region:** Same region as the database
   - **Branch:** `main`
   - **Runtime:** Docker
   - **Plan:** Free

---

### Step 4: Set Environment Variables

In your Render web service settings, add the following environment variables:

| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `postgres` |
| `JDBC_DATABASE_URL` | *(Internal Database URL from Step 2)* |

> **Important:** Use the **Internal** database URL (starts with `dpg-...`), not the External URL. Internal connections are faster and do not count against bandwidth limits.

The application reads these in `application-postgres.properties`:
```properties
spring.datasource.url=${JDBC_DATABASE_URL}
```

---

### Step 5: Deploy

Click **Create Web Service**. Render will:
1. Pull your code from GitHub
2. Build the Docker image using the project `Dockerfile`
3. Start the container

Monitor build logs and look for: `Started AutoInsuranceApplication`

Your application will be available at: `https://insuretrack.onrender.com`

---

### Step 6: Verify Deployment

```bash
# Health check — should return {"status":"UP"}
curl https://insuretrack.onrender.com/actuator/health

# Full health response:
# {"status":"UP","components":{"db":{"status":"UP"},"diskSpace":{"status":"UP"}}}
```

---

### Dockerfile Explained

```dockerfile
# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q    # Cache dependencies as a separate layer
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre-jammy   # JRE only — ~200MB vs ~600MB JDK
WORKDIR /app

# Security: run as non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser
COPY --from=build /app/target/*.jar app.jar
RUN chown appuser:appuser app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java",
  "-Xms256m", "-Xmx400m",
  "-Djava.security.egd=file:/dev/./urandom",
  "-jar", "app.jar"]
```

**Why multi-stage?** The build stage uses a full Maven + JDK image (~600MB). The final image only needs the JRE (~200MB). This reduces the deployed image size by 3x and improves startup time.

---

### ⚠️ Cold Start Behaviour

Render free tier **spins down inactive services after approximately 15 minutes** of inactivity.

- First request after inactivity: **approximately 3 minutes** for the service to restart and the JVM to fully initialise
- Subsequent requests: normal response speed
- This is expected platform behaviour — not a bug

**Mitigation:** Configure [UptimeRobot](https://uptimerobot.com) (free plan) to ping the health endpoint every 10 minutes:

```
https://<your-render-url>/actuator/health
```

This keeps the service warm during active working hours. Note: Render free tier cannot be kept permanently alive — this reduces, not eliminates, cold starts.

---

## Part 2: Manual Server / VPS Deployment

For deployment to your own server (AWS EC2, DigitalOcean, etc.):

### Step 1: Build the JAR

```bash
mvn clean package -DskipTests
# Output: target/autoinsurance-1.0.0.jar
```

### Step 2: Transfer to Server

```bash
scp target/autoinsurance-1.0.0.jar user@your-server:/opt/insuretrack/
```

### Step 3: Create Systemd Service

```ini
# /etc/systemd/system/insuretrack.service
[Unit]
Description=InsureTrack Auto Insurance Platform
After=network.target postgresql.service

[Service]
Type=simple
User=insuretrack
WorkingDirectory=/opt/insuretrack
Environment="SPRING_PROFILES_ACTIVE=postgres"
Environment="JDBC_DATABASE_URL=jdbc:postgresql://localhost:5432/insurancedb?user=insureuser&password=yourpassword"
ExecStart=/usr/bin/java -Xms256m -Xmx400m -jar /opt/insuretrack/autoinsurance-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable insuretrack
sudo systemctl start insuretrack
sudo systemctl status insuretrack
```

### Step 4: Configure Nginx Reverse Proxy

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## Part 3: GitHub Actions CI/CD (Optional)

Automate deployments to Render on every push to `main`.

Create `.github/workflows/deploy.yml`:

```yaml
name: Deploy to Render

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Set up Java 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Trigger Render Deploy Hook
        run: curl -X POST ${{ secrets.RENDER_DEPLOY_HOOK_URL }}
```

Find the Deploy Hook URL in Render: **Web Service → Settings → Deploy Hook**. Save it as a GitHub repository secret named `RENDER_DEPLOY_HOOK_URL`.

---

## Environment Variables Reference

| Variable | Required | Profile | Description |
|----------|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | Yes (prod) | all | Set to `postgres` for production |
| `JDBC_DATABASE_URL` | Yes | postgres | Full JDBC connection string |
| `PORT` | Auto | all | Injected by Render — do not set manually |

The application reads `server.port=${PORT:8080}` — Render injects `$PORT`, locally it defaults to `8080`.

---

## Post-Deployment Checklist

- [ ] `/actuator/health` returns `{"status":"UP"}`
- [ ] `/login` page loads correctly
- [ ] Login with `admin / admin123` succeeds
- [ ] Dashboard renders with seeded data
- [ ] `/portal` loads without login
- [ ] PDF download works from portal
- [ ] Premium dues page shows active policies
- [ ] Payment can be made and appears in payment list
- [ ] Failed payment appears in `/reports/failed` with Retry button
- [ ] `/admin/users` accessible to admin, returns 403 for other roles
