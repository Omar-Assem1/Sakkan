# Sakan: Milestone 1 Jira Breakdown (1 to 14 November 2026)

**Scope:** Auth (basic, Google, seeded admin), RBAC, user management, listings with search and images, viewings with conflict detection, plus the project foundation (CI, Docker, tests, Angular shell).
**Capacity assumption:** about 10 h/week per member, so roughly 50 to 60 h each. Adjust to your real availability.
**IDs are illustrative.** Jira will assign the real numbers. Use the real key in branch names.

## 1. Fair split: why members 4 to 6 carry foundation work now

Members 4, 5, and 6 own modules that land in Milestone 2 (contracts/payments, maintenance, offers). In M1 they carry the shared foundation and design spikes, and in M2 the load reverses. State this in your Jira walkthrough so the TAs see the reasoning.

| Member | Slice | Stories | Est. hours |
|---|---|---|---|
| 1 | Auth, RBAC, Admin | SKN-15, 16, 17, 18, 19 | 60 |
| 2 | Listings, Search | SKN-20, 21, 22, 23 | 56 |
| 3 | Viewings + Angular shell | SKN-12, 25, 26, 27 | 60 |
| 4 | DB, seed data, shared framework | SKN-8, 9, 10, 11, 29 | 50 |
| 5 | CI, Docker, tests | SKN-5, 6, 7, 28, 31 | 52 |
| 6 | Images, errors/API docs, events, process | SKN-13, 14, 24, 30, 32 | 48 |

## 2. Epics

| Key | Epic |
|---|---|
| SKN-1 | Platform Foundation |
| SKN-2 | Auth and User Management |
| SKN-3 | Property Listings and Search |
| SKN-4 | Viewing Appointments |

## 3. Stories

Branch pattern: `milestone_1/feat-SKN-<id>-<3-5 word description>`. Feature branches merge into `milestone_1`, and `milestone_1` merges into `main` before the discussion.

| ID | Story | Epic | Owner | Est. (h) | Branch |
|---|---|---|---|---|---|
| SKN-5 | CI pipeline | 1 | M5 | 10 | `milestone_1/feat-SKN-5-ci-pipeline-jacoco-gate` |
| SKN-6 | Docker Compose and executable build | 1 | M5 | 10 | `milestone_1/feat-SKN-6-docker-compose-executable-build` |
| SKN-7 | Test infrastructure | 1 | M5 | 18 | `milestone_1/feat-SKN-7-test-infrastructure-setup` |
| SKN-8 | Flyway baseline schema and seed data | 1 | M4 | 14 | `milestone_1/feat-SKN-8-flyway-baseline-seed-data` |
| SKN-9 | Location API (cascading data) | 1 | M4 | 8 | `milestone_1/feat-SKN-9-location-hierarchy-api` |
| SKN-10 | Shared status-transition framework | 1 | M4 | 10 | `milestone_1/feat-SKN-10-status-transition-framework` |
| SKN-11 | Demo dataset generator | 1 | M4 | 8 | `milestone_1/feat-SKN-11-demo-dataset-generator` |
| SKN-12 | Angular shell, routing, AR/EN + RTL | 1 | M3 | 16 | `milestone_1/feat-SKN-12-angular-shell-i18n-rtl` |
| SKN-13 | Global error handling, OpenAPI, generated client | 1 | M6 | 8 | `milestone_1/feat-SKN-13-error-handling-openapi-client` |
| SKN-14 | Process setup: Jira/Confluence, sprint report | 1 | M6 | 8 | _docs only, no branch_ |
| SKN-15 | Register and login with JWT (form 1) | 2 | M1 | 16 | `milestone_1/feat-SKN-15-basic-auth-jwt-login` |
| SKN-16 | Google OAuth sign up/in | 2 | M1 | 14 | `milestone_1/feat-SKN-16-google-oauth-login` |
| SKN-17 | Seeded admin and admin sign-in (form 2) | 2 | M1 | 8 | `milestone_1/feat-SKN-17-seeded-admin-signin` |
| SKN-18 | RBAC (method security and Angular guards) | 2 | M1 | 10 | `milestone_1/feat-SKN-18-rbac-security-guards` |
| SKN-19 | Admin: manage users and promote (listing 22) | 2 | M1 | 12 | `milestone_1/feat-SKN-19-admin-manage-users` |
| SKN-20 | Property CRUD API and lifecycle | 3 | M2 | 14 | `milestone_1/feat-SKN-20-property-crud-api` |
| SKN-21 | Add/Edit Property form and My Properties (forms 4, 5) | 3 | M2 | 16 | `milestone_1/feat-SKN-21-property-form-my-listings` |
| SKN-22 | Browse Properties with filters and pagination (listing 3) | 3 | M2 | 16 | `milestone_1/feat-SKN-22-browse-filters-pagination` |
| SKN-23 | Full-text search | 3 | M2 | 10 | `milestone_1/feat-SKN-23-fulltext-search` |
| SKN-24 | Image upload, validation, compression | 3 | M6 | 14 | `milestone_1/feat-SKN-24-property-image-upload` |
| SKN-25 | Set viewing availability (form 7) | 4 | M3 | 14 | `milestone_1/feat-SKN-25-viewing-availability-slots` |
| SKN-26 | Request viewing with conflict detection (form 6) | 4 | M3 | 16 | `milestone_1/feat-SKN-26-request-viewing-conflicts` |
| SKN-27 | Viewing status transitions and My Viewings (listing 8) | 4 | M3 | 14 | `milestone_1/feat-SKN-27-viewing-status-my-viewings` |
| SKN-28 | Maintenance module design spike | 1 | M5 | 8 | _docs only_ |
| SKN-29 | Contracts and payments design spike | 1 | M4 | 10 | _docs only_ |
| SKN-30 | Offers module design spike | 1 | M6 | 10 | _docs only_ |
| SKN-31 | M1 release candidate: regression run and packaging | 1 | M5 | 6 | `milestone_1/feat-SKN-31-release-candidate-packaging` |
| SKN-32 | Domain event publisher for status changes | 1 | M6 | 8 | `milestone_1/feat-SKN-32-status-change-events` |

