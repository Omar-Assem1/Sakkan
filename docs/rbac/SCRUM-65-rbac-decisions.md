# SCRUM-65 RBAC: decisions and endpoint matrix

Branch: SCRUM-65-skn-18-rbac-method-security-and-angular-guards
Story: SKN-18 RBAC (method security and Angular guards), FR-08, NFR-03
Date: 2026-11-19
Status: decisions taken by M1 (Member 1); changes of behavior are listed so reviewers can object in the PR.

## 1. Principles

1. Default deny: every request is authenticated unless it is on an explicit public list.
2. Two layers on the backend: URL rules in SecurityConfig (answer 401/403 before body validation) and @PreAuthorize on controllers (second line of defense and documentation).
3. Ownership checks stay in the service layer (NFR-03). Controllers only check roles.
4. Errors are ProblemDetail (RFC 7807): 401 from RestAuthenticationEntryPoint, 403 from RestAccessDeniedHandler.
5. The Angular guards are UX only. The server is the authority on every request.

## 2. Decisions

| # | Topic | Decision | Behavior change |
|---|-------|----------|-----------------|
| 1 | Role rules at URL level for property create/update/publish/archive/mine | Add OWNER+AGENT URL rules and @PreAuthorize("hasAnyRole('OWNER','AGENT')") on PropertyController (create, mine, update, publish, archive). | Anonymous GET /api/properties/mine now returns 401 from the filter chain (before: 401 only because the service threw). A BUYER_TENANT sending an invalid body to POST /api/properties now gets 403 (before: 400). |
| 2 | GET /api/properties/** wildcard | Replace it with explicit public patterns: /api/properties, /api/properties/*, /api/properties/*/slots, /api/properties/*/images. The /api/properties/mine rule is placed BEFORE /api/properties/*. | None today. Any new GET under /api/properties/... is protected by default. |
| 3 | ADMIN on listing management | ADMIN stays OUT of create/update/publish/archive/image/slot management (403). No moderation role is invented in this story. | None (documents current behavior). |
| 4 | Actuator and Swagger | /actuator/health/** and /actuator/info stay public. Any other /actuator/** requires ADMIN. Swagger and api-docs stay public. No management.* config exists, so only health is exposed today. | Other actuator endpoints (if ever exposed) need an ADMIN token. |
| 5 | Disabled or deleted users | Add a per-request check: a valid JWT whose user is missing or disabled gets 401. One small query per authenticated request. | A disabled user's token stops working immediately instead of after 24 h. Tests that forge tokens must use a real user id (AuthSecurityIT S03 and S07 are adjusted in step 6). |
| 6 | Frontend roles | TokenStorageService stores ALL roles from the token (key sakan_roles) and keeps sakan_role for compatibility. hasRole(...) matches any held role. MAINTENANCE_STAFF is added to the role types. | Users with several roles pass guards for each of their roles. |
| 7 | Guards and redirects | Convert authGuard to canMatch and add roleGuard(...roles) (canMatch). No token: redirect to /auth/sign-in?returnUrl=<url>. Wrong role: redirect to /error?code=403 (existing ErrorPageComponent, no new page). Interceptor: API 401 clears the session and goes to sign-in, API 403 goes to /error?code=403. Honoring returnUrl in the sign-in component belongs to M3 and is a follow-up. | Wrong-role users see a 403 page instead of being sent to /listings. |
| 8 | Test probe controller | Keep it as a test-only AdminProbeController with @PreAuthorize("hasRole('ADMIN')"), so the real mechanism is tested. Delete it when a real ADMIN endpoint exists. | Test code only. |
| 9 | Image listing visibility (found in review) | PropertyImageService.list now takes the viewer and applies the same rule as PropertyService.getById: 404 unless the property is PUBLISHED, or the viewer is its owner or agent, or an ADMIN. | GET /api/properties/{id}/images of a DRAFT or ARCHIVED property now returns 404 for the public. Image bytes at /api/property-images/{key} stay public: keys are random UUID paths. |

## 3. Final endpoint matrix

Roles: B = BUYER_TENANT, O = OWNER, A = AGENT, M = MAINTENANCE_STAFF, ADM = ADMIN. "Owner check" means a service-layer rule: only the property owner or assigned agent.

| # | Method and path | Access | Service-layer rule |
|---|-----------------|--------|--------------------|
| 1 | POST /api/auth/register | public | none |
| 2 | POST /api/auth/login | public | none |
| 3 | POST /api/auth/admin/login | public | ADMIN role check in AuthService |
| 4 | GET /api/auth/me | any authenticated | none |
| 5 | GET /api/properties | public | PUBLISHED only |
| 6 | GET /api/properties/{id} | public | 404 unless PUBLISHED, or owner/agent/ADMIN |
| 7 | GET /api/properties/mine | O, A | none |
| 8 | POST /api/properties | O, A | none |
| 9 | PUT /api/properties/{id} | O, A | owner check |
| 10 | POST /api/properties/{id}/publish | O, A | owner check |
| 11 | POST /api/properties/{id}/archive | O, A | owner check |
| 12 | GET /api/properties/{id}/slots | public | 404 unless property visible |
| 13 | POST /api/properties/{id}/slots | O, A | owner check |
| 14 | DELETE /api/properties/{id}/slots/{slotId} | O, A | owner check |
| 15 | GET /api/properties/{id}/images | public | 404 unless property visible (decision 9) |
| 16 | POST /api/properties/{id}/images | O, A | owner check |
| 17 | PUT /api/properties/{id}/images/order | O, A | owner check |
| 18 | DELETE /api/properties/{id}/images/{imageId} | O, A | owner check |
| 19 | GET /api/property-images/{key} | public | none (random key) |
| 20 | POST /api/viewings | B, O, A | no booking on own property |
| 21 | /v3/api-docs/**, /swagger-ui/** | public | none |
| 22 | /actuator/health/**, /actuator/info | public | none |
| 23 | any other /actuator/** | ADM | none |
| 24 | any other request | authenticated (default deny) | none |
| 25 | /api/_probe/admin (test source only) | ADM | none |

A user can hold several roles: access is granted if ANY held role is allowed.

## 4. Angular routes

| Route | Guard (canMatch) |
|-------|------------------|
| '', auth/**, listings, error, ** | public |
| viewings, viewings/availability, viewings/availability/:propertyId | roleGuard(OWNER, AGENT) |
| viewings/request, viewings/request/:propertyId | roleGuard(BUYER_TENANT, OWNER, AGENT) |
| properties/new, properties/:id/edit, properties/mine | roleGuard(OWNER, AGENT) |
| admin | roleGuard(ADMIN) |

## 5. Redirect rules

| Situation | Result |
|-----------|--------|
| No token on a protected route | /auth/sign-in?returnUrl=<url> |
| Token without an allowed role | /error?code=403 |
| API answers 401 (not on /api/auth/*) | clear session, go to sign-in |
| API answers 403 | go to /error?code=403 |

## 6. Known limits (not in this story)

- No ADMIN moderation of listings (decision 3).
- Roles live in the JWT for 24 h: a role change applies after the next login. Only enabled/deleted status is checked per request (decision 5).
- The sign-in page does not yet read returnUrl (M3 follow-up).
- docs/openapi.json is edited as text only (401/403 responses and the bearer scheme where missing). It is never regenerated.
