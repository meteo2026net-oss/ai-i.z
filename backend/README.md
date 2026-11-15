# AI I.Z Backend Runner

This service is an API foundation, not a production deployment. It validates Firebase ID tokens (including revocation checks), provides a health endpoint, exposes only public-schema metadata, and executes conservative SELECT-only SQL using a dedicated read-only PostgreSQL connection.

## Runtime

- Node.js 22
- HTTPS termination at a trusted reverse proxy or managed platform
- Firebase Admin credentials supplied through Application Default Credentials
- `FIREBASE_PROJECT_ID` set to the exact Firebase project
- `DATABASE_URL_READONLY` set only to a dedicated least-privilege read-only role on an isolated AI I.Z database
- `DATABASE_SSL` defaults to certificate verification; do not set it to `disable` in production

Never commit service-account JSON, database URLs, tokens, or private keys. Configure them in the hosting provider's secret manager.

## Routes

All routes require `Authorization: Bearer <Firebase ID token>`.

- `GET /api/health`
- `GET /api/db/schema`
- `POST /api/db/query-readonly` with `targetDatabase` and `sql`
- `POST /api/tests/run` currently returns 503 until an isolated worker is configured
- `POST /api/db/execute-approved` currently returns 503 until immutable server-side approval verification, replay protection, and a restricted migration role are implemented and tested

The read-only endpoint accepts a conservative single SELECT statement, uses a read-only transaction, limits execution time and returned rows, and never exposes database credentials. The PostgreSQL role itself must have no write grants. Application-side SQL checks are defense in depth, not a substitute for database permissions.

## Deployment gates

Do not connect this service to production databases. Before enabling remote test execution or approved mutations, implement isolated workers and server-side approval records bound to user, tenant, target, operation hash, expiry, and one-time execution state. Add integration tests against disposable Firebase and PostgreSQL instances. Deploy only after HTTPS, identity verification, least privilege, audit retention, backups, and restore tests are verified.
