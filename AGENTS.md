# Repository Guidelines

## Project Structure & Module Organization

This repository is split into two applications:

- `backend/`: Spring Boot 3.4 API using Java 17, Spring Security, MyBatis, and Flyway. Production code is under `src/main/java/com/hrpm`, configuration and migrations are in `src/main/resources`, and tests are in `src/test/java`.
- `frontend/`: Vue 3 and TypeScript application built with Vite. Views live in `src/views`, routing in `src/router`, API clients in `src/api`, and shared styling in `src/styles.css`.
- `docs/`: requirements, architecture, database, API, testing, and delivery documentation. Keep `docs/hrpm-v1.openapi.yaml` synchronized with API changes.
- `docker-compose.yml`: local MySQL, Redis, and MinIO services.

## Build, Test, and Development Commands

Run infrastructure from the repository root:

```powershell
docker compose up -d
```

Backend commands run from `backend/`:

```powershell
mvn spring-boot:run  # Start the API on port 8080
mvn test             # Run JUnit tests
mvn clean package    # Build and test the executable JAR
```

Frontend commands run from `frontend/`:

```powershell
npm ci               # Install locked dependencies
npm run dev          # Start Vite on port 5173
npm run typecheck    # Validate Vue and TypeScript types
npm run build        # Type-check and create the production bundle
```

## Coding Style & Naming Conventions

Use 2-space indentation in XML, YAML, JSON, Vue, and TypeScript; use 4 spaces in Java. Follow standard Java naming: `PascalCase` classes, `camelCase` members, and lowercase packages under `com.hrpm`. Vue components and views use `PascalCase` filenames such as `DashboardView.vue`. Prefer the `@/` alias for frontend imports. No formatter or linter is configured, so match nearby code and keep changes focused.

## Testing Guidelines

Backend tests use JUnit 5 via `spring-boot-starter-test`; name test classes `*Tests.java` and mirror production package paths. Add focused tests for controllers, security rules, services, and database behavior. The frontend currently has no test runner, so `npm run typecheck` and `npm run build` are required checks for frontend changes. Add a test framework before introducing frontend unit tests rather than ad hoc scripts.

## Database, Commits & Pull Requests

Database changes must be additive Flyway migrations named `V###__description.sql`; never edit an applied migration. The current history uses Conventional Commit style (`chore: initialize ...`), so use concise prefixes such as `feat:`, `fix:`, `docs:`, or `test:`. Pull requests should describe scope, list verification commands, link relevant issues, note migrations or API changes, and include screenshots for visible UI changes.
