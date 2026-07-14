# Leave Type Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver HR-controlled leave-type creation, editing, disabling, annual entitlement initialization, and a Vue management page without changing historical balances.

**Architecture:** `LeaveTypeService` becomes the transaction boundary for type lifecycle and initial balance creation. Existing employee leave selection stays active-only, while the HR route receives complete records behind `attendance:manage`. The Vue page uses the existing typed Axios client, Element Plus dialogs, and explicit reloads after mutations.

**Tech Stack:** Spring Boot 3.4, Spring Security method authorization, MyBatis, H2-backed `MockMvc` integration tests, Vue 3, TypeScript, Element Plus, Vite.

---

### Task 1: Specify the HR leave-type API contract

**Files:**
- Modify: `docs/hrpm-v1.openapi.yaml`
- Test: `backend/src/test/java/com/hrpm/OpenApiContractTests.java`

- [ ] **Step 1: Add a failing contract assertion for the explicit leave-type paths.**

Add an assertion that the parsed OpenAPI document contains `/leave-types` and `/leave-types/{id}/disable`; run `mvn -Dtest=OpenApiContractTests test` and confirm it fails because those paths are absent.

- [ ] **Step 2: Define the operations and schemas.**

Add `GET /leave-types` with optional `includeInactive`, `POST /leave-types`, `PATCH /leave-types/{id}`, and `POST /leave-types/{id}/disable`. Define `LeaveType`, `CreateLeaveTypeRequest`, `UpdateLeaveTypeRequest`, and `DisableLeaveTypeRequest`; use string int64 IDs, `annualQuota` nullable, `minUnitHours` positive, and `version` on mutation commands.

- [ ] **Step 3: Verify the contract test passes.**

Run `mvn -Dtest=OpenApiContractTests test`; expect `BUILD SUCCESS`.

### Task 2: Add failing API tests for lifecycle and authorization

**Files:**
- Create: `backend/src/test/java/com/hrpm/controller/LeaveTypeApiIntegrationTests.java`
- Modify: `backend/src/test/java/com/hrpm/controller/LeaveRequestApiIntegrationTests.java`

- [ ] **Step 1: Write the HR lifecycle tests before backend production changes.**

Seed one active employee, an HR user with `attendance:manage`, and a regular user with `attendance:submit`. Add these `MockMvc` cases:

```java
@Test
void hrCanCreateBalanceDeductingTypeAndInitializesCurrentYearBalance() throws Exception {
    mockMvc.perform(post("/leave-types").header("Authorization", bearer(hrUserId))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"code":"ANNUAL","name":"Annual Leave","deductBalance":true,"annualQuota":80,"minUnitHours":1}"""))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.code").value("ANNUAL"));
    assertThat(countBalances("ANNUAL", Year.now().getValue())).isEqualTo(1);
}
```

Also cover regular-user `403`, duplicate code `409`, zero quota/unit `400`, a stale update `409`, update retaining the original balance amount, and disable causing a later leave submission to fail.

- [ ] **Step 2: Run the focused test to verify the expected red state.**

Run `mvn -Dtest=LeaveTypeApiIntegrationTests test`; expect test failures because mutation endpoints and `LeaveTypeService` do not exist.

### Task 3: Implement the leave-type lifecycle service and persistence

**Files:**
- Create: `backend/src/main/java/com/hrpm/dto/CreateLeaveTypeDTO.java`
- Create: `backend/src/main/java/com/hrpm/dto/UpdateLeaveTypeDTO.java`
- Create: `backend/src/main/java/com/hrpm/dto/DisableLeaveTypeDTO.java`
- Create: `backend/src/main/java/com/hrpm/service/LeaveTypeService.java`
- Modify: `backend/src/main/java/com/hrpm/entity/LeaveType.java`
- Modify: `backend/src/main/java/com/hrpm/vo/LeaveTypeVO.java`
- Modify: `backend/src/main/java/com/hrpm/mapper/LeaveTypeMapper.java`
- Modify: `backend/src/main/java/com/hrpm/mapper/EmployeeMapper.java`
- Modify: `backend/src/main/java/com/hrpm/mapper/LeaveBalanceMapper.java`
- Modify: `backend/src/main/java/com/hrpm/controller/LeaveTypeController.java`

