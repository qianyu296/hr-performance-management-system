# Generic Workflow Engine Plan

## Goal

Replace the current leave-specific, single-node approval code with a reusable linear workflow engine. The first migrated business handler remains leave; the design must permit personnel changes, overtime, and performance appeals to join without direct cross-domain mapper calls.

## Invariants

1. Template selection and workflow creation occur in the same transaction as the business submission state change.
2. Every submitted instance contains immutable template, node, applicant organization, manager-chain, and resolved-assignee snapshots.
3. Only the current assignee may act on a `PENDING` task. Conditional updates enforce task version and state.
4. A non-final approval creates exactly one next task; a final approval invokes one business handler in the same transaction.
5. Rejection, withdrawal, return, and transfer append action logs. No historical task or action record is overwritten.
6. A business-handler failure rolls back the task action and preserves the prior pending task.

## Delivery Slices

### 1. Workflow Model and Template Administration

- Add DTOs, service, controller, and frontend page for templates, scopes, nodes, priorities, versions, and activation.
- Select a template by business type, enabled state, most-specific organization scope, priority, then version. Ambiguity is rejected.
- Validate contiguous node numbers, supported node types, and valid approver rules before activation.

### 2. Snapshot and Assignee Resolution

- Introduce a `WorkflowApproverResolver` for `DIRECT_MANAGER`, `DEPARTMENT_LEADER`, `HR`, and `SPECIFIC_USER` rules.
- Resolve, deduplicate, and snapshot assignees during submission; apply the documented self-approval policy.
- Move JSON construction into typed snapshot records serialized by Jackson.

### 3. Generic Task Progression

- Refactor `WorkflowTaskService` to load task/instance snapshots, conditionally complete the current task, and create the next node task.
- Add `approve`, `reject`, `withdraw`, `return`, and `transfer` actions with mandatory comments where required.
- Add task history and instance detail APIs, including `availableActions` for frontend rendering.

### 4. Business Handler Boundary

- Define `WorkflowBusinessHandler` with final-approval, rejection, withdrawal, and return callbacks.
- Convert leave approval into the first handler. Balance updates, request/instance updates, action logs, and outbox records must remain atomic.
- Keep personnel, overtime, and appeals as later handlers; they must not call workflow mappers directly.

### 5. Frontend and Verification

- Add workflow template management, task inbox, action dialog, instance timeline, and conflict/error states.
- Cover template selection, multi-node progression, assignee isolation, organization-change snapshot stability, transfer/withdraw/return, and exactly-once leave balance changes.
- Update OpenAPI and run backend integration tests, frontend build, and Playwright approval E2E flow.

## Implementation Order

1. Write failing tests for scope precedence, multi-node progression, and task ownership.
2. Implement template administration and selector.
3. Implement snapshots and resolvers.
4. Refactor generic task state transitions.
5. Migrate leave to the handler boundary and prove balance invariants.
6. Deliver frontend workflow screens and E2E coverage.
