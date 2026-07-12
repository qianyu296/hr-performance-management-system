# Organization and Employee Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a usable organization and employee master-data module with tested Spring Boot APIs and an Element Plus management screen.

**Architecture:** Extend the existing layered Spring Boot structure with focused controller, service, mapper, entity, DTO, and VO classes. Reuse the existing MySQL tables and Spring Security permission model, then connect typed Vue API clients to a department-tree and employee-directory screen.

**Tech Stack:** Java 17, Spring Boot 3.4, Spring Security, MyBatis, MySQL/Flyway, JUnit 5, Vue 3, TypeScript, Vite, Element Plus

---

### Task 1: Department Tree Query

**Files:**
- Modify: `backend/src/main/java/com/hrpm/controller/DepartmentController.java`
- Modify: `backend/src/main/java/com/hrpm/service/DepartmentService.java`
- Modify: `backend/src/main/java/com/hrpm/mapper/DepartmentMapper.java`
- Modify: `backend/src/main/java/com/hrpm/vo/DepartmentVO.java`
- Test: `backend/src/test/java/com/hrpm/controller/DepartmentApiIntegrationTests.java`

- [ ] **Step 1: Write failing API tests**

Add authenticated `GET /departments` coverage that seeds a root and child department, expects parent-before-child ordering, and verifies a caller without `org:read` receives `403`.

```java
mockMvc.perform(get("/departments").header("Authorization", bearerToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].code").value("HQ"))
        .andExpect(jsonPath("$.data[0].children[0].code").value("ENG"));
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `mvn -Dtest=DepartmentApiIntegrationTests test`

Expected: failure because `GET /departments` and `children` do not exist.

- [ ] **Step 3: Implement the tree query**

Add `DepartmentMapper.findAllActiveOrEnabled()`, assemble roots and children in `DepartmentService.listTree()`, add `children` to `DepartmentVO`, and expose:

```java
@GetMapping
@PreAuthorize("hasAuthority('org:read')")
public ApiResponse<List<DepartmentVO>> listTree() {
    return ApiResponse.success(departmentService.listTree());
}
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `mvn -Dtest=DepartmentApiIntegrationTests test`

Expected: all department tests pass.

### Task 2: Position and Rank Resources

**Files:**
- Create: `backend/src/main/java/com/hrpm/entity/Position.java`
- Create: `backend/src/main/java/com/hrpm/entity/Rank.java`
- Create: `backend/src/main/java/com/hrpm/dto/CreatePositionDTO.java`
- Create: `backend/src/main/java/com/hrpm/dto/UpdatePositionDTO.java`
- Create: `backend/src/main/java/com/hrpm/dto/CreateRankDTO.java`
- Create: `backend/src/main/java/com/hrpm/dto/UpdateRankDTO.java`
- Create: `backend/src/main/java/com/hrpm/vo/PositionVO.java`
- Create: `backend/src/main/java/com/hrpm/vo/RankVO.java`
- Create: `backend/src/main/java/com/hrpm/mapper/PositionMapper.java`
- Create: `backend/src/main/java/com/hrpm/mapper/RankMapper.java`
- Create: `backend/src/main/java/com/hrpm/service/PositionService.java`
- Create: `backend/src/main/java/com/hrpm/service/RankService.java`
- Create: `backend/src/main/java/com/hrpm/controller/PositionController.java`
- Create: `backend/src/main/java/com/hrpm/controller/RankController.java`
- Create: `backend/src/test/java/com/hrpm/controller/PositionRankApiIntegrationTests.java`

- [ ] **Step 1: Write failing CRUD tests**

Cover list, create, update, duplicate code, stale version, `org:read`, and `org:manage`. The update request contract is:

```json
{"name":"Senior Engineer","jobFamily":"Engineering","description":"IC role","sortNo":10,"status":"ACTIVE","version":"0"}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `mvn -Dtest=PositionRankApiIntegrationTests test`

Expected: compilation or `404` failure because the resources are absent.

- [ ] **Step 3: Implement entities, DTOs, VOs, and mappers**

Use string IDs and versions at the HTTP boundary. Mapper updates must enforce optimistic locking:

```sql
UPDATE hr_position
SET name=#{name}, job_family=#{jobFamily}, description=#{description},
    sort_no=#{sortNo}, status=#{status}, version=version+1
