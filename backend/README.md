# AI I.Z API — no-Firebase identity foundation

This service owns AI I.Z identity and sessions. Android sends a short-lived Google OIDC ID token to `POST /api/auth/session`; the API verifies the token directly with Google, provisions the account by immutable Google `sub`, and issues AI I.Z access/refresh tokens. Firebase Authentication and Firebase Admin are not used.

## Local setup

1. Provision PostgreSQL 15+ and apply `db/schema.sql` for a fresh database. For an existing database created before Studio cloud storage was added, apply `db/migrations/002_studio_projects.sql` after the original schema.
2. Copy `.env.example` to `.env`; set a Google OAuth **Web application client ID**, database URL, issuer, audience, and a random 32-byte HMAC key.
3. Run `npm install`, then `npm test`, then `npm start`.

Never commit `.env`, database credentials, OAuth client secrets, signing keys, or real user tokens. The Google client ID is an audience identifier, not a secret. The Android Credential Manager must request an ID token for this same client ID.

## API

- `GET /api/health` — database connectivity.
- `POST /api/auth/session` — body: `{ "idToken": "..." }`; verifies Google OIDC and creates/loads an account by provider subject.
- `POST /api/auth/refresh` — body: `{ "refreshToken": "..." }`; rotates refresh token atomically.
- `POST /api/auth/logout` — body: `{ "refreshToken": "..." }`; revokes the entire refresh-token family even if the access token has expired.
- `GET /api/me` — current server-derived profile (Bearer access token).
- `POST /api/runner/approvals` — create an immutable, expiring approval request bound to a canonical operation hash and exact target.
- `GET /api/runner/approvals` and `GET /api/runner/approvals/{approvalId}` — list or retrieve only the caller's approval records.
- `POST /api/runner/approvals/{approvalId}/decision` — explicitly approve or reject a pending request.
- `POST /api/runner/approvals/{approvalId}/consume` — atomically consume an approved request once, requiring exact action, target, and operation hash.
- `POST /api/runner/approvals/{approvalId}/report` — record an explicitly unverified client-side outcome after consumption.
- `GET /api/data/{settings|tasks|approvals|chat_messages|operation_logs|studio_projects}` — list only the signed-in user's records.
- `GET /api/data/{collection}/{recordId}` — fetch one record owned by the signed-in user.
- `PUT /api/data/{collection}/{recordId}` — validate and create/update a user-owned record; the server overwrites `userId` from the verified session. Approval state transitions are server-validated; client execution completion is recorded as `execution_reported`, not server-verified `executed`.
- The `studio_projects` collection stores bounded Universal Studio project manifests and source files. Limits: 24 files, 48 KB per file, 220 KB aggregate file content, one entrypoint, unique safe relative paths; key/certificate and credential files are rejected. Android restores the latest project and exposes the saved-project library.
- Operation logs are append-only and cannot be overwritten or deleted.
- `DELETE /api/data/{collection}/{recordId}` — delete only a record owned by the signed-in user.

Refresh token values are random and only SHA-256 hashes are stored. Reuse of a rotated token revokes its token family. The API never trusts a client-supplied user ID. Access tokens are short-lived and bound to issuer, audience, subject, and session ID.

## Deployment requirements

Use HTTPS, a managed PostgreSQL instance with encrypted connections, backups and restricted network access. Configure environment variables in the hosting provider's secret manager. Set `ACCESS_TOKEN_ISSUER` to the deployed API origin. Set `GOOGLE_OIDC_CLIENT_ID` to the exact Web OAuth client ID configured in Google Cloud Console. Do not expose database or signing secrets to Android.

## Migration status and scope boundary

The API now includes a tenant-scoped JSONB record store for settings, tasks, approvals, chat messages, and operation logs. The Android repository has been switched to call these endpoints using the AI I.Z session. The current client uses periodic polling. CI includes PostgreSQL-backed integration tests for tenant isolation, record validation, legacy approval transitions, immutable Runner approval ownership, exact target/hash binding, one-time consumption, append-only logs, logout revocation, and revoked-session rejection. The Runner's SQL and remote execution routes remain disabled until their isolated execution and policy layers are complete.

The Android login and user-data paths no longer depend on Firebase Authentication, Firestore, or App Check. Firebase AI remains only as the existing Gemini provider integration. Historical Firestore records are not imported by this code; production use still requires applying the schema, deploying the API, configuring Android build variables, and passing end-to-end and cross-account isolation tests.