## 4. Sub-tasks and tests-first lists

For each story, create the sub-tasks as Jira Tasks with the hours shown. **Write the test list in the story's Confluence page before coding**, as the spec requires.

**SKN-5 CI pipeline (M5)**
- GitHub Actions backend job: build and test (3 h)
- JaCoCo report with 60% gate on the whole backend (2 h)
- Angular job: lint, unit test, production build (3 h)
- PR template, branch protection, Jira link rule (2 h)
- Tests first: a deliberately failing test blocks the PR; coverage drop below 60% fails the build.

**SKN-6 Docker and executable build (M5)**
- Dockerfiles for API and web (3 h)
- Compose with PostgreSQL (PostGIS image), API, web, and a volume for uploads (4 h)
- Run guide in the README, and a clean-machine rehearsal (3 h)
- Tests first: `docker compose up` on a fresh clone serves the login page.

**SKN-7 Test infrastructure (M5)**
- Testcontainers base class for integration tests (4 h)
- MockMvc helper that asserts 401/403 per role (4 h)
- Playwright smoke test: login then browse (6 h)
- Tag setup for the regression suite (4 h)

**SKN-8 Flyway baseline (M4)**
- ERD in Confluence (3 h)
- V1 schema: users, roles, location, amenity, property, property_image, viewing_slot, viewing (5 h)
- Seed: locations (EN/AR), amenities, property types (4 h)
- Constraints and indexes: unique `(property_id, start_time)` on slots, B-tree on `(location_id, type, price)` (2 h)
- Tests first: migration runs on an empty database; seed counts are correct.

**SKN-9 Location API (M4)**
- Endpoints for governorates, cities, districts, compounds by parent, with `Accept-Language` (5 h)
- Unit and integration tests (3 h)

**SKN-10 Status-transition framework (M4)**
- Generic `StatusTransitions` helper with allowed-transition map (4 h)
- Apply to ViewingStatus and PropStatus (3 h)
- Exhaustive tests: every allowed and every illegal transition (3 h)

**SKN-11 Demo dataset (M4)**
- Generator producing 5,000 listings across locations and types (6 h)
- Dev-profile switch so it does not run in tests (2 h)

**SKN-12 Angular shell (M3)**
- Layout, navbar, routing with lazy features (5 h)
- Auth interceptor and token storage service (3 h)
- Transloco setup, language toggle, `dir` and `lang` on `<html>` (5 h)
- Tailwind logical-property convention and shared UI kit (3 h)

**SKN-13 Errors and OpenAPI (M6)**
- `@RestControllerAdvice` returning ProblemDetail (3 h)
- springdoc-openapi and published spec (2 h)
- openapi-generator step for the Angular client (3 h)

**SKN-14 Process setup (M6)**
- Jira board, issue types, GitHub link, workflow columns (3 h)
- Confluence spaces and templates (3 h)
- Sprint report skeleton (2 h)

**SKN-15 Register and login (M1)**
- User, Role entities and repository (3 h)
- Register and login endpoints, BCrypt, JWT filter (6 h)
- Angular sign up/in form (form 1) (4 h)
- Tests first: duplicate username rejected; wrong password returns 401; token expiry; role stored correctly (3 h)

**SKN-16 Google OAuth (M1)**
- Google Cloud project and OAuth client setup (2 h)
- Spring OAuth2 login with success handler that issues JWT (6 h)
- Angular "Continue with Google" flow (3 h)
- Tests: first login creates user; repeat login reuses account (3 h)

**SKN-17 Seeded admin (M1)**
- Startup runner that creates the admin from environment variables, idempotently (3 h)
- Admin sign-in form (form 2) (3 h)
- Tests: runs once; no admin created if one exists; credentials never logged (2 h)