WHERE id=#{id} AND version=#{version} AND deleted=0
```

- [ ] **Step 4: Implement services and controllers**

Expose `GET/POST /positions`, `PATCH /positions/{id}`, and equivalent rank operations. List requires `org:read`; writes require `org:manage`. Convert duplicate codes to a stable conflict exception and zero-row updates to `VERSION_CONFLICT`.

- [ ] **Step 5: Run tests and verify GREEN**

Run: `mvn -Dtest=PositionRankApiIntegrationTests test`

Expected: all position/rank tests pass.

### Task 3: Employee Directory and Detail Queries

**Files:**
- Create: `backend/src/main/java/com/hrpm/entity/Employee.java`
- Create: `backend/src/main/java/com/hrpm/entity/EmployeeListRow.java`
- Create: `backend/src/main/java/com/hrpm/vo/EmployeeVO.java`
- Create: `backend/src/main/java/com/hrpm/vo/EmployeeListVO.java`
- Create: `backend/src/main/java/com/hrpm/vo/PageVO.java`
- Create: `backend/src/main/java/com/hrpm/mapper/EmployeeMapper.java`
- Create: `backend/src/main/java/com/hrpm/service/EmployeeService.java`
- Create: `backend/src/main/java/com/hrpm/controller/EmployeeController.java`
- Create: `backend/src/test/java/com/hrpm/controller/EmployeeApiIntegrationTests.java`

- [ ] **Step 1: Write failing query tests**

Seed multiple departments and employees. Verify `GET /employees?page=1&pageSize=20&keyword=Alice&departmentId=...&employmentStatus=FORMAL`, detail lookup, pagination metadata, and `404` for an unknown ID.

```json
{"records":[],"total":0,"page":1,"pageSize":20}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `mvn -Dtest=EmployeeApiIntegrationTests test`

Expected: `404` because employee endpoints are absent.

- [ ] **Step 3: Implement mapper queries**

Join `hr_department`, `hr_position`, `hr_rank`, and manager `hr_employee`. Apply optional filters in a MyBatis `<script>` query, cap page size at `100`, and order by employee number. Include a `scopeDepartmentIds` parameter in mapper signatures; pass `null` in this iteration.

- [ ] **Step 4: Implement service and controller queries**

Expose `GET /employees` and `GET /employees/{id}` guarded by `org:read`. Reject page values below `1` and page sizes outside `1..100` with `VALIDATION_FAILED`.

- [ ] **Step 5: Run tests and verify GREEN**

Run: `mvn -Dtest=EmployeeApiIntegrationTests test`

Expected: all directory and detail tests pass.

### Task 4: Employee Creation and Ordinary Updates

**Files:**
- Create: `backend/src/main/java/com/hrpm/dto/CreateEmployeeDTO.java`
- Create: `backend/src/main/java/com/hrpm/dto/UpdateEmployeeDTO.java`
- Create: `backend/src/main/java/com/hrpm/common/exception/OrganizationReferenceInvalidException.java`
- Create: `backend/src/main/java/com/hrpm/common/exception/DuplicateResourceException.java`
- Create: `backend/src/main/java/com/hrpm/common/exception/VersionConflictException.java`
- Modify: `backend/src/main/java/com/hrpm/common/ApiExceptionHandler.java`
- Modify: `backend/src/main/java/com/hrpm/mapper/EmployeeMapper.java`
- Modify: `backend/src/main/java/com/hrpm/service/EmployeeService.java`
- Modify: `backend/src/main/java/com/hrpm/controller/EmployeeController.java`
- Test: `backend/src/test/java/com/hrpm/controller/EmployeeApiIntegrationTests.java`

- [ ] **Step 1: Write failing command tests**

Cover successful creation, duplicate employee number, inactive/missing department or position, invalid manager, self-manager, successful update, stale version, and rejection of `employmentStatus` in the update body.

