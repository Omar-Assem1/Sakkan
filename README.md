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