**SKN-18 RBAC (M1)**
- Security config and `@PreAuthorize` on services (4 h)
- Angular `canMatch` guards per role (3 h)
- 401/403 test per role per endpoint group (3 h)

**SKN-19 Admin manage users (M1)**
- Users listing endpoint with filter and pagination (4 h)
- Promote and demote endpoints (never the hardcoded admin) (4 h)
- Angular listing 22 (4 h)

**SKN-20 Property CRUD API (M2)**
- Property entity, DTOs, MapStruct mapper (4 h)
- CRUD, publish, archive with ownership check (6 h)
- Tests: non-owner edit rejected; illegal status transition rejected (4 h)

**SKN-21 Property form and My Properties (M2)**
- Add/Edit form with cascading location dropdowns (8 h)
- My Properties listing with status and actions (5 h)
- Form validation messages in EN and AR (3 h)

**SKN-22 Browse and filters (M2)**
- Specification-based filter query: location, price range, type, purpose, bedrooms, amenities (6 h)
- Pagination and sort (3 h)
- Angular listing 3 with filter panel and empty state (7 h)

**SKN-23 Full-text search (M2)**
- `pg_trgm` and `tsvector` indexes via Flyway (3 h)
- Search parameter in the listing query (4 h)
- Tests with Arabic and English titles (3 h)

**SKN-24 Image upload (M6)**
- Storage interface with local-volume implementation (4 h)
- Upload endpoint: type, size, and count validation (4 h)
- Client-side compression in Angular before upload (3 h)
- Thumbnail generation (3 h)

**SKN-25 Availability (M3)**
- ViewingSlot entity and endpoints (6 h)
- Angular form 7 (5 h)
- Tests: overlapping slots rejected (3 h)

**SKN-26 Request viewing (M3)**
- `ViewingConflictChecker` service (6 h)
- Request endpoint and Angular form 6 (6 h)
- Tests: double booking rejected; concurrent requests for the same slot (4 h)

**SKN-27 Viewing transitions (M3)**
- Confirm, cancel, complete endpoints using the SKN-10 framework (6 h)
- My Viewings listing, filtered by role (5 h)
- Tests: every transition, both parties' views (3 h)

**SKN-28 / 29 / 30 Design spikes (M5 / M4 / M6)**
- Entity list, CRC cards, and a draft sequence diagram for the module. These feed the Design Phase document and keep M2 on schedule.

**SKN-31 Release candidate (M5)**
- Run the full regression suite on `milestone_1` (2 h)
- Build the executable and test on a clean machine (3 h)
- Tag and merge to `main` (1 h)

**SKN-32 Domain events (M6)**
- `StatusChangedEvent` published by the transition framework through Spring application events (5 h)
- A test listener proving it fires on viewing transitions (3 h)

## 5. Timeline

| Dates | Focus |
|---|---|
| 1 to 3 Nov | SKN-8 migrations merged first (everything depends on it). SKN-5, 6 (CI and Docker) up. SKN-12 shell started. |
| 4 to 7 Nov | SKN-15 login endpoint available to the frontend. SKN-20, 25 backends. SKN-10, 13 merged. Mid-sprint check against the Jira board. |
| 8 to 11 Nov | Remaining forms and listings. SKN-16, 18, 24 integrated. Feature freeze on 11 Nov. |
| 12 to 13 Nov | SKN-31: regression run, executable rehearsal, merge to `main`, assemble the milestone document. |
| 14 Nov | Discussion. |

## 6. PR and review rotation

Every member opens at least one PR and reviews at least one. Use this rotation so nobody is missed.

| Author | Primary reviewer | Second reviewer (for auth, money, or state-machine code) |
|---|---|---|
| M1 | M2 | M5 |
| M2 | M3 | M1 |
| M3 | M4 | M2 |
| M4 | M5 | M3 |
| M5 | M6 | M4 |
| M6 | M1 | M6's choice |

Link each PR to its Jira key (the branch name does this automatically), and log time on each task.

## 7. Milestone 1 document checklist (spec §3.3)

1. Requirements elicitation for FR-01 to FR-24 and FR-48 (use the requirements file).
2. Use case diagrams for: Sign Up/In, Google Sign-In, Admin Sign-In, Manage Users, Manage Listings, Browse Properties, Set Availability, Book Viewing.
3. Flow of events (sequence or activity diagram) for each use case.
4. Changes and refactoring with reasons.
5. Jira walkthrough: sprint backlog, assignments, estimates, time logged.
6. PR list linked to stories (each member 1 or more opened, 1 or more reviewed).
7. JaCoCo report showing 60% or more backend coverage.
8. Executable build and a clean-machine run.
9. Test cases written before implementation, plus the regression and integration suites.

## 8. Definition of Done (per story)

Acceptance criteria met, tests written first and passing, PR reviewed by a non-author, CI green, Swagger and Flyway updated, Jira time logged, Confluence page updated.
