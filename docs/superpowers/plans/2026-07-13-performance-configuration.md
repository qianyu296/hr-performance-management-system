# Performance Configuration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver metrics, versioned performance schemes, level rules, performance cycles, and task snapshots at cycle start.

**Architecture:** The backend owns configuration validation and transactional cycle-start snapshot creation. Vue presents configuration through typed REST clients. Existing `perf_*` tables are used without altering applied migrations.

**Tech Stack:** Spring Boot, Spring Security, MyBatis, MySQL, JUnit 5, Vue 3, TypeScript, Element Plus.

---

### Task 1: Performance configuration persistence and API

**Files:**
- Create: `backend/src/main/java/com/hrpm/{controller,service,mapper,dto,entity,vo}/Performance*.java`
- Create: `backend/src/test/java/com/hrpm/controller/PerformanceConfigurationApiIntegrationTests.java`
- Modify: `docs/hrpm-v1.openapi.yaml`

- [ ] Write failing `MockMvc` tests for `performance:config` authorization, metric create/update, draft scheme-version configuration, duplicate metric codes, invalid weights, overlapping rules, version enable, and stale updates.
- [ ] Run `mvn -Dtest=PerformanceConfigurationApiIntegrationTests test`; expect missing endpoints.
- [ ] Implement metric, scheme, scheme-version, item, and level-rule mappers, services, DTOs, VOs, controllers, and OpenAPI operations. Enforce draft-only changes and immutable enabled versions.
- [ ] Re-run the focused suite; expect zero failures.

### Task 2: Cycle start and task snapshots

**Files:**
- Create: `backend/src/main/java/com/hrpm/service/PerformanceCycleService.java`
- Modify: `backend/src/main/java/com/hrpm/mapper/{EmployeeMapper,PerformanceMapper}.java`
- Modify: `backend/src/test/java/com/hrpm/controller/PerformanceConfigurationApiIntegrationTests.java`

- [ ] Add failing tests that start a cycle from an enabled version, produce one task per active employee, retain scheme and organization snapshots, and reject a second start.
- [ ] Run the focused suite; expect the cycle-start assertions to fail.
- [ ] Implement version-checked cycle creation/start and transactional task/task-item snapshots with `PENDING_SELF_ASSESSMENT` status.
- [ ] Re-run the focused suite; expect zero failures.

### Task 3: Performance configuration workspace

**Files:**
- Create: `frontend/src/api/performance.ts`
- Create: `frontend/src/views/PerformanceConfigurationView.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `docs/04-接口设计.md`

- [ ] Replace the performance placeholder route with a workspace for metrics, scheme versions, and cycles; include enable and cycle-start confirmations.
- [ ] Use typed API methods only, handle loading, empty, validation, conflict, and request-error states.
- [ ] Run `npm run typecheck` and `npm run build`; expect both to exit 0.

### Task 4: Full verification

**Files:**
- Modify: `docs/hrpm-v1.openapi.yaml`

- [ ] Run `mvn test`, `npm run typecheck`, and `npm run build` after all tasks. Keep Docker-only Testcontainers skips distinct from failures.
- [ ] Commit with `feat: add performance configuration`.
