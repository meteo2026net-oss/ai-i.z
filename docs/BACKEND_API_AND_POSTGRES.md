# AI I.Z Backend Runner — API Contract and Security Requirements

## 1. Current implementation status

The repository now contains an initial Node.js Backend Runner implementation under `backend/`. It verifies Firebase ID tokens and revocation, exposes authenticated health/schema endpoints, and provides restricted read-only PostgreSQL access. It is **not deployed** and is not yet production-ready. Remote test execution and approved database mutations deliberately return 503 until their isolated worker and server-side approval controls are implemented.


## 2.1 Current server implementation boundaries

Implemented in `backend/server.js`:
- Firebase Admin ID-token verification with revocation checking on every API request.
- Authenticated health endpoint with a real PostgreSQL connectivity check.
- Public-schema metadata for tables, columns, primary keys, indexes, foreign-key relations, estimated rows, and RLS state.
- A single-statement SELECT-only endpoint using a read-only transaction, statement/lock timeouts, a 200-row response cap, and the `isolated_dev` target only.
- Request body limits, rate limiting, security headers, and non-sensitive error responses.

Not yet enabled:
- `POST /api/tests/run` returns HTTP 503 until an isolated, resource-limited test worker is available.
- `POST /api/db/execute-approved` returns HTTP 503 until approvals are verified server-side and one-time execution/replay protection is implemented.

The SQL filter is defense in depth. The PostgreSQL account supplied through `DATABASE_URL_READONLY` must be a non-owner, non-superuser role with SELECT grants only on the intended isolated schema, no write grants, and no EXECUTE permission on unsafe application/extension functions. Never point it at a legacy or production database.

## 2. Required API

All endpoints require an authenticated caller. The server must validate the Firebase ID token (or an explicitly approved equivalent identity provider) and derive the user identity from the verified token. A static shared bearer token alone is not sufficient user authentication.

### Health

`GET /api/health`

Returns service version and dependency health without exposing secrets, connection strings, internal hostnames, or database credentials.

Example:
```json
{
  "status": "ok",
  "version": "1.0.0",
  "dotnetAvailable": false,
  "flutterAvailable": false,
  "postgresConnected": false,
  "lastCheckUtc": "2026-01-01T00:00:00Z"
}
```

Capability flags must reflect actual server-side checks, not hard-coded success values.

### Run tests

`POST /api/tests/run`

Accepts an allowlisted repository identifier, branch, and test target. The server must reject arbitrary shell commands, arbitrary URLs, unapproved repositories, and untrusted branch input. Run jobs in isolated, resource-limited workers with timeouts and no ambient production credentials.

### Database schema

`GET /api/db/schema`

Returns only the schema metadata the authenticated user is authorized to inspect. Never return secrets, connection strings, credential values, or unrelated tenant data.

### Read-only SQL

`POST /api/db/query-readonly`

Use a dedicated database role with read-only transaction permissions. Enforce query timeouts, row limits, statement-size limits, and tenant scoping. Reject multi-statement input and any operation that can mutate data or invoke privileged functions.

### Approved database operation

`POST /api/db/execute-approved`

A client-provided approval identifier is not proof of approval. The server must retrieve and verify the approval record, bind it to the authenticated user, exact target, exact operation hash, expiry, and policy tier, then revalidate the proposed operation immediately before execution. Log the decision and result without logging credentials or sensitive row contents.

## 3. Database isolation requirements

- Use a dedicated AI I.Z development database or isolated schema. Do not assume or reuse legacy METRA databases.
- Keep read-only and migration/execution credentials separate.
- Store credentials only in a server-side secret manager or environment configuration.
- Never ship database credentials or service-role keys in the Android APK.
- Require backups and a tested restore procedure before production-impacting migrations.
- Use least-privilege grants and tenant-aware row-level security where applicable.
- Use parameterized queries; do not concatenate user or model-generated values into SQL.
- Restrict outbound network access from execution workers.

## 4. Approval and audit model

Every high-impact operation must have a server-side approval record containing at least:
- authenticated user and tenant;
- operation type and exact target;
- immutable operation/payload hash;
- human-readable preview and impact summary;
- approval status, approver, timestamp, and expiry;
- execution status and resulting audit reference.

The server must reject expired, reused, altered, mismatched, or revoked approvals. Destructive operations and production deployment require an explicit approval step and an auditable rollback plan.

## 5. Minimum acceptance tests before deployment

1. Unauthenticated requests are rejected.
2. Invalid, expired, or wrong-audience identity tokens are rejected.
3. A user cannot access another user's project, logs, approvals, or database metadata.
4. The test runner cannot execute arbitrary shell commands or access unapproved repositories.
5. Read-only SQL rejects INSERT, UPDATE, DELETE, DDL, multi-statement payloads, and privileged functions.
6. Approved execution rejects missing, expired, altered, or mismatched approval records.
7. Timeouts, output limits, and worker isolation are enforced.
8. Secrets and sensitive data are absent from logs and API error messages.
9. Health responses accurately report dependency availability.
10. Backup restoration and migration rollback are tested.

## 6. Deployment gate

Do not enable remote execution or database operations in a production app until the server exists, its security tests pass, HTTPS and identity verification are configured, and the client/server contract is tested end to end. A successful Android build does not satisfy this gate.