```json
{"employeeNo":"E2026001","name":"Alice","gender":"FEMALE","departmentId":"1","positionId":"2","rankId":"3","managerEmployeeId":"4","employmentStatus":"PROBATION","hireDate":"2026-07-12"}
```

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -Dtest=EmployeeApiIntegrationTests test`

Expected: create/update tests fail because command endpoints are absent.

- [ ] **Step 3: Implement validation and persistence**

Validate every referenced record as `ACTIVE`, validate the manager as an active employee, insert with a generated ID, and update with `WHERE id=? AND version=?`. `UpdateEmployeeDTO` deliberately omits `employmentStatus`.

- [ ] **Step 4: Map stable API errors**

Map duplicate identifiers to `409 IDEMPOTENCY_CONFLICT`, stale versions to `409 VERSION_CONFLICT`, invalid references to `400 VALIDATION_FAILED`, and missing employee IDs to `404 RESOURCE_NOT_FOUND`.

- [ ] **Step 5: Run tests and verify GREEN**

Run: `mvn -Dtest=EmployeeApiIntegrationTests test`

Expected: all employee tests pass.

### Task 5: OpenAPI Contract

**Files:**
- Modify: `docs/hrpm-v1.openapi.yaml`

- [ ] **Step 1: Add concrete operations**

Replace generic organization placeholders with paths for department tree, position/rank list-create-update, and employee list-create-detail-update.

- [ ] **Step 2: Add request and response schemas**

Define `DepartmentNode`, `Position`, `Rank`, `EmployeeListItem`, `EmployeeDetail`, `CreateEmployeeRequest`, `UpdateEmployeeRequest`, and paginated response schemas. IDs and versions remain strings.

- [ ] **Step 3: Validate contract consistency**

Run: `rg -n "(/departments|/positions|/ranks|/employees|EmployeeDetail|CreateEmployeeRequest)" docs/hrpm-v1.openapi.yaml`

Expected: each path and schema is present exactly once.

### Task 6: Typed Frontend API Client

**Files:**
- Create: `frontend/src/api/organization.ts`
- Create: `frontend/src/types/organization.ts`

- [ ] **Step 1: Define API types**

Create types matching the OpenAPI schemas, including `DepartmentNode`, `Position`, `Rank`, `EmployeeListItem`, `EmployeeDetail`, `EmployeePage`, and create/update payloads.

- [ ] **Step 2: Implement API functions**

```ts
export const listEmployees = (params: EmployeeQuery) =>
  http.get<ApiResponse<EmployeePage>>('/employees', { params }).then(({ data }) => data.data)
```

Add equivalent functions for department tree, positions, ranks, employee detail, create, and update.

- [ ] **Step 3: Verify types**

Run: `npm run typecheck`

Expected: type checking passes before page integration.

### Task 7: Organization and Employee Management Page

**Files:**
- Create: `frontend/src/views/OrganizationEmployeesView.vue`
- Create: `frontend/src/components/organization/DepartmentTreePanel.vue`
- Create: `frontend/src/components/organization/EmployeeEditorDrawer.vue`
- Create: `frontend/src/components/organization/PositionRankDialog.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Route organization pages to the real view**

Map both `/org/departments` and `/org/employees` to `OrganizationEmployeesView` while retaining route-specific initial focus.

- [ ] **Step 2: Build the directory workflow**

Use a fixed-width department tree beside a flexible employee table. Add keyword/status filters, pagination, loading/empty/error states, and table actions for view/edit.

- [ ] **Step 3: Build create/edit drawer**

Use Element Plus form controls and validation. Show employment status during edit as read-only. On `VERSION_CONFLICT`, keep the drawer open and offer refresh rather than overwriting server data.

- [ ] **Step 4: Build position/rank maintenance dialog**

Provide list, create, and edit tabs/dialogs using icon buttons with tooltips. Refresh employee form option lists after successful maintenance.

- [ ] **Step 5: Verify responsive layout and types**

Run: `npm run typecheck`

Expected: no Vue or TypeScript errors.

### Task 8: Full Verification

**Files:**
- Modify only files required by failures discovered in this task.

- [ ] **Step 1: Run the backend suite**

Run: `mvn test`

Expected: all backend tests pass with zero failures and errors.

- [ ] **Step 2: Build the frontend**

Run: `npm run build`

Expected: `vue-tsc` and Vite complete successfully.

- [ ] **Step 3: Run repository hygiene checks**

Run: `git diff --check`

Expected: no whitespace errors.

- [ ] **Step 4: Review scope**

Confirm no API permits ordinary employee updates to change employment status, no sensitive plaintext fields were added, and no applied Flyway migration was modified.
