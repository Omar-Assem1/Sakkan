# Sakan (سكن): Software Requirements (Functional and Non-Functional)

Version 2. Supersedes the HomeHub design document's requirement lists. Paste into the Confluence "Requirements" space.

**Legend, column "vs. design doc":** **Kept** (unchanged), **Changed** (reworded or moved), **New** (added for the course spec or the Egyptian market).
**Milestone:** M1 = 14 Nov, M2 = 28 Nov, M3 = 12 Dec.

## A. Functional Requirements

### A1. Authentication and User Management

| ID | Requirement | M | vs. design doc |
|---|---|---|---|
| FR-01 | A visitor can register with username, email, password, and phone number (E.164 format) and choose a role: Buyer/Tenant, Owner, or Agent. | M1 | Changed (was FR-01) |
| FR-02 | A registered user can log in with username and password and receives a signed JWT (expiry 24 h). | M1 | Kept (FR-02) |
| FR-03 | A visitor can sign up or sign in with a Google account (OAuth2). A first-time Google login creates an account, later logins match by email, and the system issues its own JWT. | M1 | **New** (course spec §2.2.1a) |
| FR-04 | An admin signs in through a dedicated admin form using username and password. | M1 | **New** (course spec §2.2.1c) |
| FR-05 | At first startup the system seeds exactly one hardcoded admin account whose credentials come from environment configuration. This account cannot be deleted or demoted. | M1 | Changed (was "first registered user is admin") |
| FR-06 | An admin can promote any user to admin and demote promoted admins (never the hardcoded one). | M1 | Kept (FR-04) |
| FR-07 | An admin can create Maintenance Staff accounts. | M2 | **New** (the design doc never said how staff accounts are created) |
| FR-08 | Role-based access control is enforced on every API endpoint and Angular route. Unauthenticated requests return 401 and unauthorised requests return 403. | M1 | Kept (FR-05) |

### A2. Location, Currency, and Language

| ID | Requirement | M | vs. design doc |
|---|---|---|---|
| FR-09 | The system provides a seeded location hierarchy (Governorate → City → District → Compound) with English and Arabic names. | M1 | **New** |
| FR-10 | Listing forms and filters use cascading location dropdowns. | M1 | Changed (was city/neighbourhood text) |
| FR-11 | Prices are stored with a currency (EGP or USD) and always displayed with it. | M1 | **New** |
| FR-12 | The user can switch the interface between English (LTR) and Arabic (RTL). | M2 | **New** |

### A3. Property Listings

| ID | Requirement | M | vs. design doc |
|---|---|---|---|
| FR-13 | Owners and agents can create, edit, publish, and archive property listings. | M1 | Kept (FR-06) |
| FR-14 | A listing has title, description, purpose (sale or rent), property type, location, price and currency, bedrooms, bathrooms, area (m²), amenities, and images. | M1 | Changed (FR-07) |
| FR-15 | Only the listing's owner or assigned agent can edit or archive it. | M1 | Changed (from CRC card) |
| FR-16 | Buyers/tenants can browse listings with filters: location, price range, property type, purpose, bedrooms, amenities. Results are paginated. | M1 | Kept (FR-08) |
| FR-17 | A search bar performs full-text search across listing titles and descriptions. | M1 | Kept (FR-09) |
| FR-18 | A listing shows its status: Draft, Published, Rented, Sold, or Archived. | M1 | Kept (FR-10) |
| FR-19 | An owner or agent can upload up to 10 images per listing. File type and size are validated and images are compressed. | M1 | Changed (FR-07) |

### A4. Viewing Appointments

| ID | Requirement | M | vs. design doc |
|---|---|---|---|
| FR-20 | Owners and agents define available time slots for a property. | M1 | Kept (FR-11) |
| FR-21 | Buyers/tenants request a viewing from the available slots. | M1 | Kept (FR-12) |
| FR-22 | The system detects and prevents double-booking. | M1 | Kept (FR-13) |
| FR-23 | Viewing status transitions: Requested → Confirmed → Cancelled / Completed. | M1 | Kept (FR-14) |
| FR-24 | Both parties can view their upcoming and past viewings. | M1 | Kept (FR-15) |

