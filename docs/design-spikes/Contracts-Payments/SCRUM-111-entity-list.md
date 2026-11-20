# SCRUM-111: Contracts and Payments Entity List

This document defines the proposed domain entities for the contracts and payments design spike. It is based on FR-30–FR-34 in `docs/requirements/Sakan_02_Requirements_FR_NFR.md`. These are design proposals for Milestone 2, not production schema commitments.

## Contract

Represents a rental agreement created from an accepted offer.

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Primary key. |
| `offerId` | `Long` | Required reference to the accepted offer; conversion must be idempotent so one accepted offer creates at most one contract. |
| `propertyId` | `Long` | Required reference to the rented listing. |
| `ownerId` | `Long` | Required reference to the owner/lessor. |
| `tenantId` | `Long` | Required reference to the tenant/lessee. |
| `startDate` | `LocalDate` | Contract start date. |
| `endDate` | `LocalDate` | Contract end date; must be after `startDate`. |
| `monthlyRent` | `BigDecimal` | Positive agreed monthly amount. |
| `currency` | `CurrencyCode` | `EGP` or `USD`; preserve the accepted offer's currency. |
| `status` | `ContractStatus` | `DRAFT`, `ACTIVE`, `COMPLETED`, or `TERMINATED`. Only activation generates scheduled payments. |
| `createdAt` | `Instant` | Creation timestamp. |
| `updatedAt` | `Instant` | Last update timestamp. |

**Relationships:** one accepted `Offer` converts to zero or one `Contract`; a `Contract` belongs to one `Property`, one owner, and one tenant, and owns zero or many `Payment` records. Contract parties and agreed financial terms should be snapshotted so later profile/listing changes do not rewrite the agreement.

## Payment

Represents one monthly installment in a contract's payment schedule. The milestone simulates payment; this entity records status and does not process a real payment gateway transaction.

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Primary key. |
| `contractId` | `Long` | Required reference to the parent contract. |
| `periodStart` | `LocalDate` | First date covered by this installment. |
| `periodEnd` | `LocalDate` | Last date covered by this installment. |
| `dueDate` | `LocalDate` | Date the installment is due. |
| `amount` | `BigDecimal` | Positive scheduled amount. |
| `currency` | `CurrencyCode` | `EGP` or `USD`; copied from the contract. |
| `status` | `PaymentStatus` | `PENDING`, `PAID`, or `OVERDUE`. |
| `paidAt` | `Instant?` | Set when marked paid; otherwise null. |
| `createdAt` | `Instant` | Schedule creation timestamp. |
| `updatedAt` | `Instant` | Last status-change timestamp. |

**Constraints:** unique `(contractId, periodStart)` prevents duplicate installments. `OVERDUE` is derived by an idempotent scheduled job when `status = PENDING` and `dueDate` is before the current date. Marking paid records the simulated payment time; no card, gateway, or bank data is stored.

## Schedule generation and lifecycle

When a contract changes to `ACTIVE`, create one `PENDING` payment per rental month from `startDate` through `endDate`, with each row's amount and currency copied from the contract. A partial final month remains a product decision for the design review; until settled, do not silently prorate it. Schedule generation must be transactional and idempotent, backed by the unique `(contractId, periodStart)` constraint. Completion or termination does not delete historical payments.

## Out of scope

This entity list does not define a real payment provider, refunds, deposits, fees, contract document storage/signatures, proration rules, or multi-currency conversion. Resolve these in the parent design spike before implementation.

