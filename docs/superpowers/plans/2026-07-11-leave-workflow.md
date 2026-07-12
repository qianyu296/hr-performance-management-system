# Leave Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver transactional leave submission and workflow approval with exactly-once balance changes.

**Architecture:** Leave validation and persistence remain in `attendance`. A focused `workflow` application service selects a template, snapshots it into an instance, creates tasks, and invokes leave completion only after final approval.

**Tech Stack:** Java 17, Spring Boot 3.4, MyBatis annotations, MySQL/Flyway, JUnit 5, MockMvc.

---

### Task 1: Specify Submission With Failing Tests

**Files:**
- Modify: `backend/src/test/java/com/hrpm/attendance/LeaveRequestApiIntegrationTests.java`

- [ ] **Step 1: Add the success test.** Seed an active `LEAVE` template scoped to the requester department, with first-node JSON rule `{"userId":90002}`. Create a draft and call:

```java
mockMvc.perform(post("/leave-requests/{id}/submit", requestId)
        .header("Authorization", bearerToken())
        .header("Idempotency-Key", "leave-submit-success-0001")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"version\":\"0\"}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM wf_instance", Integer.class));
```

- [ ] **Step 2: Run and verify RED.**

```powershell
$env:DB_URL='jdbc:mysql://127.0.0.1:3306/hrpm?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC'
$env:DB_USERNAME='hrpm'; $env:DB_PASSWORD='hrpm_dev_password'; $env:JWT_SIGNING_KEY='test-signing-key-at-least-32-characters'
& 'D:\apache-maven-3.9.9\bin\mvn.cmd' '-Dmaven.repo.local=D:/apache-maven-3.9.9/repository' -o -Dtest=LeaveRequestApiIntegrationTests#ownerCanSubmitDraftAndCreateInitialWorkflowTask test
```

Expected: failure because no submit mapping exists.

- [ ] **Step 3: Add no-template and wrong-owner tests.** Expect 422 `WORKFLOW_TEMPLATE_MISSING` with no `wf_instance` row, and 404 for another employee. Both assert the request remains `DRAFT`.

- [ ] **Step 4: Run all leave-request tests and verify RED only for submission behavior.**

```powershell
& 'D:\apache-maven-3.9.9\bin\mvn.cmd' '-Dmaven.repo.local=D:/apache-maven-3.9.9/repository' -o -Dtest=LeaveRequestApiIntegrationTests test
```

- [ ] **Step 5: Commit.** Run `git add backend/src/test/java/com/hrpm/attendance/LeaveRequestApiIntegrationTests.java`, then `git commit -m "test: define leave submission workflow"`.

### Task 2: Implement Template Selection And Submission

**Files:**
- Create: `backend/src/main/java/com/hrpm/workflow/WorkflowTemplateSelector.java`
- Create: `backend/src/main/java/com/hrpm/workflow/WorkflowMapper.java`
- Create: `backend/src/main/java/com/hrpm/workflow/LeaveWorkflowService.java`
- Create: `backend/src/main/java/com/hrpm/attendance/SubmitLeaveRequestCommand.java`
- Modify: `backend/src/main/java/com/hrpm/attendance/LeaveRequestMapper.java`
- Modify: `backend/src/main/java/com/hrpm/attendance/LeaveApplicationService.java`
- Modify: `backend/src/main/java/com/hrpm/attendance/LeaveRequestController.java`
- Modify: `backend/src/main/java/com/hrpm/common/web/ApiExceptionHandler.java`

- [ ] **Step 1: Add request load and conditional update methods.** The request query returns employee, department, type, dates, status, workflow ID, and version. The update protects ownership, state, and version:

```java
@Update("""
    UPDATE att_leave_request
    SET status='IN_PROGRESS', workflow_instance_id=#{instanceId}, version=version+1
    WHERE id=#{id} AND employee_id=#{employeeId} AND status='DRAFT' AND version=#{version} AND deleted=0
    """)
int markSubmitted(long id, long employeeId, int version, long instanceId);
```

- [ ] **Step 2: Implement `selectLeaveTemplate(departmentId)`.** Filter active, non-deleted `LEAVE` templates. Rank scoped candidates before global candidates, then `priority DESC` and `template_version DESC`; reject no candidate and a top-ranked tie with `WorkflowTemplateMissingException`.

- [ ] **Step 3: Implement `LeaveWorkflowService.submit`.** In one `@Transactional` method, revalidate active employee/type, positive range, time overlap against active requests, and balance when the type deducts balance. Select a template, load its lowest node, parse its `userId`, insert `wf_instance`, insert `PENDING wf_task`, and invoke `markSubmitted`. A zero-row conditional update raises a state/version conflict and rolls back all inserts.

- [ ] **Step 4: Map the endpoint.** `POST /leave-requests/{id}/submit` requires `attendance:submit`, takes `SubmitLeaveRequestCommand(int version)`, and returns id, status, workflow instance ID, and version. Map ownership to 404, races to 409, and missing template to 422.

- [ ] **Step 5: Run Task 1 tests and verify GREEN.**