### A5. Offers and Negotiation

| ID | Requirement | M | vs. design doc |
|---|---|---|---|
| FR-25 | Buyers/tenants can submit an offer (amount, currency, message) on a published listing. | M2 | Kept (FR-16) |
| FR-26 | Owners/agents can accept, reject, or counter with a new amount. The buyer can accept or reject a counter. | M2 | Kept (FR-17) |
| FR-27 | Offers have an expiry time. Expired offers change status automatically. | M2 | Kept (FR-18) |
| FR-28 | Offer status: PENDING → COUNTERED → ACCEPTED / REJECTED / EXPIRED. | M2 | Kept (FR-19) |
| FR-29 | A buyer can have only one active offer per listing at a time. | M2 | Kept (FR-20) |

### A6. Rental Contracts and Payments

| ID | Requirement | M | vs. design doc |
|---|---|---|---|
| FR-30 | An accepted offer can be converted into a rental contract. | M2 | Kept (FR-21) |
| FR-31 | A contract records start date, end date, monthly rent, currency, and both parties. | M2 | Kept (FR-22) |
| FR-32 | On activation the system generates a payment schedule (one record per month). | M2 | Kept (FR-23) |
| FR-33 | Payments have status PENDING, PAID, or OVERDUE. Overdue is set automatically after the due date. | M2 | Kept (FR-24) |
| FR-34 | Tenants and owners can view payment history with filters. | M2 | Kept (FR-25) |

### A7. Maintenance Tickets

| ID | Requirement | M | vs. design doc |
|---|---|---|---|
| FR-35 | A tenant with an active contract can submit a ticket with title, description, category, and priority (Low/Medium/High/Urgent). | M2 | Kept (FR-26) |
| FR-36 | An owner or admin assigns a ticket to a maintenance staff member. | M2 | Kept (FR-27) |
| FR-37 | Ticket lifecycle: OPEN → ASSIGNED → IN_PROGRESS → RESOLVED. | M2 | Kept (FR-28) |
| FR-38 | All involved parties can view ticket history and current status. | M2 | Kept (FR-29) |

### A8. Leads and Contact

| ID | Requirement | M | vs. design doc |
|---|---|---|---|
| FR-39 | A listing detail page has a "Contact via WhatsApp" button that opens a WhatsApp chat with the listing's contact number, prefilled with the listing title, and records a lead. | M2 | **New** |
| FR-40 | Owners and agents can view a leads inbox (listing, user, time, status NEW/CONTACTED/CLOSED) and update lead status. | M2 | **New** |

### A9. Roommate Matching

| ID | Requirement | M | vs. design doc |
|---|---|---|---|
| FR-41 | A tenant can create a roommate profile: budget, city, lifestyle tags, move-in date. | M3 | Kept (FR-30) |
| FR-42 | The system computes a compatibility score (0 to 100) between profiles. | M3 | Kept (FR-31) |
| FR-43 | A tenant sees a ranked list of suggested matches. | M3 | Kept (FR-32) |
| FR-44 | Users can contact matched roommates through the platform. | M3 | Kept (FR-33) |

### A10. Reviews and Agent Directory

| ID | Requirement | M | vs. design doc |
|---|---|---|---|
| FR-45 | After a completed viewing or contract, a tenant can review the agent (1 to 5 stars and a comment). | M3 | Kept (FR-34) |
| FR-46 | Agents have a public profile with average rating and review history. | M3 | Kept (FR-35) |
| FR-47 | Buyers/tenants can browse the agents directory sorted by rating. | M3 | Kept (FR-36) |

### A11. Admin and Moderation

