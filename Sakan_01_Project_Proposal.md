# Sakan (سكن): Project Proposal

**Course:** CSE322 Software Engineering, Computer and Systems Engineering Dept., Alexandria University (Third Year)
**Deliverable:** Project Proposal (due Saturday 10 October 2026; resubmission 17 October 2026)
**Team:** 6 members. Fill in names and IDs below.

| # | Name | Student ID | Primary module |
|---|------|-----------|----------------|
| 1 | _TBD_ | _TBD_ | Auth, Roles, Admin |
| 2 | _TBD_ | _TBD_ | Property Listings, Search |
| 3 | _TBD_ | _TBD_ | Viewings and Scheduling |
| 4 | _TBD_ | _TBD_ | Contracts and Payments |
| 5 | _TBD_ | _TBD_ | Maintenance Tickets |
| 6 | _TBD_ | _TBD_ | Offers, Reviews, Roommate Matching |

---

## 1. Problem Statement

Finding, renting, buying, and managing property in Egypt is highly fragmented. Buyers and tenants search across scattered websites, Facebook groups, and brokers' WhatsApp messages, and they rarely get a consistent view of price, location, and availability. Individual owners struggle to filter serious clients, while agents lose track of viewings, offers, and the leads they generate. Egyptian users also expect a bilingual (Arabic and English) experience, prices in both EGP and USD, and a location structure that reflects how people actually search, such as New Cairo, Sheikh Zayed, or the North Coast. After a deal is agreed, rent payments, contracts, and maintenance issues are usually handled through phone calls and paper, so nothing is tracked and nobody is accountable. Young professionals and university students have an additional problem: finding a compatible roommate in a new city. **Sakan** (سكن) is a single web platform that addresses the whole journey. Owners and agents publish properties, buyers and tenants search with advanced filters and book viewings, both sides negotiate offers through a structured workflow, and, once a rental starts, the platform tracks the contract, rent payments, and maintenance tickets. Sakan also matches compatible roommates and keeps the marketplace trustworthy through agent reviews and admin moderation.

## 2. Vision and Goals

| Goal | Measure of success |
|---|---|
| Centralise property search and listing | Sale and rental listings on one platform, searchable by location, price, type, bedrooms, amenities |
| Egypt-ready experience | Arabic/English with RTL, EGP/USD pricing, seeded Governorate → City → District → Compound hierarchy |
| Structured offer negotiation | Offer and counter-offer with automatic expiry |
| Direct lead routing | "Contact via WhatsApp" button that records a lead for the owner or agent |
| Digital contracts and payments | Payment schedule per contract, zero paper-based payment records |
| Transparent maintenance | Four-status ticket lifecycle (Open → Assigned → In Progress → Resolved) |
| Roommate matching | Compatibility score (0 to 100) from profile preferences |
| Trustworthy marketplace | Admin moderation queue and agent ratings |

## 3. Possible Users of the System

| User | Description |
|---|---|
| **Buyer / Tenant** | Searches properties, books viewings, makes offers, pays rent, requests maintenance, finds roommates, reviews agents |
| **Owner** | Publishes and manages listings, reviews offers and leads, creates rental contracts, tracks payments |
| **Agent / Broker** | Manages listings for owners, schedules viewings, makes and counters offers, receives ratings |
| **Maintenance Staff** | Views assigned tickets and updates their status |
| **Admin** | Manages users and roles, moderates reported listings. One hardcoded admin account can promote other admins |

## 4. Functionalities

1. **Authentication and user management:** sign up/in with username and password, sign up/in with Google (OAuth2), a dedicated admin sign-in, one hardcoded admin who promotes other admins, role-based access control.
2. **Location and localisation:** seeded bilingual location hierarchy, Arabic/English switch with right-to-left layout, EGP/USD pricing.
3. **Property listings and search:** create, edit, publish, and archive listings with images and amenities; filter by location, price range, type, bedrooms, and amenities; full-text search; listing status (Available, Rented, Sold).
4. **Viewing appointments:** owners and agents publish time slots, buyers and tenants request a slot, the system prevents double-booking, and viewings move through Requested → Confirmed → Cancelled/Completed.
5. **Offers and negotiation:** submit an offer, accept, reject, or counter, automatic expiry, one active offer per buyer per listing.
6. **Rental contracts and payments:** convert an accepted offer into a contract, generate a monthly payment schedule, record payments (simulated), mark overdue payments, view payment history.
7. **Maintenance tickets:** tenants file requests, owners or admins assign staff, staff update status, everyone sees the ticket history.
8. **Leads:** WhatsApp contact button that opens a prefilled chat and records a lead; leads inbox for owners and agents.
9. **Roommate matching:** roommate profiles, compatibility scoring, ranked suggestions, in-platform contact.
10. **Reviews and agent directory:** rate agents after a completed viewing or contract, public agent profiles, directory sorted by rating.
11. **Moderation:** report a listing, admin moderation queue, remove or dismiss reports.

