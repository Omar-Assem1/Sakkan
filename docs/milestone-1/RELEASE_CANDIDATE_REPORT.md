# Milestone 1 — Release Candidate Report

| Field           | Value |
|-----------------|-------|
| **Date**        | 2026-11-18 |
| **Commit SHA**  | `5b75810` (origin/dev at time of branch) + `8d6149a` (@RegressionTest annotations) |
| **Branch**      | `milestone_1/feat-SCRUM-118-release-candidate-packaging` |
| **Author**      | Member 5 (Nour) |

---

## Environment

| Component      | Version |
|----------------|---------|
| OS             | Windows NT 10.0.26300.0 |
| Java (JDK)     | OpenJDK 21.0.13 (Eclipse Temurin-21.0.13+9) |
| Node.js        | v26.12.1 |
| Docker Desktop | 29.9.3 (build 5457199) |
| Spring Boot    | 4.1.2 |
| Angular        | 22 |
| PostgreSQL     | 16 + PostGIS 3.4 (via Docker / Testcontainers) |

---

## Summary Results Table

| Check | Command | Result | Status |
|-------|---------|--------|--------|
| Backend tests & coverage gate | `cd backend && ./mvnw -B verify` | 242 run, 0 failures, 0 errors, 0 skipped | ✅ PASS |
| JaCoCo line coverage | `target/site/jacoco/jacoco.csv` (`index.html`) | **97.16%** (615 / 633 lines; gate ≥ 60%) | ✅ PASS |
| Regression tests (before `@RegressionTest` expansion) | `./mvnw -B test -Dgroups=regression` | 51 tests (9 property test classes), 0 failures | ✅ PASS |
| Regression tests (after `@RegressionTest` expansion) | `./mvnw -B test -Dgroups=regression` | **232 tests** (22 test classes), 0 failures | ✅ PASS |
| Frontend install & lint | `cd frontend && npm ci && npm run lint` | 460 packages; `All files pass linting.` | ✅ PASS |
| Frontend unit tests | `npm test -- --watch=false` | **22 test files, 110 tests passed**, 0 failures | ✅ PASS |
| Frontend production build | `npm run build -- --configuration production` | 376.27 kB initial bundle (`dist/frontend/browser`) | ✅ PASS |
| Playwright E2E smoke test | `npx playwright test` (`login-then-browse.spec.ts`) | **1 passed** (898 ms) against Docker stack | ✅ PASS |
| Executable JAR build & run | `./mvnw -B -DskipTests package` + `java -jar target/sakan-api-0.0.1-SNAPSHOT.jar` | 67.29 MB fat JAR; connects to PostGIS, `/actuator/health` UP, `/v3/api-docs` HTTP 200 | ✅ PASS |
| Clean-machine Docker rehearsal | Fresh `git clone --branch dev` (no `.env`) + `docker compose up --build -d` | `db`, `api`, `web` all `healthy`; web HTTP 200, API health UP, 4 Flyway migrations applied (v30) | ✅ PASS |

---

## Test / Build Failures & Root Causes

No test or build failures occurred in the backend test suite (242/242 passed), frontend lint/unit tests/production build (110/110 passed), or Playwright E2E smoke test (1/1 passed).

During the **SCRUM-120 README walkthrough**, two documentation omissions were discovered and fixed in `README.md`:
1. **Standalone `java -jar` without `JWT_SECRET` set fails on startup** with `PlaceholderResolutionException: Could not resolve placeholder 'JWT_SECRET'` because `application.yml` deliberately sets `app.jwt.secret: ${JWT_SECRET}` without a default (`# no default on purpose: the app must fail fast without it`). Fixed in `README.md` by documenting `$env:JWT_SECRET` / `JWT_SECRET` in the Executable JAR and Local Development sections.
2. **Playwright README example credentials (`E2E_USERNAME=tenant@sakan.com E2E_PASSWORD=secret`) fail registration validation** because `login-then-browse.spec.ts` constructs `email: ${username}@sakan.test` and `/api/auth/register` requires a password of at least 8 characters with uppercase, lowercase, and digit. Fixed in `README.md` to use `E2E_USERNAME=e2e_tenant` and `E2E_PASSWORD=Password123` (with both PowerShell and Bash examples).

