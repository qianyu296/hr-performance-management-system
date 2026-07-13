# Organization and Employee Management Design

## Scope

This iteration turns the organization and personnel placeholder into a usable master-data workflow. It covers department tree queries, position and rank maintenance, employee directory queries, employee creation, employee details, and ordinary profile updates. Personnel status transitions, onboarding approval, transfers, promotions, and exits remain controlled by the later personnel-change workflow.

## Backend Design

The backend follows the repository's layered package structure. `PositionController`, `RankController`, and `EmployeeController` expose `/api/v1/positions`, `/api/v1/ranks`, and `/api/v1/employees`; `DepartmentController` gains a tree query. Controllers validate HTTP input and delegate to services. Services enforce business rules and transaction boundaries. MyBatis mappers own database access, while DTOs and VOs keep persistence entities out of the API contract.

Position and rank resources support list, create, and version-checked update operations. Employee resources support paginated filtering by keyword, department, position, and employment status; creation; detail retrieval; and version-checked updates of ordinary profile and assignment fields. Employee updates cannot change `employmentStatus`; that field is accepted only when creating the initial record and later changes must use personnel-change actions.

## Validation and Data Rules

Employee creation requires a unique `employeeNo`, an active department, an active position, and an optional active rank. An optional manager must reference an existing active employee and cannot reference the employee itself. Department, position, rank, and manager references are revalidated during updates. Duplicate codes or employee numbers return a conflict response; missing or inactive references return a validation error; stale versions return `VERSION_CONFLICT`.

The first implementation uses existing Spring Security authentication and permission identifiers for function access. Mapper methods accept a query-scope parameter so the next iteration can add role-derived data scopes without rewriting controller contracts. Sensitive identity, phone, and banking fields are excluded from this iteration; field masking and full-value grants will be implemented with the dedicated authorization work.

## Frontend Design

The organization-personnel route becomes a work-focused management screen. A department tree filters a central employee table. The toolbar provides keyword and status filters plus employee creation. Employee details and editing use an Element Plus drawer so users retain directory context. Position and rank maintenance are available from compact dialogs or tabs on the same page.

The UI reads and writes only through typed API clients. It displays loading, empty, validation, unauthorized, version-conflict, and network-error states. Employment status is visible but not editable in the ordinary edit form.

## API and Testing

`docs/hrpm-v1.openapi.yaml` will define concrete schemas and operations for departments, positions, ranks, and employees, including string IDs and versions. Backend development follows test-first cycles covering authentication, happy paths, duplicate identifiers, invalid organization references, forbidden status changes, filtering, and optimistic-lock conflicts. Completion requires the full Maven test suite plus `npm run typecheck` and `npm run build`.

## Out of Scope

This iteration does not implement account provisioning, role assignment, custom department scopes, sensitive-field encryption, personnel-change approval, employee history, contracts, or exit handovers. Those features build on the stable employee master data delivered here.
