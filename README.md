# HR Performance Management System

基于 Spring Boot、Vue 3、MySQL、Redis 与 MinIO 的人力资源与绩效管理系统。

## Project Layout

- `backend/`: Spring Boot API, Flyway migrations, security and domain modules.
- `frontend/`: Vue 3 + TypeScript management application.
- `docs/`: product, architecture, API and implementation baseline.
- `docker-compose.yml`: local MySQL, Redis and MinIO.

## Local Development

1. Start dependencies: `docker compose up -d`.
2. Start backend: `cd backend; mvn spring-boot:run`.
3. Start frontend: `cd frontend; npm install; npm run dev`.

For an existing local MySQL instance, provide `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` as environment variables rather than changing `application.yml`.

The backend runs on `http://localhost:8080`; the frontend runs on `http://localhost:5173`.

## Documentation

Development follows [09-开发交付清单.md](docs/09-开发交付清单.md). Database changes are Flyway-only and API changes must update `docs/hrpm-v1.openapi.yaml`.