---

## SCRUM-119: Regression Suite Results

### Backend — Full Verify (`./mvnw -B verify`)

| Metric       | Value |
|--------------|-------|
| Tests run    | **242** |
| Failures     | **0** |
| Errors       | **0** |
| Skipped      | **0** |
| Build result | **SUCCESS** |
| Duration     | 2 min 20 s |

```
[INFO] Tests run: 242, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### JaCoCo Coverage

| Metric        | Value |
|---------------|-------|
| Lines covered | 615 |
| Lines missed  | 18 |
| Total lines   | 633 |
| **Line coverage** | **97.16%** |
| Gate threshold | 60% (0.60) |
| Gate result    | ✅ **PASSED** |

```
[INFO] Analyzed bundle 'sakan-api' with 49 classes
[INFO] All coverage checks have been met.
```

**JaCoCo HTML report location:** `backend/target/site/jacoco/index.html`
CI uploads it as artifact named `jacoco-report`.

### JaCoCo Gate Configuration (verified in `pom.xml`)

- Plugin: `org.jacoco:jacoco-maven-plugin:0.8.14`
- `check` goal bound to `verify` phase
- Counter: `LINE`, Value: `COVEREDRATIO`, Minimum: `${jacoco.minimum.coverage}` = `0.60`
- `haltOnFailure: true`
- Excludes: `com/sakan/SakanApplication.class`, `**/*MapperImpl.class`

### Regression-Tagged Tests (`./mvnw -B test -Dgroups=regression`)

| Run | Tests | Failures | Notes |
|-----|-------|----------|-------|
| Before annotations | 51 | 0 | Property module only (9 existing classes) |
| **After annotations** | **232** | **0** | +13 classes annotated |

**Classes annotated with `@RegressionTest` in this PR (pure annotation-only, no logic changes):**

| Area | Test Class |
|------|-----------|
| Auth/RBAC | `AuthServiceTest` |
| Auth/RBAC | `JwtServiceTest` |
| Auth/RBAC | `AuthControllerIT` |
| Auth/RBAC | `AuthSecurityIT` |
| Auth/RBAC | `LoginRateLimitIT` |
| Common | `SecurityContextCurrentUserProviderTest` |
| Common | `GlobalExceptionHandlerTest` |
| Property | `PropertyControllerTest` |
| Property | `PropertyServiceTest` |
| Property | `PropertyStatusTest` |
| Viewings | `ViewingSlotControllerIntegrationTest` |
| Viewings | `ViewingSlotControllerTest` |
| Viewings | `ViewingSlotServiceTest` |

**Remaining M1 feature areas with NO `@RegressionTest` coverage:**

| Area | Status | Reason |
|------|--------|--------|
| Admin | ❌ No coverage | No admin-specific test classes exist yet |
| Locations | ❌ No coverage | No location-specific test classes exist yet |

### Frontend

| Step | Result |
|------|--------|
| `npm ci` | ✅ 460 packages installed |
| `npm run lint` | ✅ All files pass linting |
| `npm test -- --watch=false` | ✅ **22 test files, 110 tests passed** |
| `npm run build -- --configuration production` | ✅ 376.27 kB initial bundle, 6.3 s |

```
 Test Files  22 passed (22)
      Tests  110 passed (110)
```

```
Initial chunk files | Names    |  Raw size | Estimated transfer size
main-COKHRQRF.js    | main     | 173.55 kB |                44.84 kB
chunk-BJEq97vZ.js   | -        | 171.93 kB |                50.65 kB
styles-MH4652EN.css | styles   |  30.79 kB |                 5.50 kB
                    | Total    | 376.27 kB |               100.99 kB
```

### Playwright E2E Smoke Test

| Metric | Value |
|--------|-------|
| Spec file | `frontend/e2e/login-then-browse.spec.ts` |
| Tests run | **1** |
| Passed | **1** |
| Duration | 2.1 s (test: 898 ms) |
| Target | `http://localhost:4200` (Docker stack) |

```
  ok 1 [chromium] › e2e\login-then-browse.spec.ts:14:3 › signs in and displays the Browse Properties listing page (898ms)
  1 passed (2.1s)
```

> **Note:** The current CI workflow (`.github/workflows/ci.yml`) does **NOT** run Playwright tests.
> **Proposed CI Job (not added in this PR — recommend tracking as an M2 story):**
> ```yaml
>   e2e-playwright:
>     name: Playwright E2E Smoke (Docker stack)
>     runs-on: ubuntu-latest
>     needs: [backend, frontend]
>     steps:
>       - name: Checkout code
>         uses: actions/checkout@v4
>       - name: Start Docker Compose stack
>         run: docker compose up --build -d --wait
>       - name: Set up Node.js 24
>         uses: actions/setup-node@v4
>         with:
>           node-version: 24
>           cache: npm
>           cache-dependency-path: frontend/package-lock.json
>       - name: Install frontend dependencies & Playwright Chromium
>         working-directory: frontend
>         run: |
>           npm ci
>           npx playwright install --with-deps chromium
>       - name: Run Playwright smoke tests
>         working-directory: frontend
>         env:
>           BASE_URL: http://localhost:4200
>           E2E_USERNAME: e2e_tenant
>           E2E_PASSWORD: ${{ secrets.E2E_TEST_PASSWORD }}
>         run: npm run e2e
>       - name: Tear down Docker Compose stack
>         if: always()
>         run: docker compose down -v
> ```

---

## SCRUM-120: Executable Build & Docker Rehearsal

### Executable JAR Build & Run Against PostGIS

| Metric | Value |
|--------|-------|
| Build command | `cd backend && ./mvnw -B -DskipTests package` |
| Artifact | `backend/target/sakan-api-0.0.1-SNAPSHOT.jar` (repackaged fat JAR) |
| Size | 67.29 MB |
| Build result | ✅ **SUCCESS** (9.032 s) |
| Database container | `docker compose up -d db` (`postgis/postgis:16-3.4` on `localhost:5433`) |
| Run command | `$env:JWT_SECRET="dev-only-jwt-secret-change-in-production-min-32-chars"; java -jar target/sakan-api-0.0.1-SNAPSHOT.jar` |
| Health check | `/actuator/health` → `{"groups":["liveness","readiness"],"status":"UP"}` |
| OpenAPI spec | `/v3/api-docs` → `HTTP 200 (13434 bytes)` |

```
2026-11-18T21:57:39.388+02:00  INFO 33092 --- [sakan] [main] com.sakan.SakanApplication       : Starting SakanApplication v0.0.1-SNAPSHOT using Java 21.0.13
2026-11-18T21:57:42.734+02:00  INFO 33092 --- [sakan] [main] org.flywaydb.core.FlywayExecutor : Database: jdbc:postgresql://localhost:5433/sakan (PostgreSQL 16.4)
2026-11-18T21:57:42.828+02:00  INFO 33092 --- [sakan] [main] o.f.core.internal.command.DbMigrate : Current version of schema "public": 30
2026-11-18T21:57:46.170+02:00  INFO 33092 --- [sakan] [main] com.sakan.SakanApplication       : Started SakanApplication in 7.235 seconds (process running for 7.806)
```

### Clean-Machine Docker Rehearsal

| Step | Result |
|------|--------|
| Fresh `git clone --branch dev` (no `.env`) | ✅ Cloned to temp dir |
| `docker compose up --build -d` | ✅ All 3 images built |
| `docker compose ps` — db healthy | ✅ |
| `docker compose ps` — api healthy | ✅ |
| `docker compose ps` — web healthy | ✅ |
| Web login page `http://localhost:4200/auth/sign-in` → HTTP 200 | ✅ |
| API health `http://localhost:18080/actuator/health` → HTTP 200, status UP | ✅ |
| Flyway migrations applied (4 migrations, now at v30) | ✅ |
| Admin account seeded | ⚠️ **No admin seeder found** (see findings) |
| `docker compose down -v` cleanup | ✅ |

```
docker compose ps:
NAME                       STATUS                    PORTS
sakan-rc-rehearsal-db-1    Up 46 seconds (healthy)   0.0.0.0:5433->5432/tcp
sakan-rc-rehearsal-api-1   Up 40 seconds (healthy)   0.0.0.0:18080->8080/tcp
sakan-rc-rehearsal-web-1   Up 24 seconds (healthy)   0.0.0.0:4200->80/tcp
```

```
Flyway log:
Successfully validated 4 migrations
Successfully applied 4 migrations to schema "public", now at version v30
```

### README Walkthrough Findings

| # | Finding | Severity | Action Taken |
|---|---------|----------|--------------|
| 1 | **No admin account seeder.** README mentions `ADMIN_USERNAME` and `ADMIN_PASSWORD` env vars and `.env.example` has them, but there is no `CommandLineRunner`/`ApplicationRunner` that seeds the admin user on startup. The `is_seeded_admin` column exists in `V10__auth_users_roles_extensions.sql` and `User.java`, but the actual seeding code is missing. A new teammate following the README would expect an admin account to exist after `docker compose up`. | Medium | Documented in report (application code not modified per ground rules). |
| 2 | **Missing `JWT_SECRET` in README "Executable JAR Build" and "Local Development" sections.** `application.yml` requires `JWT_SECRET` with no default (`app.jwt.secret: ${JWT_SECRET}`), while `docker-compose.yml` provides a default for containerized runs. Running `java -jar target/sakan-api-0.0.1-SNAPSHOT.jar` directly from the README failed until `JWT_SECRET` was set in the shell. | Medium | Fixed in `README.md` (`SCRUM-120`) for both Windows PowerShell and macOS/Linux. |
| 3 | **Playwright README example credentials (`E2E_USERNAME=tenant@sakan.com E2E_PASSWORD=secret`) fail registration validation.** `login-then-browse.spec.ts` posts `${username}@sakan.test` as the email and requires a password matching `RegisterRequest` policy (≥ 8 chars, uppercase, lowercase, digit). Also lacked a Windows PowerShell snippet. | Low | Fixed in `README.md` (`SCRUM-120`) using `E2E_USERNAME=e2e_tenant` and `E2E_PASSWORD=Password123` with PowerShell & Bash examples. |
| 4 | **Port 8080 conflict on Windows.** On some Windows machines (e.g., Huawei laptops with `MTAgentService`), port 8080 is occupied. The README troubleshooting section documents the `$env:API_PORT="18080"` override workaround, which worked seamlessly in the rehearsal. | Info (documented) | Verified working. |
| 5 | **`docker compose up` works without `.env`.** The README states that `docker compose up` works out-of-the-box on a fresh clone using safe development defaults. Confirmed in the clean-machine rehearsal (`Test-Path .env` was `False`). | Info (positive) | Verified working. |

---

## SCRUM-121: Release Flow (PREPARED — NOT EXECUTED)

### Branch Protection Assumptions (from README & Project Guidelines)

> **Branch flow:** `main` (release) ← `stage` (testing) ← `dev` (integration) ← `milestone_1/feat-SCRUM-<id>-<description>`
> Pull requests only. Commit messages and PR titles start with the Jira key.

Expected branch protection rules on GitHub (to be verified manually in Repository Settings → Branches):
- `dev`: Pull request required, **1 approval** required, CI checks (`Backend (build, test, coverage)`, `Frontend (lint, test, build)`, `PR title has Jira key`) must pass.
- `stage`: Pull request required, CI checks must pass.
- `main`: Pull request required, **2 approvals** required, CI checks must pass, no force-pushes.

### Proposed Release Flow & Tag

```
dev  ──PR #1──>  stage  ──PR #2──>  main  ──annotated tag──>  m1-rc1 (or v1.0.0-m1)
```

- **Suggested tag name:** `m1-rc1` (release candidate tag) or `v1.0.0-m1` (final Milestone 1 release tag).

### Draft Commands & PR Descriptions (Awaiting Confirmation)

#### Step 1: Merge `SCRUM-118` PR into `dev`
Wait for CI to pass and Member 6 review approval on the `SCRUM-118` PR into `dev`, then merge via GitHub UI.

#### Step 2: Open PR from `dev` → `stage`
- **PR Title:** `SCRUM-121 promote dev to stage for Milestone 1 release candidate`
- **Base:** `stage` ← **Compare:** `dev`
- **PR Description:**
  ```markdown
  ## Jira
  SCRUM-121 https://saakan.atlassian.net/browse/SCRUM-121

  ## What changed
  - Promotes integrated Milestone 1 (`dev`) to `stage` for release candidate verification.
  - Includes all M1 stories, Docker Compose packaging (SCRUM-14), Playwright smoke test (SCRUM-21), expanded `@RegressionTest` suite (SCRUM-119), and M1 Release Candidate Report (`docs/milestone-1/RELEASE_CANDIDATE_REPORT.md`).

  ## How it was tested
  - [x] Tests written first and passing locally (242 backend tests, 97.16% JaCoCo line coverage; 110 frontend unit tests; 1 Playwright E2E test)
  - [x] CI is green (backend build + tests, JaCoCo >= 60%, frontend lint + test + build)
  - [ ] Flyway migration / Swagger updated (if API or DB changed) — N/A
  - [ ] Jira time logged on the sub-task
  ```

#### Step 3: Open PR from `stage` → `main`
- **PR Title:** `SCRUM-121 Milestone 1 release: merge stage into main`
- **Base:** `main` ← **Compare:** `stage`
- **Reviewers:** Request at least 2 approvals per `main` branch protection rules.
- **PR Description:**
  ```markdown
  ## Jira
  SCRUM-121 https://saakan.atlassian.net/browse/SCRUM-121

  ## What changed
  - Promotes verified Milestone 1 release candidate from `stage` into `main`.
  - Full regression and packaging evidence recorded in `docs/milestone-1/RELEASE_CANDIDATE_REPORT.md`.

  ## How it was tested
  - [x] Tests written first and passing locally (242 backend tests, 232 `@RegressionTest`, 97.16% line coverage; 110 frontend tests; 1 Playwright E2E smoke test; clean-machine Docker rehearsal)
  - [x] CI is green on `stage` (backend build + tests, JaCoCo >= 60%, frontend lint + test + build)
  - [x] Flyway migrations V10, V20, V21, V30 verified on clean PostGIS container
  - [ ] Jira time logged on the sub-task
  ```

#### Step 4: Create and Push Annotated Tag on `main` (after `stage → main` PR merges)
```bash
git fetch origin main
git checkout main
git pull --ff-only origin main
git tag -a m1-rc1 -m "SCRUM-121 Milestone 1 release candidate 1"
git push origin m1-rc1
```

> ⛔ **AWAITING USER CONFIRMATION.** None of the SCRUM-121 promotion merges or tag commands have been executed. Reply **"approved"** (and specify your preferred tag name: `m1-rc1` or `v1.0.0-m1`) when you want me to proceed step by step.

---

## Verdict

### ✅ READY for Release

| Criterion | Status |
|-----------|--------|
| Backend tests (242) | ✅ All pass |
| JaCoCo coverage (97.16% ≥ 60%) | ✅ Gate passed |
| Regression suite (232 tagged tests) | ✅ All pass |
| Frontend lint | ✅ Clean |
| Frontend unit tests (110) | ✅ All pass |
| Frontend production build | ✅ Success |
| Playwright e2e (1 smoke test) | ✅ Passes |
| Executable JAR build | ✅ 67.29 MB |
| Docker clean-machine rehearsal | ✅ All services healthy |
| README accuracy | ✅ Mostly accurate |

**Known gaps (non-blocking for M1):**
1. No admin account seeder (schema supports it, code not yet implemented)
2. No `@RegressionTest` coverage for admin and locations (no test classes exist)
3. CI does not run Playwright (propose separate story)

**Recommendation:** Proceed with release. The admin seeder and Playwright CI can be addressed in M2.
