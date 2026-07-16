# Apifox Non-Performance API Test Suite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver an Apifox-importable interface collection and a CLI-created, executable native scenario for all 69 currently exposed non-performance API endpoints.

**Architecture:** Keep environment variables and credentials in a Postman environment JSON. Keep request order, token extraction, generated-resource capture, and assertions in a Postman Collection v2.1 document. Convert that document into a native Apifox scenario through the Apifox CLI because importing a Collection does not create a runnable scenario. Use a separate idempotent local MySQL seed script to make identities, permissions, data scopes, and baseline records deterministic.

**Tech Stack:** MySQL 8, Spring Boot API contract, Postman Collection v2.1 scripting (`pm.*`), Apifox.

---

### Task 1: Establish Local Test Identities

**Files:**
- Create: `tools/apifox/setup-local-api-test-accounts.sql`

- [ ] Insert the dedicated department, position, rank, manager, employee, and HR employee with fixed `990000x` IDs and `APIFOX_` identifiers.
- [ ] Insert or reactivate the seven test users and seven dedicated roles using the BCrypt hash for `admin123`.
- [ ] Map current non-performance permission-code menus to the roles, reset stale mappings for those users/roles, and configure ALL, DEPT_TREE, and SELF data scopes.
- [ ] Seed one leave-balance record for `api-employee`, commit the transaction, and query the resulting users/roles/scopes.

### Task 2: Define Environment and Collection Contract

**Files:**
- Create: `tools/apifox/hrpm-local-dev.postman_environment.json`
- Create: `tools/apifox/hrpm-non-performance-api-tests.postman_collection.json`

- [ ] Define `baseUrl`, all local credentials, role tokens, fixed test IDs, and generated-object variables.
- [ ] Add collection-level helpers for success/error assertions and a Bootstrap folder that logs in every account and extracts each access token.
- [ ] Add all 69 endpoint mappings under folders ordered by dependency: health/auth, organization, personnel, attendance, workflow, system/report.
- [ ] Capture response IDs and versions before any dependent request; use timestamped `APIFOX_` values for every mutable created record.
- [ ] Add validation and permission-denied requests that assert `VALIDATION_FAILED`, HTTP 403, or the endpoint's documented business status.

### Task 3: Document and Verify

**Files:**
- Create: `tools/apifox/README.md`
- Modify: `docs/superpowers/plans/2026-07-15-backend-api-test-plan.md`

- [ ] Document the required local backend URL, SQL command, Apifox import order, run order, and exclusion boundary.
- [ ] Correct the endpoint inventory from 70 to 69 because `PersonnelChangeController` exposes nine mappings, not ten.
- [ ] Parse both JSON artifacts, execute the SQL against local `hrpm`, query created accounts and grant counts, and run whitespace checks.

### Task 4: Create a Runnable Native Apifox Scenario

**Files:**
- Create: `tools/apifox/generate-native-scenario.mjs`
- Create: `tools/apifox/test-native-scenario-generator.mjs`
- Create: `tools/apifox/import-native-scenario.ps1`
- Modify: `tools/apifox/README.md`
- Modify: `.gitignore`

- [ ] Convert the 90 Postman requests into one Apifox bootstrap script and 90 `customHttp` steps, preserving URLs, methods, headers, bodies, pre-request scripts, and assertions.
- [ ] Generate separate metadata and update JSON files for the target Apifox project ID; do not commit them because they contain user-specific project IDs.
- [ ] Use the Apifox CLI to create the scenario before updating its steps; importing a Postman Collection alone must be documented as insufficient for the scenario runner.
- [ ] Verify the generated update has 91 steps and validate both JSON files against the Apifox CLI schemas.
- [ ] Check PowerShell syntax and document the Chinese, one-command path to create the scenario after `apifox auth login`.
