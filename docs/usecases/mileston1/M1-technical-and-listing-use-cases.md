# Milestone 1 Use Case Flows for Every Story

This document has one named flow for each of the 28 stories in the supplied Milestone 1 Jira Breakdown. Each heading includes the planning story ID (SKN) and its actual Jira key (SCRUM). Jira's populated milestone sprint is **Sprint M1** (sprint ID 3); the separately named **SCRUM Sprint 1** (ID 1) is empty.

## UC-08 Configure and Run the CI Pipeline (SKN-5, SCRUM-9)

[PNG activity flow](SCRUM-9_UC-08_Flow.png)
**Actor:** Contributor. **Precondition:** repository and pull request exist.

**Main flow:**
1. The contributor pushes a branch or opens a pull request.
2. CI checks out the code and runs backend build and tests.
3. CI generates the JaCoCo report and checks the 60% backend coverage gate.
4. CI runs Angular lint, unit tests, and production build.
5. CI reports the result to the pull request; a passing result permits merge under branch protection.

**Alternate flows:** Any failed job or coverage below 60% marks the check failed and blocks merge until corrected.
**Postcondition:** the pull request has a reproducible pass/fail result.

## UC-09 Start the Application with Docker Compose (SKN-6, SCRUM-14)

[PNG activity flow](SCRUM-14_UC-09_Flow.png)
**Actor:** Developer or evaluator. **Precondition:** Docker is installed and the repository is available.

**Main flow:**
1. The actor follows the README run guide and starts the Compose stack.
2. Compose starts PostgreSQL/PostGIS, API, and web services and mounts the uploads volume.
3. The API connects to the database and the web app becomes available.
4. The actor opens the login page.

**Alternate flows:** A service fails to build or start; Compose reports the failing service and startup does not count as successful.
**Postcondition:** the app is runnable from a clean clone with persistent upload storage.

## UC-10 Run the Automated Test Suites (SKN-7, SCRUM-18)

[PNG activity flow](SCRUM-18_UC-10_Flow.png)
**Actor:** Contributor or CI. **Precondition:** backend and frontend test dependencies are available.

**Main flow:**
1. The actor starts the regression suite.
2. Integration tests provision PostgreSQL using Testcontainers; MockMvc helpers exercise authentication and authorization responses.
3. Playwright opens the application, signs in, and browses listings.
4. The suite reports results by test tag.

**Alternate flows:** A test or required container fails; the suite reports the failure and the build is not accepted.
**Postcondition:** test results cover unit, integration, authorization, and browser smoke paths.

## UC-11 Initialize the Application Database (SKN-8, SCRUM-23)

[PNG activity flow](SCRUM-23_UC-11_Flow.png)
**Actor:** Application operator. **Precondition:** an empty PostgreSQL database is configured.

**Main flow:**
1. The application starts and Flyway checks the schema history.
2. Flyway applies the baseline schema for identity, location, amenities, properties, images, and viewings.
3. Seed data inserts bilingual locations, amenities, and property types.
4. Constraints and indexes enforce valid and efficient data access.

**Alternate flows:** A migration or seed operation fails; startup reports the failed migration and does not proceed as initialized.
**Postcondition:** the database has the versioned M1 schema and reference data.

## UC-12 Retrieve Locality Options (SKN-9, SCRUM-28)

[PNG activity flow](SCRUM-28_UC-12_Flow.png)
**Actor:** Property owner or listing editor. **Precondition:** the client is creating or editing a property.

**Main flow:**
1. The client requests governorates.
2. The actor selects a governorate; the client requests its cities.
3. The actor selects a city; the client requests districts, then compounds as applicable.
4. The API returns each level filtered by its parent and localized using `Accept-Language`.

**Alternate flows:** The parent has no child locations; the API returns an empty list. Unsupported language falls back to the default locale.
**Postcondition:** the client has valid cascading locality choices.

## UC-13 Apply a Valid Status Transition (SKN-10, SCRUM-31)

[PNG activity flow](SCRUM-31_UC-13_Flow.png)
**Actor:** Application service. **Precondition:** a property or viewing exists with a current status.

**Main flow:**
1. A service requests a transition from the current status to a target status.
2. The shared transition framework checks the allowed-transition map.
3. If allowed, the service stores the new status.

**Alternate flows:** A transition is not in the map; the framework rejects it and the record remains unchanged.
**Postcondition:** only legal property and viewing status changes are applied.

## UC-14 Generate Development Demo Listings (SKN-11, SCRUM-35)

[PNG activity flow](SCRUM-35_UC-14_Flow.png)
**Actor:** Developer. **Precondition:** the development profile is enabled and reference data exists.

**Main flow:**
1. The developer starts the application in the development profile.
2. The generator creates 5,000 varied property listings across locations and types.
3. The developer uses the listings to exercise browse and search flows.

