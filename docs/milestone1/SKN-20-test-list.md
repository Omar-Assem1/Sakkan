# SKN-20: Property CRUD API and lifecycle: tests-first list

Written before the implementation (spec requirement). Copy this page into the story's Confluence page.

Covers: FR-13, FR-14, FR-15, FR-18, NFR-03, NFR-08. Branch: `milestone_1/feat-SKN-20-property-crud-api`.

## Acceptance tests named in the Jira story

| # | Test | Where |
|---|---|---|
| 1 | A user who is not the owner or assigned agent cannot edit a listing (403, nothing saved) | `PropertyServiceTest.update_byNonOwner_...`, `PropertyControllerTest.update_byNonOwner_returns403` |
| 2 | An illegal status transition is rejected (409, nothing saved) | `PropertyServiceTest.publish_archivedListing_...`, `PropertyStatusTest` (every status pair) |

## Lifecycle (FR-18)

| From | Allowed targets |
|---|---|
| DRAFT | PUBLISHED, ARCHIVED |
| PUBLISHED | RENTED, SOLD, ARCHIVED |
| RENTED | PUBLISHED, ARCHIVED |
| SOLD | ARCHIVED |
| ARCHIVED | none (records are archived, never deleted) |

`PropertyStatusTest` checks all 25 ordered pairs: the 8 above must pass, the other 17 must fail.

## Further cases

- Create: owner gets a DRAFT owned by the caller; agent is also set as assigned agent; buyer/tenant gets 403.
- Update: owner and assigned agent can edit; admin cannot (FR-15); archived, rented and sold listings cannot be edited; missing id gives 404; owner and status never change through the request.
- Publish and archive: allowed from the right status; illegal status gives 409; non-owner gives 403.
- View: published is public; draft is visible only to owner, assigned agent and admin; everyone else gets 404 (existence not leaked).
- My Properties: returns owned or assigned listings, newest change first; page size capped at 50; negative page becomes 0; buyer gets 403.
- Validation (400): blank title, negative price, missing currency.
- Current user: anonymous gives empty; numeric name plus roles (with or without `ROLE_`) gives an `AuthenticatedUser`; non-numeric name gives empty.

## Out of scope for SKN-20

Filters and pagination for browsing (SKN-22), full-text search (SKN-23), image upload (SKN-24), forms (SKN-21).
