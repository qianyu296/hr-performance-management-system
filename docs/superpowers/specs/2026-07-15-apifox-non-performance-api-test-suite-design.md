# Apifox Non-Performance API Test Suite Design

## Goal

Provide a one-click importable and runnable Apifox suite for every currently exposed non-performance backend endpoint. The suite must use dedicated local development accounts, create its own `APIFOX_` data, and never call a performance endpoint.

## Scope

The suite covers 69 endpoint mappings under `/api/v1`:

| Area | Endpoints |
| --- | ---: |
| Health, authentication, and current user | 8 |
| Departments, positions, ranks, and employees | 16 |
| Personnel changes and exit handover | 9 |
| Leave, overtime, work calendars, and monthly summaries | 21 |
| Workflow tasks and templates | 11 |
| System access and headcount report | 4 |
| Total | 69 |

Excluded without exception: `/performance/**` and `GET /reports/performance-level-distribution`.

## Deliverables

- `tools/apifox/setup-local-api-test-accounts.sql`: idempotently creates dedicated users, roles, data scopes, and baseline organization/leave-balance data in local MySQL.
- `tools/apifox/hrpm-non-performance-api-tests.postman_collection.json`: Postman Collection v2.1 JSON for importing and debugging ordinary interfaces in Apifox.
- `tools/apifox/hrpm-local-dev.postman_environment.json`: local environment variables, credentials, tokens, and generated-object variables.
- `tools/apifox/generate-native-scenario.mjs`: converts the canonical Collection requests and scripts into Apifox native `customHttp` scenario steps.
- `tools/apifox/import-native-scenario.ps1`: creates and updates the runnable Apifox scenario through the Apifox CLI after the user has authenticated it.
- `tools/apifox/README.md`: import, initialization, execution, and safety instructions.

## Execution Model

The Collection is the canonical request definition. It first logs in all dedicated accounts and stores access and refresh tokens. Requests are sequenced by folder so creation responses save IDs and optimistic-lock versions for later operations. The native-scenario generator preserves these requests, headers, bodies, pre-request scripts, and test scripts, then adds an initial run-stamp script.

Apifox treats an imported Postman Collection as an interface collection, not as an automation scenario. Therefore, the PowerShell importer first creates scenario metadata with the Apifox CLI, then updates it with one initialization script and 90 independent `customHttp` steps. This avoids any dependency on interface IDs already present in a specific Apifox project. The user selects the imported `HRPM Local Development` environment when manually running the created scenario.

The standard response assertion requires HTTP 200 and `code: SUCCESS`. Negative authorization and validation requests explicitly assert their expected non-200 status and business error code. Workflow tests create scoped templates with the dedicated manager as the specific approver before submitting leave, overtime, and personnel-change records.

## Test Accounts

All dedicated accounts use password `admin123` and are reset by the initialization script.

| User | Purpose | Data scope |
| --- | --- | --- |
| `api-admin` | non-performance administrative happy paths | ALL |
| `api-hr` | organization and personnel-change paths | ALL |
| `api-manager` | workflow approval and department-tree read scope | DEPT_TREE |
| `api-employee` | self-service leave, overtime, and self-scope checks | SELF |
| `api-attendance-admin` | leave type, balance, calendar, and summary administration | ALL |
| `api-workflow-admin` | template maintenance and task transfer authorization | none required |
| `api-readonly` | authenticated 403 assertions | none |

## Safety and Limitations

The SQL script is restricted to the dedicated `990000x` test identity range and data whose codes begin with `APIFOX_`. It does not delete or modify ordinary business data. The collection avoids changing existing users and avoids destructive endpoint calls against non-test records.

The created native scenario can run every request in order. A rerun generates new code values and can reuse the fixed test accounts. The suite leaves created business data available for investigation; cleanup is intentionally not automatic because the API does not expose delete endpoints for every resource type.