**Alternate flows:** The test profile is active; the generator is skipped.
**Postcondition:** development has representative demo data and automated tests remain isolated from it.

## UC-15 Return a Consistent API Error and API Description (SKN-13, SCRUM-43)

[PNG activity flow](SCRUM-43_UC-15_Flow.png)
**Actor:** API client or developer. **Precondition:** the API is running.

**Main flow:**
1. A client submits an invalid or unauthorized request.
2. The API advice handler converts the exception into a ProblemDetail response.
3. A developer opens the published OpenAPI specification.
4. The build generates the Angular API client from that specification.

**Alternate flows:** A valid request proceeds normally; an undocumented response is treated as a specification defect.
**Postcondition:** clients receive a consistent error shape and developers can use the generated client.

## UC-16 Prepare the Team's Jira and Confluence Process (SKN-14, SCRUM-47)

[PNG activity flow](SCRUM-47_UC-16_Flow.png)
**Actor:** Project team member. **Precondition:** project tools are accessible.

**Main flow:**
1. The member configures the Jira board, issue types, workflow columns, and repository linking.
2. The member prepares Confluence spaces and templates.
3. The member creates a sprint report skeleton.

**Alternate flows:** A required permission or integration is unavailable; the member records the blocker for the project team.
**Postcondition:** the team has a consistent workflow and reporting structure.

## UC-17 Upload Property Images (SKN-24, SCRUM-89)

[PNG activity flow](SCRUM-89_UC-17_Flow.png)
**Actor:** Property owner or authorized agent. **Precondition:** the actor can edit the property and the property exists.

**Main flow:**
1. The actor selects images in the property editor.
2. The client compresses the images before upload.
3. The API validates media type, file size, and image count.
4. The system stores accepted originals and generates thumbnails.
5. The property view displays the stored images.

**Alternate flows:** Invalid type, excess size, or excess count is rejected with an actionable error; rejected images are not attached.
**Postcondition:** validated images and thumbnails are associated with the property.

## UC-18 Record the M1 Release Candidate (SKN-31, SCRUM-118)

[PNG activity flow](SCRUM-118_UC-18_Flow.png)
**Actor:** Release owner. **Precondition:** M1 changes are merged to the milestone branch.

**Main flow:**
1. The owner runs the full regression suite.
2. The owner builds the executable and rehearses it on a clean machine.
3. After successful verification, the owner tags the release candidate and merges the milestone branch to main.

**Alternate flows:** Regression or clean-machine startup fails; the owner records the failure and resolves it before tagging.
**Postcondition:** a verified M1 release candidate is available.

## UC-19 Publish a Status-Changed Domain Event (SKN-32, SCRUM-122)

[PNG activity flow](SCRUM-122_UC-19_Flow.png)
**Actor:** Application service and event listener. **Precondition:** a legal status transition is requested.

**Main flow:**
1. The transition framework validates and applies the new status.
2. The application publishes a `StatusChangedEvent` with the relevant transition context.
3. Registered listeners receive the event.

**Alternate flows:** An illegal transition is rejected and no status-changed event is emitted.
**Postcondition:** downstream application components can react to successful status changes.

## UC-20 Explore the Maintenance Module Design (SKN-28, SCRUM-106)

[PNG activity flow](SCRUM-106_UC-20_Flow.png)
**Actor:** Design team member. **Precondition:** maintenance requirements are available for the design spike.

**Main flow:**
1. The member identifies maintenance entities and responsibilities.
2. The member writes CRC cards.
3. The member drafts a sequence diagram for a maintenance workflow.

**Alternate flows:** A requirement is unclear; it is recorded as an open design question.
**Postcondition:** a draft design is available to guide the later maintenance module.

## UC-21 Explore the Contracts and Payments Design (SKN-29, SCRUM-110)

[PNG activity flow](SCRUM-110_UC-21_Flow.png)
**Actor:** Design team member. **Precondition:** contract and payment requirements are available for the design spike.

**Main flow:**
1. The member identifies contract and payment entities and responsibilities.
2. The member writes CRC cards.
3. The member drafts a sequence diagram for a contract or payment workflow.

**Alternate flows:** A policy or state rule is unclear; it is recorded as an open design question.
**Postcondition:** a draft design is available to guide the later contracts and payments module.

## UC-22 Explore the Offers Module Design (SKN-30, SCRUM-114)

[PNG activity flow](SCRUM-114_UC-22_Flow.png)
**Actor:** Design team member. **Precondition:** offer requirements are available for the design spike.

**Main flow:**
1. The member identifies offer entities and responsibilities.
2. The member writes CRC cards.
3. The member drafts a sequence diagram for submitting and responding to an offer.