```powershell
& 'D:\apache-maven-3.9.9\bin\mvn.cmd' '-Dmaven.repo.local=D:/apache-maven-3.9.9/repository' -o -Dtest=LeaveRequestApiIntegrationTests test
```

- [ ] **Step 6: Commit.** Run `git add backend/src/main/java/com/hrpm/attendance backend/src/main/java/com/hrpm/workflow backend/src/main/java/com/hrpm/common/web/ApiExceptionHandler.java`, then `git commit -m "feat: submit leave requests to workflow"`.

### Task 3: Define And Implement Workflow Task Completion

**Files:**
- Create: `backend/src/main/java/com/hrpm/workflow/WorkflowTaskController.java`
- Create: `backend/src/main/java/com/hrpm/workflow/WorkflowTaskService.java`
- Create: `backend/src/main/java/com/hrpm/workflow/WorkflowActionCommand.java`
- Modify: `backend/src/main/java/com/hrpm/workflow/WorkflowMapper.java`
- Modify: `backend/src/main/java/com/hrpm/attendance/LeaveRequestMapper.java`
- Modify: `backend/src/test/java/com/hrpm/attendance/LeaveRequestApiIntegrationTests.java`

- [ ] **Step 1: Add a failing final-approval test.** Seed a submitted request and pending task. The assignee posts `{ "version":"0", "comment":"approved" }` to `/workflow/tasks/{id}/approve`; assert request and instance `APPROVED`, task `APPROVED`, balance `16 -> 8`, and exactly one `att_balance_change` with source type `LEAVE_APPROVAL`. Add wrong-assignee and repeat-approval assertions for `WORKFLOW_TASK_INVALID` and 409 with no extra ledger row.

- [ ] **Step 2: Run and verify RED.**

```powershell
& 'D:\apache-maven-3.9.9\bin\mvn.cmd' '-Dmaven.repo.local=D:/apache-maven-3.9.9/repository' -o -Dtest=LeaveRequestApiIntegrationTests#assigneeApprovalCompletesLeaveExactlyOnce test
```

- [ ] **Step 3: Implement conditional task completion.** Update only a `PENDING` task with matching ID, assignee, and version. Insert an action log. A non-final node creates the next task; a final node conditionally marks instance/request approved, performs optimistic balance update, and inserts the immutable balance change. Rejection logs the action and marks only task, instance, and request rejected.

- [ ] **Step 4: Run the leave-request suite and verify GREEN.**

```powershell
& 'D:\apache-maven-3.9.9\bin\mvn.cmd' '-Dmaven.repo.local=D:/apache-maven-3.9.9/repository' -o -Dtest=LeaveRequestApiIntegrationTests test
```

- [ ] **Step 5: Commit.** Run `git add backend/src/main/java/com/hrpm/workflow backend/src/main/java/com/hrpm/attendance backend/src/test/java/com/hrpm/attendance/LeaveRequestApiIntegrationTests.java`, then `git commit -m "feat: complete leave workflow tasks"`.

### Task 4: Add Approval Cancellation And Regressions

**Files:**
- Modify: `backend/src/main/java/com/hrpm/attendance/LeaveRequestController.java`
- Modify: `backend/src/main/java/com/hrpm/attendance/LeaveApplicationService.java`
- Modify: `backend/src/main/java/com/hrpm/attendance/LeaveRequestMapper.java`
- Modify: `backend/src/test/java/com/hrpm/attendance/LeaveRequestApiIntegrationTests.java`

- [ ] **Step 1: Add a failing cancellation test.** The owner calls `POST /leave-requests/{id}/cancel` with the current version for an approved request. Assert `CANCELLED`, balance restoration, one `LEAVE_CANCELLATION` row, and a repeated call returns 409 without another row.

- [ ] **Step 2: Run and verify RED.**

```powershell
& 'D:\apache-maven-3.9.9\bin\mvn.cmd' '-Dmaven.repo.local=D:/apache-maven-3.9.9/repository' -o -Dtest=LeaveRequestApiIntegrationTests#ownerCanCancelApprovedLeaveExactlyOnce test
```

- [ ] **Step 3: Implement `cancelApproved`.** In one transaction, conditionally mark the owner request cancelled, restore the versioned balance, and create a `LEAVE_CANCELLATION` balance change.

- [ ] **Step 4: Run focused and full regression tests.**

```powershell
& 'D:\apache-maven-3.9.9\bin\mvn.cmd' '-Dmaven.repo.local=D:/apache-maven-3.9.9/repository' -o -Dtest=LeaveRequestApiIntegrationTests test
& 'D:\apache-maven-3.9.9\bin\mvn.cmd' '-Dmaven.repo.local=D:/apache-maven-3.9.9/repository' -o test
```

Expected: all tests pass with zero failures.

- [ ] **Step 5: Commit.** Run `git add backend/src/main/java/com/hrpm/attendance backend/src/test/java/com/hrpm/attendance/LeaveRequestApiIntegrationTests.java`, then `git commit -m "feat: cancel approved leave requests"`.