| ID | Requirement | M | vs. design doc |
|---|---|---|---|
| FR-48 | An admin can view all users and change their roles. | M1 | Kept (FR-37), moved earlier |
| FR-49 | Any user can report a listing with a reason. | M3 | Kept (FR-38) |
| FR-50 | An admin sees a moderation queue of reported listings and can remove the listing or dismiss the report. | M3 | Kept (FR-39) |

### A12. Reserved

| ID | Requirement | M |
|---|---|---|
| FR-51 | **Surprise functional requirement:** to be defined when announced, before Milestone 3. | M3 |

### Not committed (stretch, only if a sprint has slack)
EGP/USD conversion display from a stored rate table, agency accounts that group brokers, favourites, saved searches.

## B. Non-Functional Requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Performance | API responses under 500 ms for 95% of requests under normal load. Search endpoint p95 under 300 ms at 50 concurrent users on a seeded dataset of 5,000 to 10,000 listings. |
| NFR-02 | Security: authentication | Passwords hashed with BCrypt. JWTs signed with HS256, 24 h expiry, secret loaded from environment. |
| NFR-03 | Security: access control | RBAC on all endpoints. Ownership checks in the service layer (no editing another user's listing, offer, or ticket). |
| NFR-04 | Security: web | Input validation on all DTOs, CORS allow-list, upload type and size checks, rate limiting on login, parameterised queries only. |
| NFR-05 | Scalability | Stateless REST backend so it can run behind a load balancer. |
| NFR-06 | Usability | Responsive Angular SPA for desktop and tablet, usable on mobile widths. |
| NFR-07 | Localisation | Arabic and English with RTL support. Location and amenity names stored in both languages. |
| NFR-08 | Reliability | Records are archived rather than deleted. Payment is simulated. Scheduled jobs (offer expiry, overdue payments) are idempotent. |
| NFR-09 | Testability | At least 60% unit-test coverage on the **whole backend** (JaCoCo gate in CI), targeting 70% on services. Test cases are written before implementation. A regression suite re-runs earlier milestones' features. |
| NFR-10 | Maintainability | Modular monolith organised by feature. Layers: Controller → Service → Repository. DTOs separate from entities. Module boundaries checked by ArchUnit. |
| NFR-11 | Deployability | Dockerised. Every milestone ships as an executable build (runnable JAR plus Docker Compose), never an IDE run. |
| NFR-12 | Data integrity | Foreign keys and unique indexes at the database level. Money stored as NUMERIC with a currency code. A partial unique index enforces one active offer per buyer per listing. |
| NFR-13 | Observability | Health endpoint and structured logs with request IDs. |
| NFR-14 | Documentation | OpenAPI/Swagger contract for all endpoints. All design and milestone documents in Confluence. Jira stories linked to GitHub branches and PRs. |

## C. Edits to Make in the HomeHub Design Document

| Section | Change |
|---|---|
| Title, headers, glossary | Rename HomeHub to Sakan (سكن). |
| §2 Roles | Add: how each role is obtained (self-selected at registration for Buyer/Tenant, Owner, Agent; admin-created for Maintenance Staff). Admin is a hardcoded seeded account. |
| §3 FRs | Replace with section A above. |
| §4 NFRs | Replace with section B above. NFR-07 now covers the whole backend. |
| §5 Architecture | Add: modular monolith, location hierarchy table, PostGIS enabled for later use, Lead entity. State "no maps in UI" still holds (dropdowns only). |
| §6 and §7 Use cases | Add Google Sign-In, Admin Sign-In, and Contact via WhatsApp. |
| §8 Class diagram | Add Location, Lead, `currency` on Property, Offer, Contract, and Payment. Change `price: Double` to `BigDecimal`. |
| §9 CRC cards | Add LeadService, LocationService, AuthService, and cards for every entity class (the spec asks for each class). |
| §10 Forms | Use the 24-item table from the proposal. |
| §11 Schedule | Move admin user management (FR-48) into Milestone 1. |
| Glossary: "Hardcoded Admin" | Rewrite as "seeded at startup from environment config." |
