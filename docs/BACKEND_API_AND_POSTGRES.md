# AI I.Z Backend Runner — API Contract and Security Requirements

## 1. Current implementation status

The separate AI I.Z identity API in `backend/` issues app-owned Google OIDC sessions and PostgreSQL-backed user records. This is not the Backend Runner implementation. The legacy Android Backend Runner client still uses its separate token setting and is not yet integrated with the AI I.Z session API.

The Android application contains a Retrofit client contract for Backend Runner. This repository does **not** currently contain a deployable Backend Runner server implementation. The endpoints below describe the required server contract; they must not be treated as live services until a server is implemented, deployed over HTTPS, and tested end to end.

## 2. Required API

All endpoints require an authenticated caller. The server must validate the short-lived AI I.Z access token issued by `backend/` (signature, algorithm, issuer, audience, expiry, subject, and session ID), then confirm that the session remains active and unrevoked in PostgreSQL. It must derive the user identity from the verified token and session record. A Firebase ID token or static shared bearer token is not accepted as a substitute.

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

A client-provided approval identifier is not proof of approval. The server must retrieve and verify the approval record from a trusted server-side store, bind it to the authenticated AI I.Z user, exact target, exact operation hash, expiry, and policy tier, then revalidate the proposed operation immediately before execution. A client-supplied approval ID or client-side `ApprovalGuard` result is never sufficient. Log the decision and result without logging credentials or sensitive row contents.

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
