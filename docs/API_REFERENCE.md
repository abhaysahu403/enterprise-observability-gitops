# API Reference Overview

Full interactive documentation lives in each service's Swagger UI
(`/swagger-ui.html`) once running — this file is a quick-reference index of
the routes exposed through the Gateway (prefix `/api`).

All responses are wrapped in a standard envelope:

```json
{ "success": true, "message": "...", "data": { ... }, "timestamp": "..." }
```

Paginated list endpoints wrap `data` in:

```json
{ "content": [...], "page": 0, "size": 20, "totalElements": 100, "totalPages": 5, "last": false }
```

## Authentication Service (`/api/auth`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/login` | public | Returns access + refresh JWT |
| POST | `/register` | public | Create a new account |
| POST | `/refresh` | public (valid refresh token) | Rotate tokens |
| POST | `/logout` | authenticated | Blacklists current tokens in Redis |
| GET | `/roles` | authenticated | List available roles |
| GET/PUT/DELETE | `/users`, `/users/{id}` | ADMIN | User management |

## Employee Service (`/api/employees`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/` | ADMIN, HR | Create employee |
| GET | `/{id}` | authenticated | Get by id (Redis-cached) |
| GET | `/` | authenticated | Search + filter + paginate (`search`, `department`, `status`, `managerId`) |
| GET | `/department/{department}` | authenticated | List by department |
| GET | `/manager/{managerId}` | authenticated | Direct reports |
| GET | `/reports/department-summary` | authenticated | Headcount per department (cached) |
| GET | `/reports/active-count` | authenticated | Active employee count (cached) |
| PUT | `/{id}` | ADMIN, HR | Update |
| DELETE | `/{id}` | ADMIN | Delete |

## Leave Service (`/api/leaves`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/apply` | authenticated | Apply for leave (checks balance) |
| PUT | `/{id}/approve` | ADMIN, MANAGER, HR | Approve pending request |
| PUT | `/{id}/reject` | ADMIN, MANAGER, HR | Reject pending request |
| PUT | `/{id}/cancel?employeeId=` | owner only | Cancel own request |
| GET | `/balance?employeeId=&year=` | authenticated | Balance by leave type |
| GET | `/history` | authenticated | Filter by `employeeId`/`status`, paginated |

## Payroll Service (`/api/payroll`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/generate` | ADMIN, HR | Manually trigger a payroll run (also runs automatically monthly) |
| GET | `/payslip?employeeId=&month=&year=` | authenticated | Single payslip |
| GET | `/history?employeeId=` | authenticated | Salary history, paginated |
| GET | `/status?month=&year=` | ADMIN, HR | Payroll status for a period |
| PUT | `/{id}/mark-paid` | ADMIN, HR | Mark a record as paid |

## Asset Service (`/api/assets`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/` | ADMIN, HR | Register a new asset |
| GET | `/{id}` | authenticated | Get by id |
| GET | `/` | authenticated | List, filter by `status`/`type`, paginated |
| GET | `/employee/{employeeId}` | authenticated | Assets held by an employee |
| PUT | `/{id}/assign` | ADMIN, HR | Assign to employee |
| PUT | `/{id}/return` | ADMIN, HR | Mark returned |
| PUT | `/{id}/status?status=` | ADMIN, HR | Change status (repair/retired) |
| GET | `/reports/count-by-status?status=` | authenticated | Count by status |

## Help Desk Service (`/api/tickets`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/` | authenticated | Raise a ticket (SLA due date auto-computed by priority) |
| GET | `/{id}` | authenticated | Get with comments |
| GET | `/` | authenticated | Filter by `status`/`assignedTo`/`raisedBy`, paginated |
| PUT | `/{id}/assign` | ADMIN, HR, MANAGER | Assign to an agent |
| PUT | `/{id}/status?status=` | authenticated | Update status |
| PUT | `/{id}/resolve` | authenticated | Resolve with a resolution note |
| PUT | `/{id}/reopen` | authenticated | Reopen resolved/closed |
| POST | `/{id}/comments` | authenticated | Add a comment |
| GET | `/reports/sla-breached` | ADMIN, HR, MANAGER | Open tickets past SLA |
| GET | `/reports/status-summary` | authenticated | Count per status |

## Notification Service (`/api/notifications`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/send` | authenticated | Send (mock) via EMAIL/TEAMS/SMS |
| GET | `/{id}` | authenticated | Get status |
| PUT | `/{id}/retry` | authenticated | Manual retry |
| GET | `/history` | authenticated | Filter by `employeeId`/`status`, paginated |
| GET | `/reports/status-summary` | authenticated | Count per delivery status |
