# Leave Type Management Design

## Scope

This iteration completes HR-managed leave types. It covers creating, listing, editing, and disabling a leave type; configuring whether it consumes a balance, the annual entitlement, and the minimum request unit; and a dedicated management screen. Leave-type organization scopes, attachment rules, and automatic yearly rollover remain out of scope.

## Rules

A leave type has a unique immutable code, a name, `deductBalance`, `annualQuota`, `minUnitHours`, status, and optimistic-lock version. `minUnitHours` must be greater than zero. A balance-consuming type requires an `annualQuota` greater than zero; a non-balance type stores no annual quota. Existing leave requests retain their original type reference and remain readable after the type is disabled.

Creating an active balance-consuming type initializes an `att_leave_balance` record for every active employee in the current calendar year, using the type code as `balanceType` and the annual quota as `availableHours`. Existing balances are never overwritten. Changing a type's annual quota does not update issued employee balances; HR uses the existing balance-adjustment workflow so every difference has an immutable adjustment record. Disabling a type prevents new leave requests.

## Backend and API

`LeaveTypeService` owns validation, transaction boundaries, balance initialization, and version checks. `LeaveTypeMapper` owns type persistence; `EmployeeMapper` supplies active employee IDs; `LeaveBalanceMapper` inserts missing annual balances idempotently. The current `GET /leave-types` remains the employee-safe active list. HR receives the complete list through `GET /leave-types?includeInactive=true`; that query and the following mutation operations require `attendance:manage`:

- `POST /leave-types`
- `PATCH /leave-types/{id}`
- `POST /leave-types/{id}/disable`

Create and update inputs contain `code` only for create, `name`, `deductBalance`, `annualQuota`, `minUnitHours`, and a version where applicable. Disable also requires the current version. Duplicate codes return a conflict response; malformed IDs, invalid quota/unit combinations, and stale versions return the repository's existing validation or version-conflict responses.

## Frontend

Add `/attendance/leave-types` under the attendance group, visible only with `attendance:manage`. The screen shows code, name, balance behavior, annual quota, minimum unit, status, and actions. A single Element Plus dialog supports create and edit; annual quota is disabled and cleared when balance deduction is off. Disabling requires a confirmation dialog. The existing leave request view continues to call the active-only API and shows no management-only fields.

## Verification

Integration tests cover HR authorization, creation with annual-balance initialization, duplicate-code rejection, invalid quota and unit rejection, version conflicts, edit behavior that leaves existing balances unchanged, and disable behavior that blocks a subsequent leave submission. Frontend validation mirrors server rules; `npm run typecheck` and `npm run build` verify the page. The OpenAPI contract documents each new operation and preserves the active-list behavior.
