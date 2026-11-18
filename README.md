# Sakan (سكن)

Real-estate platform built with an Angular 22 SPA, a Spring Boot 4 (Java 21) modular monolith API, and PostgreSQL 16 + PostGIS.

- [Run with Docker](#run-with-docker)
- [Executable JAR Build (Without Docker for API)](#executable-jar-build-without-docker-for-api)
- [Local Development (IDE / CLI)](#local-development-ide--cli)
- [Layout & Branches](#layout--branches)
- [Testing guide](#testing-guide)

---

## Run with Docker

### Prerequisites
- **Docker Desktop** (with Docker Compose v2). Nothing else (no local Java, Node.js, or PostgreSQL installation) is required to build and run the full stack.

### 1. Start the Full Stack (`db` + `api` + `web`)
`docker compose up` works out-of-the-box on a fresh clone using safe development defaults. Optionally copy `.env.example` to `.env` if you want to customize ports or credentials.

**Windows PowerShell:**
```powershell
# Optional: create a local .env file to override defaults (for example API_PORT)
Copy-Item .env.example .env

# Build images and start all services in the background
docker compose up --build -d

# Check that db, api, and web are all "Up ... (healthy)"
docker compose ps
```

**macOS / Linux (Bash / Zsh):**
```bash
# Optional: create a local .env file to override defaults
cp .env.example .env

# Build images and start all services in the background
docker compose up --build -d

# Check that db, api, and web are all "Up ... (healthy)"
docker compose ps
```

### 2. URLs and Host Ports

| Service | Default URL / Host Port | Description |
|---|---|---|
| **Web App (Angular + Nginx)** | `http://localhost:4200` (`WEB_PORT`, default `4200`) | Main SPA (`/` redirects to `/listings`, auth route at `/auth`) |
| **API via Web Proxy** | `http://localhost:4200/api/properties` | Nginx reverse-proxies `/api/*` to `http://api:8080/api/*` |
| **API Direct & Health** | `http://localhost:8080/actuator/health` (`API_PORT`, default `8080`) | Direct Spring Boot container port |
| **Swagger / OpenAPI UI** | `http://localhost:8080/swagger-ui/index.html` | Interactive API documentation |
| **PostgreSQL / PostGIS** | `localhost:5433` (`DB_PORT`, default `5433`) | Database `sakan`, user `sakan`, dev password `change_me` |

### 3. Environment Variables & Default Dev Credentials
All configurable variables are documented in [`.env.example`](.env.example). In local development (when `.env` is omitted or copied unchanged):
- **Database:** `DB_NAME=sakan`, `DB_USER=sakan`, `DB_PASSWORD=change_me` *(DEV ONLY)*
- **Admin account variables:** `ADMIN_USERNAME=admin`, `ADMIN_PASSWORD=change_me` *(DEV ONLY)*
- **JWT signing secret:** `JWT_SECRET=dev-only-jwt-secret-change-in-production-min-32-chars` *(DEV ONLY)*
- **Google OAuth2:** `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` (optional in local dev)

> **Security Note:** `.env` is git-ignored. Always replace all `[DEV ONLY]` and `[SECRET]` values with strong secrets in staging/production.

### 4. View Logs, Stop, or Reset the Database

**View live logs:**
```powershell
# All services
docker compose logs -f

# Only the API container (to inspect Flyway migrations and Spring Boot startup)
docker compose logs -f api
```

**Stop containers (keep database data and uploaded files):**
```powershell
docker compose down
```

**Full reset (stop containers AND wipe the `pgdata` and `uploads` volumes):**
```powershell
docker compose down -v
```

### 5. Troubleshooting (4 Most Common Problems)

1. **`error during connect ... open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified`**
   - **Cause:** Docker Desktop is not running.
   - **Fix:** Start **Docker Desktop**, wait until the bottom-left status bar shows **Engine running**, and verify with `docker ps`.

2. **`Ports are not available: exposing port TCP 0.0.0.0:8080 ... bind: Only one usage of each socket address is normally permitted`**
   - **Cause:** Another process on your machine (for example `MTAgentService` on Huawei laptops, or a local Tomcat/Spring Boot instance) is already listening on host port `8080` (or `5433` / `4200`).
   - **Fix:** Override the host port in `.env` (or in your shell before running Compose) without changing internal container communication:
     ```powershell
     # Windows PowerShell
     $env:API_PORT="18080"
     docker compose up --build -d
     ```
     ```bash
     # macOS / Linux
     API_PORT=18080 docker compose up --build -d
     ```
     The web app at `http://localhost:4200` and `/api/*` proxy continue working normally because Nginx talks to `http://api:8080` inside the Docker network.

3. **First `docker compose up --build` takes several minutes**
   - **Cause:** The first build downloads base images (`postgis/postgis:16-3.4`, `eclipse-temurin:21`, `node:24-alpine`, `nginx:1.27-alpine`), Maven dependencies, and npm packages.
   - **Fix:** Wait for the initial download to finish. Both `backend/Dockerfile` and `frontend/Dockerfile` cache dependency layers (`pom.xml` and `package-lock.json`), so subsequent builds take only a few seconds.

4. **`api` container reports `unhealthy` or exits on startup**
   - **Cause:** Either an old incompatible database volume exists from an earlier schema, or database credentials in `.env` do not match the password stored when the `pgdata` volume was first initialized.
   - **Fix:** Inspect the error with `docker compose logs api`. To recreate the database volume cleanly so Flyway re-applies all migrations from scratch, run:
     ```powershell
     docker compose down -v
     docker compose up --build -d
     ```

---

## Executable JAR Build (Without Docker for API)

To build and run the standalone Spring Boot executable JAR directly against the Docker database:

```powershell
# 1. Start only the PostGIS database container (exposed on localhost:5433)
docker compose up -d db

# 2. Package the runnable fat JAR (requires JDK 21)
cd backend
./mvnw -B -DskipTests package

# 3. Run the executable JAR
java -jar target/sakan-api-0.0.1-SNAPSHOT.jar
```

---

## Local Development (IDE / CLI)

```bash
cp .env.example .env
docker compose up -d db
cd backend && ./mvnw spring-boot:run
cd frontend && npm ci && npm start
```

---

## Layout & Branches

- `backend/` — Spring Boot API (feature modules under `com.sakan`)
- `frontend/` — Angular SPA
- `docs/` — Milestone documents, requirements, diagrams

**Branch flow:** `main` (release) <- `stage` (testing) <- `dev` (integration) <- `milestone_1/feat-SCRUM-<id>-<description>`
Pull requests only. Commit messages and PR titles start with the Jira key (for example `SCRUM-14`).

---

## Testing guide

### 1. Backend Integration Tests (`AbstractIntegrationTest`)
Extend `com.sakan.support.AbstractIntegrationTest` instead of repeating `@SpringBootTest` and `@Import(TestcontainersConfiguration.class)` on every integration test class. All subclasses share a single cached Spring context and a single `postgis/postgis:16-3.4` container (matching `docker-compose.yml`), so tests never touch your local PostgreSQL database.

```java
@Transactional // roll back after each test
class MyRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private MyRepository repository;

    @Test
    void savesAndFindsEntity() { ... }
}
```
> Keep the Spring context identical across subclasses: avoid `@MockitoBean`/`@MockBean`, `@TestPropertySource`, `@ActiveProfiles`, or `@DirtiesContext` on subclasses unless strictly necessary, as each variation starts a new context and container.

### 2. MockMvc 401/403 RBAC Helper (`EndpointSecurityAssertions`)
Use `com.sakan.support.security.EndpointSecurityAssertions` and the `com.sakan.support.security.Role` enum (`BUYER_TENANT`, `OWNER`, `AGENT`, `MAINTENANCE_STAFF`, `ADMIN`) to assert authentication and role-based access rules on endpoints:

```java
// Single assertions:
EndpointSecurityAssertions.assertUnauthorized(mvc, HttpMethod.POST, "/api/properties");
EndpointSecurityAssertions.assertForbiddenFor(
        mvc, HttpMethod.POST, "/api/properties", Role.BUYER_TENANT, Role.MAINTENANCE_STAFF);
EndpointSecurityAssertions.assertAllowedFor(
        mvc, HttpMethod.POST, "/api/properties", Role.OWNER, Role.AGENT);

// Or assert 401 unauthenticated + 403 for all non-allowed roles + allowed for permitted roles in one call:
EndpointSecurityAssertions.assertAccessMatrix(
        mvc, HttpMethod.POST, "/api/properties", EnumSet.of(Role.OWNER, Role.AGENT));
```

### 3. Test Tags & Maven Commands (`com.sakan.support.tags`)
Annotate test classes or methods with `@RegressionTest` (`@Tag("regression")`), `@IntegrationTest` (`@Tag("integration")`), or `@UnitTest` (`@Tag("unit")`):

```bash
# Run only the Milestone regression suite:
cd backend && ./mvnw -B test -Dgroups=regression

# Run only integration tests (requires Docker):
cd backend && ./mvnw -B test -Dgroups=integration

# Run only fast non-integration unit/slice tests (no Docker needed):
cd backend && ./mvnw -B test -DexcludedGroups=integration
```

### 4. Playwright Smoke Tests (`frontend/e2e/`)
End-to-end smoke tests live in `frontend/e2e/` and are configured via `frontend/playwright.config.ts` (separate from `npm test` and `npm run lint`). Pass credentials and optional base URL via environment variables:

```bash
cd frontend
npx playwright install chromium
BASE_URL=http://localhost:4200 E2E_USERNAME=tenant@sakan.com E2E_PASSWORD=secret npm run e2e
```