**Alternate flows:** An offer rule is unclear; it is recorded as an open design question.
**Postcondition:** a draft design is available to guide the later offers module.

## UC-23 Configure the Angular Application Shell (SKN-12, SCRUM-38)

[PNG activity flow](SCRUM-38_UC-23_Flow.png)
**Actor:** Visitor or signed-in user. **Precondition:** the web application is available.

**Main flow:**
1. The user opens the application and the shell loads the shared layout and navigation.
2. The user navigates to a route; the router loads the matching feature lazily.
3. For a protected route, the auth interceptor attaches the stored JWT to API requests and the route guard checks the user's role.
4. The user switches between Arabic and English; the application updates its language, document direction, and localized labels.

**Alternate flows:** An expired or missing token sends the user to sign-in; a user without the required role is denied access. A translation key missing in the selected locale falls back to the default language.
**Postcondition:** the requested permitted view is rendered with the chosen language and direction.

## UC-24 Register and Sign In with JWT (SKN-15, SCRUM-51)

[PNG activity flow](SCRUM-51_UC-24_Flow.png)
**Actor:** Visitor. **Precondition:** the visitor is not signed in.

**Main flow:**
1. The visitor opens Form 1, enters username, email, password, phone, and an allowed user role, then submits registration.
2. The system validates the fields and uniqueness of username and email, hashes the password, and stores the account and role.
3. The visitor enters username and password on the sign-in form.
4. The system verifies the credentials, issues a signed JWT, and returns it to the client.
5. The client stores the token and uses it on later API requests.

**Alternate flows:** Invalid fields or a forbidden role are rejected; duplicate username or email returns a conflict; incorrect credentials return 401.
**Postcondition:** the account exists and, after successful sign-in, the client holds a valid JWT.

## UC-25 Sign In with Google (SKN-16, SCRUM-56)

[PNG activity flow](SCRUM-56_UC-25_Flow.png)
**Actors:** Visitor and Google identity provider. **Precondition:** Google OAuth is configured and the visitor is not signed in.

**Main flow:**
1. The visitor chooses Continue with Google.
2. Google authenticates the visitor and returns the verified profile to the OAuth callback.
3. The system finds the account by provider identity or email; if no account exists, it creates one with the default buyer/tenant role.
4. The system issues a Sakan JWT and redirects to the client callback.
5. The client stores the token and opens the application.

**Alternate flows:** A returning Google user reuses the existing account; a conflicting local account follows the configured linking policy; cancellation or provider failure returns the visitor to sign-in with an error.
**Postcondition:** the visitor is signed in with a Sakan JWT, or remains unauthenticated if the flow fails.

## UC-26 Seed an Administrator and Sign In (SKN-17, SCRUM-61)

[PNG activity flow](SCRUM-61_UC-26_Flow.png)
**Actors:** Application startup process and administrator. **Precondition:** admin credentials are provided through environment configuration.

**Main flow:**
1. On startup, the system checks whether the protected seeded admin already exists.
2. If none exists, it creates the admin with a hashed password and ADMIN role.
3. The administrator opens Form 2 and submits credentials.
4. The system verifies both the credentials and ADMIN role, issues a JWT, and opens the admin area.

**Alternate flows:** Existing seeded admin means no duplicate is created; missing configuration fails startup with a clear message; invalid credentials or a non-admin account receives a generic authentication failure. Secrets are never logged.
**Postcondition:** exactly one seeded admin is maintained and a valid admin can sign in.

## UC-27 Enforce Role-Based Access Control (SKN-18, SCRUM-65)

[PNG activity flow](SCRUM-65_UC-27_Flow.png)
**Actor:** API or web client. **Precondition:** the client requests a protected endpoint or route.

**Main flow:**
1. The system validates the JWT, if present.
2. The API security rule or Angular route guard checks the user's role.
3. The service checks resource ownership when the operation requires it.
4. The system allows the operation only when authentication, role, and ownership checks pass.

**Alternate flows:** Missing, expired, or invalid credentials return 401; an insufficient role or failed ownership check returns 403.
**Postcondition:** protected data or actions are available only to authorized callers.

## UC-28 Manage Users and Roles (SKN-19, SCRUM-69)

[PNG activity flow](SCRUM-69_UC-28_Flow.png)
**Actor:** Administrator. **Precondition:** the administrator is signed in.

**Main flow:**
1. The administrator opens Listing 22 and filters or pages through users.
2. The system returns the matching users.
3. The administrator promotes a user to ADMIN or demotes a promoted admin.
4. The system records the role change and who granted it.

**Alternate flows:** A non-admin caller receives 403; a missing user returns 404; the seeded admin cannot be demoted or deleted.
**Postcondition:** the user's role is updated and the protected seeded account remains intact.

