# Leave Workflow Design

## Scope

Implement the first complete leave-request workflow: a draft owner submits a request, the system selects an enabled leave template, creates a workflow instance and first pending task, and an assignee approves or rejects it. Final approval deducts leave balance exactly once; cancellation of an approved request restores it exactly once.

## API And State

`POST /leave-requests/{id}/submit` is available only to the draft owner with `attendance:submit`. It accepts the current request version and requires an `Idempotency-Key`. A valid submission changes `DRAFT` to `IN_PROGRESS`, assigns `workflowInstanceId`, and returns the new status and available actions.

`POST /workflow/tasks/{id}/approve` and `/reject` require the assigned user and the task version. Approving a non-final node creates the next pending task. Approving the final node changes the request to `APPROVED` and writes one negative `att_balance_change`; rejecting ends the instance and changes the request to `REJECTED`.

## Template And Transaction Rules

The selector filters enabled `LEAVE` templates and matches the requester's department snapshot. It prefers a scoped template over a global template, then higher priority and template version. No match raises `WORKFLOW_TEMPLATE_MISSING`; an ambiguous top-ranked match is rejected.

Submitting, task completion, leave-status changes, balance updates, and balance-change inserts run in one transaction. Conditional updates based on status and version prevent duplicate submissions and duplicate approvals. The unique balance-source key remains the final backstop for duplicate deductions.

## Validation And Errors

Submission rechecks active employee/type, positive time range, overlap against active requests, balance sufficiency when the type deducts balance, ownership, and the current `DRAFT` state. State or version races return `STATE_CONFLICT` or `VERSION_CONFLICT`; absent templates return `WORKFLOW_TEMPLATE_MISSING`; invalid task ownership or state returns `WORKFLOW_TASK_INVALID`.

## Tests

Integration tests cover successful submit and approval, missing-template rejection with no partial rows, request ownership, overlap and insufficient balance rejection, wrong-assignee rejection, duplicate approval, and final approval/cancellation balance-ledger consistency. Tests run against the local Flyway-migrated MySQL configuration already used by the backend suite.
