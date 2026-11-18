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
> **Proposal:** Add a separate CI job or workflow step that:
> 1. Starts the Docker stack (`docker compose up --build -d`)
> 2. Waits for healthy containers
> 3. Runs `npx playwright test`
> 4. Tears down with `docker compose down -v`
>
> This was **not** added in this PR to avoid scope creep; it should be a separate story.

---

## SCRUM-120: Executable Build & Docker Rehearsal

### Executable JAR Build

| Metric | Value |
|--------|-------|
| Command | `./mvnw -B -DskipTests package` |
| Artifact | `backend/target/sakan-api-0.0.1-SNAPSHOT.jar` |
| Size | 67.29 MB |
| Build result | ✅ **SUCCESS** (9 s) |

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

| # | Finding | Severity |
|---|---------|----------|
| 1 | **No admin account seeder.** README mentions `ADMIN_USERNAME` and `ADMIN_PASSWORD` env vars and `.env.example` has them, but there is no `CommandLineRunner`/`ApplicationRunner` that seeds the admin user on startup. The `is_seeded_admin` column exists in `V10__auth_users_roles_extensions.sql` and `User.java`, but the actual seeding code is missing. A new teammate following the README would expect an admin account to exist after `docker compose up`. | Medium |
| 2 | **Port 8080 conflict on Windows.** On some Windows machines (e.g., Huawei laptops with `MTAgentService`), port 8080 is occupied. The README troubleshooting section correctly documents this with the `API_PORT` override workaround. | Info (documented) |
| 3 | **`docker compose up` works without `.env`.** The README correctly states this ("works out-of-the-box on a fresh clone using safe development defaults"). Confirmed in this rehearsal. | Info (positive) |
| 4 | **First build speed.** Docker layer caching makes subsequent builds fast (< 10 s). First build requires downloading base images and dependencies. README documents this in troubleshooting. | Info (documented) |

---

## SCRUM-121: Release Flow (PREPARED — NOT EXECUTED)

### Branch Protection Assumptions (from README)

> **Branch flow:** `main` (release) ← `stage` (testing) ← `dev` (integration) ← feature branches
> Pull requests only. Commit messages and PR titles start with the Jira key.

Expected protection rules (to be verified on GitHub):
- `dev`: 1 approval required, CI must pass
- `stage`: CI must pass
- `main`: 2 approvals required, CI must pass

### Proposed Release Flow

```
dev  ──PR──>  stage  ──PR──>  main  ──tag──>  m1-rc1
```

**Suggested tag name:** `m1-rc1`

### Draft Commands (awaiting confirmation)

**Step 1: PR from `dev` → `stage`**
```bash
# Create PR via GitHub CLI or web UI
# Title: "SCRUM-121 M1 release candidate: dev → stage"
# Body: "Promotes all M1 features from dev to stage for final testing."
```

**Step 2: PR from `stage` → `main`**
```bash
# Title: "SCRUM-121 M1 release candidate: stage → main"
# Body: "Promotes M1 release candidate from stage to main. All CI checks passed. Regression suite: 242 backend tests, 110 frontend tests, 1 Playwright e2e — all green."
```

**Step 3: Tag (after main merge)**
```bash
git fetch origin main
git tag m1-rc1 origin/main
git push origin m1-rc1
```

> ⛔ **AWAITING USER CONFIRMATION.** These commands will NOT be executed until Member 5 replies "approved" in chat.

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