## UC-29 Create and Manage a Property Listing (SKN-20, SCRUM-73)

[PNG activity flow](SCRUM-73_UC-29_Flow.png)
**Actor:** Owner or assigned agent. **Precondition:** the actor is authenticated and permitted to manage the property.

**Main flow:**
1. The actor submits property details for creation.
2. The system validates the details and stores the listing as a draft.
3. The actor updates the listing, then publishes it or archives it when ready.
4. The system checks ownership and applies each allowed lifecycle transition.

**Alternate flows:** Invalid data is rejected; a non-owner or unassigned agent cannot edit or archive; an illegal status transition is rejected.
**Postcondition:** the listing is saved with an allowed status and the correct owner.

## UC-30 Add or Edit Property and Review My Properties (SKN-21, SCRUM-77)

[PNG activity flow](SCRUM-77_UC-30_Flow.png)
**Actor:** Owner or assigned agent. **Precondition:** the actor is signed in and has permission for the listing.

**Main flow:**
1. The actor opens the add or edit property form.
2. The actor enters listing details and selects locality from cascading location controls.
3. The system validates the form and displays localized messages.
4. The actor saves the listing and opens My Properties to view its status and actions.

**Alternate flows:** Invalid fields are highlighted; changing a parent location clears dependent selections; the actor cannot edit a listing they do not control.
**Postcondition:** the listing is saved and appears in the actor's permitted property list.

## UC-31 Browse Properties with Filters (SKN-22, SCRUM-81)

[PNG activity flow](SCRUM-81_UC-31_Flow.png)
**Actor:** Visitor or signed-in buyer/tenant. **Precondition:** published listings are available.

**Main flow:**
1. The actor opens the browse listings page.
2. The actor applies location, price, type, purpose, bedroom, and amenity filters.
3. The system returns matching published listings with pagination and sorting.
4. The actor moves between result pages or changes the sort order.

**Alternate flows:** No listings match; the page shows an empty state and allows the actor to adjust filters. Invalid filter values are rejected.
**Postcondition:** the actor sees the requested page of matching published listings.

## UC-32 Search Listings by Text (SKN-23, SCRUM-85)

[PNG activity flow](SCRUM-85_UC-32_Flow.png)
**Actor:** Visitor or signed-in buyer/tenant. **Precondition:** the browse listings service is available.

**Main flow:**
1. The actor enters a search phrase in Arabic or English.
2. The system searches listing titles and descriptions and applies any selected listing filters.
3. The system returns matching published listings in the browse results.

**Alternate flows:** No result matches; the page shows an empty state. Search input is treated as data and cannot alter the query structure.
**Postcondition:** the displayed results match the submitted text query.

## UC-33 Set Viewing Availability (SKN-25, SCRUM-94)

[PNG activity flow](SCRUM-94_UC-33_Flow.png)
**Actor:** Owner or assigned agent. **Precondition:** the actor is signed in and controls the property.

**Main flow:**
1. The actor opens Form 7 for the property.
2. The actor enters one or more available time slots.
3. The system validates and stores the slots.
4. The slots become available to buyers/tenants requesting a viewing.

**Alternate flows:** An overlapping or invalid slot is rejected and the actor is asked to correct it.
**Postcondition:** valid viewing slots are associated with the property.

## UC-34 Request a Viewing Without Double-Booking (SKN-26, SCRUM-98)

[PNG activity flow](SCRUM-98_UC-34_Flow.png)
**Actor:** Buyer/tenant. **Precondition:** the actor is signed in and an available slot exists for a published property.

**Main flow:**
1. The actor selects a property and an available slot in Form 6.
2. The system checks that the slot is still available.
3. The system creates a viewing request and marks the slot as occupied for active booking purposes.
4. The system confirms the request to the actor.

**Alternate flows:** Another request has already claimed the slot, including a concurrent request; the system rejects this booking and asks the actor to choose another time.
**Postcondition:** at most one active viewing request occupies the slot.

## UC-35 Manage Viewing Status and My Viewings (SKN-27, SCRUM-102)

[PNG activity flow](SCRUM-102_UC-35_Flow.png)
**Actors:** Buyer/tenant and owner or assigned agent. **Precondition:** a viewing request exists and the actor is a participant.

**Main flow:**
1. The actor opens My Viewings and the system lists the viewings visible to that role.
2. The owner/agent confirms a requested viewing, or a participant cancels it when permitted.
3. After the appointment, an authorized participant marks it completed.
4. The system validates each status transition and updates the appropriate upcoming or past view.

**Alternate flows:** An invalid transition is rejected; a nonparticipant cannot view or change the appointment; unavailable records are not disclosed.
**Postcondition:** the viewing has a valid status and is shown to its authorized participants.

