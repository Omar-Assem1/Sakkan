# Milestone 1: Identity Use Cases and Stories (Member 1)

Epic: Auth and User Management. Diagram: `docs/diagrams/m1-identity-use-cases.png`.

| Story | Use case | FRs | Forms |
|---|---|---|---|
| SCRUM-51 | UC-01 Register, UC-02 Login | FR-01, FR-02 | Form 1 |
| SCRUM-56 | UC-03 Google Sign-In | FR-03 | Form 1 (Google button) |
| SCRUM-61 | UC-04 Admin Sign-In, UC-05 Seed Admin | FR-04, FR-05 | Form 2 |
| SCRUM-65 | UC-06 Enforce RBAC | FR-08 | none |
| SCRUM-69 | UC-07 Manage Users | FR-06, FR-48 | Listing 22 |

---

## UC-01 Register (SCRUM-51)
- **Actor:** Visitor. **Precondition:** not logged in; roles BUYER_TENANT, OWNER, AGENT exist.
- **Main flow:**
    1. The visitor enters username, email, password, phone (E.164) and a role.
    2. The system validates the DTO.
    3. The system checks that the username and email are unique.
    4. The system hashes the password (BCrypt) and saves the user with auth_provider = LOCAL.
    5. The system returns 201 with the user summary (never the hash).
- **Alternate flows:**
    - Invalid field: 400 ProblemDetail.
    - Role ADMIN or MAINTENANCE_STAFF: rejected.
    - Duplicate username or email: 409.
- **Postcondition:** one new user with exactly one role, and no token issued.

## UC-02 Login (SCRUM-51)
- **Actor:** Registered user. **Precondition:** LOCAL account, enabled.
- **Main flow:**
    1. The user submits username and password.
    2. The system verifies the BCrypt hash.
    3. The system signs a JWT (HS256, 24 h, secret from environment) with user id, username and roles.
    4. The system returns 200 with the token.
    5. The client sends it as a Bearer header on later requests.
- **Alternate flows:**
    - Unknown user or wrong password: 401 with the same generic message.
    - Disabled account or Google-only account: 401.
- **Postcondition:** the client holds a valid JWT and the server keeps no session.

## UC-03 Google Sign-In (SCRUM-56)
- **Actors:** Visitor, Google (external). **Precondition:** Google OAuth client configured.
- **Main flow:**
    1. The visitor clicks "Continue with Google".
    2. Spring Security redirects to Google and the visitor consents.
    3. Google redirects back and Spring exchanges the code for the profile (email, sub).
    4. The success handler finds the user by email.
    5. If none exists, it creates one (GOOGLE, google_sub, BUYER_TENANT, phone empty).
    6. The system issues Sakan's own JWT and redirects the SPA to /auth/callback.
    7. The SPA stores the token.
- **Alternate flows:**
    - Returning user: reuse the account, no new row.
    - Email already used by a LOCAL account: see Decision D1.
    - Google error or consent denied: redirect to sign-in with an error.
- **Postcondition:** the user holds a Sakan JWT and is handled like a local user.

## UC-04 Admin Sign-In (SCRUM-61)
- **Actor:** Admin. **Precondition:** the seeded or a promoted admin exists.
- **Main flow:**
    1. The admin submits username and password on the dedicated admin form.
    2. The system verifies the credentials and that the user has the ADMIN role.
    3. The system returns a JWT, and the SPA opens the admin area.
- **Alternate flows:**
    - Valid credentials but no ADMIN role: 401 with the same generic message as a wrong password.
    - Wrong credentials: 401.

## UC-05 Seed Hardcoded Admin (SCRUM-61)
- **Actor:** Startup runner (system). **Precondition:** ADMIN_USERNAME and ADMIN_PASSWORD are set.
- **Main flow:**
    1. At startup the runner checks whether an is_seeded_admin user exists.
    2. If none exists, it creates one from the environment (BCrypt, ADMIN role, is_seeded_admin = true).
- **Alternate flows:**
    - A seeded admin already exists: do nothing (idempotent).
    - Variables missing: fail startup with a clear message, never logging the values.
- **Postcondition:** exactly one seeded admin exists, and credentials are never logged.

## UC-06 Enforce RBAC (SCRUM-65)
- **Actor:** any caller. **Precondition:** none.
- **Main flow:**
    1. A request arrives at a protected endpoint or Angular route.
    2. The JWT filter validates the token.
    3. @PreAuthorize (or the canMatch guard) checks the role.
    4. The request proceeds, and the service layer checks ownership where relevant.
- **Alternate flows:**
    - Missing, expired or tampered token: 401.
    - Wrong role: 403.
    - Wrong owner: 403 (from the service).
- **Postcondition:** no protected action is executed without a valid token and the right role.

## UC-07 Manage Users (SCRUM-69)
- **Actor:** Admin. **Precondition:** signed in as admin (UC-04).
- **Main flow:**
    1. The admin opens listing 22.
    2. The system returns users with filter and pagination.
    3. The admin changes a role or promotes a user to admin.
    4. The system updates user_roles (granted_by, granted_at).
    5. For a promoted admin, the admin can later demote them.
- **Alternate flows:**
    - Target is the seeded admin (demote or delete): 403 "protected account".
    - Caller is not an admin: 403.
    - User not found: 404.
- **Postcondition:** the role change is stored with who granted it.

---

## Decisions to confirm with the team
- **D1 (Google email collision):** if the Google email matches a LOCAL account and Google reports email_verified = true, link the account (store google_sub, keep the password). Otherwise reject with 409. This is an assumption.
- **D2:** a new Google user has an empty phone and the default role BUYER_TENANT. The SPA asks for phone and role afterwards (per the Design doc, 8.1).
- **D3:** registration returns 201 without a token; the user then logs in.
- **D4:** attempts to demote or delete the seeded admin return 403.