## 5. UI Forms and Listings (24 total; minimum required is 10)

| # | Type | Name | Module | Roles |
|---|------|------|--------|-------|
| 1 | Form | Sign Up / Sign In (username + password, Google button) | Auth | All |
| 2 | Form | Admin Sign In | Auth | Admin |
| 3 | Listing | Browse Properties (filters + search) | Listings | Buyer/Tenant |
| 4 | Form | Add / Edit Property | Listings | Owner, Agent |
| 5 | Listing | My Properties | Listings | Owner, Agent |
| 6 | Form | Request a Viewing | Viewings | Buyer/Tenant |
| 7 | Form | Set Viewing Availability | Viewings | Owner, Agent |
| 8 | Listing | My Viewings / Agent Schedule | Viewings | All |
| 9 | Form | Make / Counter Offer | Offers | Buyer, Owner, Agent |
| 10 | Listing | Offers Received / Sent | Offers | Buyer, Owner, Agent |
| 11 | Form | Create Rental Contract | Contracts | Owner, Agent |
| 12 | Listing | My Contracts | Contracts | Owner, Tenant |
| 13 | Form | Record Rent Payment | Payments | Tenant, Owner |
| 14 | Listing | Payment History | Payments | Owner, Tenant |
| 15 | Form | Submit Maintenance Request | Maintenance | Tenant |
| 16 | Listing | Maintenance Tickets (with assign action) | Maintenance | All |
| 17 | Form | Roommate Profile and Preferences | Roommates | Tenant |
| 18 | Listing | Suggested Roommate Matches | Roommates | Tenant |
| 19 | Form | Review an Agent | Reviews | Tenant |
| 20 | Listing | Agents Directory | Reviews | Buyer/Tenant |
| 21 | Form | Report a Listing | Moderation | All |
| 22 | Listing | Admin: Manage Users | Admin | Admin |
| 23 | Listing | Admin: Reported Listings | Admin | Admin |
| 24 | Listing | Leads Inbox | Leads | Owner, Agent |

12 forms and 12 listings.

## 6. Compliance with the Mandatory Requirements (Course Spec §2.2)

| Requirement | How Sakan meets it |
|---|---|
| Sign up/in with Gmail | Google OAuth2 login. The backend then issues Sakan's own JWT |
| Sign up/in with username and password | Registration and login forms (#1) |
| Admin sign-in with basic authentication | Dedicated admin form (#2) |
| One hardcoded admin who can promote others | Admin account seeded at first startup from environment configuration. It can promote users through form #22 |
| At least 10 forms/listings | 24 (section 5) |
| Relational database | PostgreSQL |
| Site data and user data stored in the DB | **Site data:** seeded locations (EN/AR), amenities, property types, demo listings. **User data:** accounts, credentials, and everything submitted through forms |

## 7. Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Backend | Spring Boot 3, Spring Security (JWT + OAuth2 client), Spring Data JPA / Hibernate, Flyway |
| Frontend | Angular 17+ (standalone components, Signals, Tailwind CSS, Transloco for AR/EN) |
| Database | PostgreSQL (PostGIS extension enabled for future map and radius features) |
| Testing | JUnit 5, Mockito, Testcontainers, JaCoCo (≥ 60% backend coverage), Playwright |
| Delivery | Docker and Docker Compose, GitHub Actions |
| Process | Jira, Confluence, GitHub (branching model as required by the course) |

## 8. Scope Notes

**In scope for the course:** everything in sections 4 and 5.
**Simulated:** payments (status tracking only, no real gateway) and WhatsApp (a deep link, not the WhatsApp Business API).
**Planned beyond the course (not committed):** real SMS/WhatsApp OTP, map view, Meilisearch for search, CDN-backed image delivery, saved searches, agency accounts, monitoring stack.

## 9. Milestone Plan

| Phase | Due | Contents |
|---|---|---|
| Proposal | 10 Oct 2026 (resubmission 17 Oct) | This document |
| Design | 31 Oct 2026 | Requirements, architecture, interaction diagrams, detailed class diagram, CRC cards, schedule |
| Milestone 1 | 14 Nov 2026 | Auth (basic, Google, seeded admin), RBAC, user management, listings and search, images, viewings |
| Milestone 2 | 28 Nov 2026 | Offers, contracts, payments, maintenance, leads, Arabic/RTL on core pages |
| Milestone 3 | 12 Dec 2026 | Roommates, reviews, moderation, surprise requirement |
