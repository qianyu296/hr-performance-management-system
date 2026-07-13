# Performance Configuration and Task Snapshot Design

## Scope

This iteration delivers the R1 performance configuration workflow: metrics, scheme versions, level rules, performance cycles, and task snapshot generation at cycle start. It excludes self-assessment, manager scoring, calibration, publishing, appeals, notifications, and reports.

## Configuration Rules

Metrics have a unique immutable code, name, metric type, optional unit, score method, score configuration, description, status, and optimistic-lock version. Schemes hold a code, name, applicability rule, and status. A scheme version owns ordered metric items, weights, level rules, and evaluation-stage configuration.

Only a draft version can be changed. Enabling a version requires at least one active metric, weights totaling exactly 100 percent, and level intervals that map every configured score boundary exactly once without overlap. Enabled versions are immutable. A cycle references an enabled scheme version, has start and end dates plus self and manager deadlines, and starts only once.

## Task Snapshot Generation

Starting a cycle runs in one transaction. The service selects active employees, resolves their current manager, copies the employee's organization details and the referenced scheme-version configuration into `perf_task` and `perf_task_item`, and creates one unique task per employee per cycle with status `PENDING_SELF_ASSESSMENT`. Existing tasks are never overwritten; a duplicate cycle start is rejected. Employees added or transferred after start are deliberately not included in this iteration.

## API and Authorization

All metric, scheme, scheme-version, and cycle configuration endpoints require `performance:config`. Mutation commands include the current resource version. The API exposes list, create, version creation/update while draft, version enable, cycle create, and cycle start actions. Invalid configuration returns validation errors, duplicate codes return conflicts, and stale versions return `VERSION_CONFLICT`.

## Frontend and Verification

`/performance/cycles` becomes a real management workspace with metric, scheme, and cycle views. Element Plus dialogs collect configuration, and starting a cycle uses a confirmation dialog. Backend integration tests cover authorization, configuration validation, immutable enabled versions, unique snapshot task generation, and stale writes. The frontend must pass `npm run typecheck` and `npm run build`; the backend must pass `mvn test`.