- [ ] **Step 1: Implement DTO and view contracts.**

Use Bean Validation to require a 1-64 character code, a nonblank 1-128 character name, a positive `minUnitHours`, and a version string on update/disable. Extend `LeaveType` and `LeaveTypeVO` with `annualQuota` and `version`; retain the simple active-list response behavior for employee callers.

- [ ] **Step 2: Implement mapper operations.**

Add methods to find by code, list all types, insert a type, version-check update name/deduction/quota/unit, and version-check status change. Add `EmployeeMapper.listActiveIds()` with `employment_status <> 'TERMINATED'`. Add `LeaveBalanceMapper.insertIfAbsent(id, employeeId, balanceType, balanceYear, availableHours, createdBy)` using `INSERT ... SELECT ... WHERE NOT EXISTS` so retries cannot duplicate an annual balance.

- [ ] **Step 3: Implement minimal service behavior.**

`create` normalizes code to uppercase, validates the quota/deduction combination, rejects an existing code, inserts `ACTIVE`, and initializes balances in one transaction only when `deductBalance` is true. `update` validates the same combination and updates only the type record. `disable` changes status to `INACTIVE`. Parse versions consistently with `WorkCalendarService`; return `VersionConflictException` when an update count is zero.

- [ ] **Step 4: Expose permission-checked controller endpoints.**

Keep active `GET /leave-types` available to authenticated requestors. When `includeInactive=true`, require `attendance:manage`; require that authority for `POST`, `PATCH /{id}`, and `POST /{id}/disable`. Delegate every mutation to `LeaveTypeService` and return `ApiResponse<LeaveTypeVO>`.

- [ ] **Step 5: Verify the focused lifecycle tests turn green.**

Run `mvn -Dtest=LeaveTypeApiIntegrationTests,LeaveRequestApiIntegrationTests test`; expect no failures, including the existing minimum-unit test.

### Task 4: Build the HR management page

**Files:**
- Create: `frontend/src/views/LeaveTypeManagementView.vue`
- Modify: `frontend/src/api/leave.ts`
- Modify: `frontend/src/router/navigation.ts`
- Modify: `frontend/src/router/index.ts`

- [ ] **Step 1: Extend the typed client.**

Add `LeaveType`, `LeaveTypePayload`, `fetchManagedLeaveTypes`, `createLeaveType`, `updateLeaveType`, and `disableLeaveType`. Keep `fetchLeaveTypes` typed as the active employee list and leave its request unchanged.

- [ ] **Step 2: Add the authenticated attendance-management route.**

Add `/attendance/leave-types` with the title `请假类型` and `attendance:manage`. Map it to `LeaveTypeManagementView`; do not route it to `DomainView`.

- [ ] **Step 3: Implement the page.**

Use `PageFrame`, a status filter, a refresh icon button, a primary create action, and an Element Plus table. The dialog contains code, name, balance switch, annual quota, and minimum hours; disable the code in edit mode and disable/clear annual quota when `deductBalance` is false. Submit create or update based on record ID, then reload. Use `ElMessageBox.confirm` before disabling and surface request failures with `ElMessage`.

- [ ] **Step 4: Verify frontend compilation.**

Run `npm run typecheck` and `npm run build` from `frontend`; expect both commands to exit with code 0.

### Task 5: Run full regression verification and document the delivery

**Files:**
- Modify: `docs/04-接口设计.md`

- [ ] **Step 1: Synchronize the API design summary.**

Update the leave-type row to name creation, update, disable, annual quota, and minimum request unit explicitly. Do not modify existing Flyway migrations: the schema already has the required columns.

- [ ] **Step 2: Run backend and frontend verification.**

Run `mvn test` from `backend` and `npm run typecheck; npm run build` from `frontend`. Record any Docker-only Testcontainers skip separately from failures.

- [ ] **Step 3: Commit the feature.**

```powershell
git add backend frontend docs
git commit -m "feat: manage leave types"
```
