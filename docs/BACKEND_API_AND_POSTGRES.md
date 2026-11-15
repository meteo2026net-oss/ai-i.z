# AI I.Z Backend Runner — API Contract and Security Requirements

## 1. Current implementation status

The separate AI I.Z identity API in `backend/` issues app-owned Google OIDC sessions and PostgreSQL-backed user records. This is not the Backend Runner implementation. The Android client now sends its current AI I.Z access token as the Bearer credential and no longer asks the user for a separate static Backend Runner token. The Runner server must validate that token against the AI I.Z identity service (or share the same trusted session-validation module and PostgreSQL session store); it must not accept static shared tokens.

The Android application contains a Retrofit client and this repository now contains an initial deployable Runner service in `runner/`. It verifies every caller by forwarding the Bearer token to the identity API's authenticated `GET /api/me`; it never trusts decoded claims or static tokens. `GET /api/health` and administrator-restricted `GET /api/db/schema` are implemented. The identity API now also stores immutable, user-bound Runner approval requests with an exact canonical operation hash, target, ten-minute expiry, explicit approve/reject decision, and transactional one-time consumption. Test execution and both SQL endpoints intentionally return HTTP 501 until their worker/query security gates are implemented. Approval records alone do not authorize execution; the Runner must independently consume and validate one for the exact operation.

## 2. Required API

All endpoints require an authenticated caller. The server must validate the short-lived AI I.Z access token issued by `backend/` (signature, algorithm, issuer, audience, expiry, subject, and session ID), then confirm that the session remains active and unrevoked in PostgreSQL. A separately deployed Runner should do this by calling the authenticated `GET /api/me` endpoint on the AI I.Z API with the supplied Bearer token and treating any non-success response as unauthenticated; the identity API performs the signature, audience, expiry, session, and revocation checks. The Runner must not independently trust decoded JWT claims or accept static shared bearer tokens. It must derive the user identity from the verified session. A Firebase ID token is not accepted as a substitute.

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

`POST /api/tests/run` currently returns HTTP 501 (`TEST_EXECUTION_DISABLED`). Keep it disabled until allowlisted repository/branch validation and isolated, resource-limited workers with timeouts and no ambient production credentials are implemented.

### Database schema

`GET /api/db/schema`

Returns only the schema metadata the authenticated user is authorized to inspect. Never return secrets, connection strings, credential values, or unrelated tenant data.

### Read-only SQL

`POST /api/db/query-readonly`

`POST /api/db/query-readonly` currently returns HTTP 501 (`SQL_QUERY_DISABLED`). Keep it disabled until a parser-backed allowlist, dedicated read-only database role, query timeouts, row limits, statement-size limits, and tenant scoping are implemented. Do not rely on regular expressions alone.

### Approved database operation

`POST /api/db/execute-approved`

`POST /api/db/execute-approved` currently returns HTTP 501 (`APPROVED_EXECUTION_DISABLED`). A client-provided approval identifier is not proof of approval. Keep this endpoint disabled until a trusted server-side approval store binds the authenticated user, exact target, exact operation hash, expiry, and policy tier, with one-time consumption, immediate revalidation, and an auditable rollback plan.

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

The identity API implements these authenticated endpoints:

- `POST /api/runner/approvals` — create a pending request with `actionType`, exact `targetResource`, JSON `operation`, and human-readable `preview`. The server canonicalizes the operation and stores its SHA-256 hash; requests expire after ten minutes.
- `GET /api/runner/approvals` — list only the caller's own approval records.
- `GET /api/runner/approvals/{approvalId}` — retrieve only the caller's own approval metadata.
- `POST /api/runner/approvals/{approvalId}/decision` — explicitly approve or reject a pending request, with an optional bounded note.
- `POST /api/runner/approvals/{approvalId}/consume` — atomically consume an approved request once, but only when action, target, caller, and operation hash match the stored record. It returns the stored operation to the authenticated caller.
- `POST /api/runner/approvals/{approvalId}/report` — store a client-reported success/failure after consumption. The response explicitly sets `executionVerified: false`; this is an audit note, not proof that the remote operation actually succeeded.

These endpoints provide the approval data layer, not an execution engine. The Runner's execution endpoints remain disabled until they call the consume endpoint and independently enforce the exact approved operation, target isolation, and action-specific policy.

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
