# Sakan (سكن)

Real-estate platform. Angular + Spring Boot modular monolith + PostgreSQL.

## Run

    cp .env.example .env
    docker compose up -d db
    cd backend && ./mvnw spring-boot:run
    cd frontend && npm start

## Layout

- backend/  Spring Boot API (feature modules under com.sakan)
- frontend/ Angular SPA
- docs/     Milestone documents, requirements, diagrams

## Branches
`main` (release) <- `stage` (testing) <- `dev` (integration) <- `milestone_1/feat-SCRUM-<id>-<description>`
Pull requests only. Commit messages and PR titles start with the Jira key (for example `SCRUM-51`).

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
