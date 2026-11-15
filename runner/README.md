# AI I.Z Backend Runner

A separate Node.js 22 service for authenticated backend health and PostgreSQL schema inspection.

## Implemented

- Every `/api/*` request requires an AI I.Z access token. The Runner forwards it to the identity API authenticated `GET /api/me` endpoint and derives the caller from the returned server profile.
- `GET /api/health` checks the configured Runner PostgreSQL connection and reports the actual database name.
- `GET /api/db/schema` returns PostgreSQL metadata for the `public` schema only to AI I.Z user IDs explicitly listed in `RUNNER_SCHEMA_USER_IDS`. With an empty allowlist, all schema inspection is denied. It does not read table rows or return connection details.
- The database pool has connection, statement, and query timeouts.

## Approval boundary

The identity API (`backend/`) now exposes PostgreSQL-backed approval request, decision, listing, one-time consumption, and explicitly unverified client-report endpoints under `/api/runner/approvals`. Runner execution routes do not yet consume those approvals and remain disabled; do not treat an approval record or client report as proof of completed execution.

## Intentionally disabled

- `POST /api/tests/run` returns HTTP 501 until an allowlisted, isolated worker exists.
- `POST /api/db/query-readonly` returns HTTP 501 until parser-backed query restrictions and a dedicated read-only database role are available.
- `POST /api/db/execute-approved` returns HTTP 501 until immutable server-side approvals, target binding, one-time consumption, and audited execution are implemented.

Do not remove these gates by returning fabricated success responses. The Android client must treat HTTP 501 as an unavailable capability.

## Setup

1. Use Node.js 22 or newer.
2. Provision a dedicated Runner database, separate from the identity database and all legacy/protected databases.
3. Copy `.env.example` to a private environment configuration and set `DATABASE_URL` and `AI_IZ_API_BASE_URL`.
4. Use a PostgreSQL account with only the privileges required for catalog metadata inspection. Do not give this service a production superuser or migration credential.
5. Set `RUNNER_DATABASE_ISOLATED=true` only after the target database has been independently verified as isolated.
6. Add only trusted operator account IDs to `RUNNER_SCHEMA_USER_IDS` (comma-separated); leave it empty to deny schema access to everyone.
7. Run `npm install --prefix runner`, `npm test --prefix runner`, then `npm start --prefix runner`.
7. Publish behind HTTPS. Do not log Authorization headers, access tokens, or database URLs.

The Runner database connection is independent of the AI I.Z identity API connection. Schema inspection currently exposes metadata for `public` only and is not a data export feature.
