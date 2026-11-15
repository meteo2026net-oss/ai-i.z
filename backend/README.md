# AI I.Z API — no-Firebase identity foundation

This service owns AI I.Z identity and sessions. Android sends a short-lived Google OIDC ID token to `POST /api/auth/session`; the API verifies the token directly with Google, provisions the account by immutable Google `sub`, and issues AI I.Z access/refresh tokens. Firebase Authentication and Firebase Admin are not used.

## Local setup

1. Provision PostgreSQL 15+ and apply `db/schema.sql`.
2. Copy `.env.example` to `.env`; set a Google OAuth **Web application client ID**, database URL, issuer, audience, and a random 32-byte HMAC key.
3. Run `npm install`, then `npm test`, then `npm start`.

Never commit `.env`, database credentials, OAuth client secrets, signing keys, or real user tokens. The Google client ID is an audience identifier, not a secret. The Android Credential Manager must request an ID token for this same client ID.

## API

- `GET /api/health` — database connectivity.
- `POST /api/auth/session` — body: `{ "idToken": "..." }`; verifies Google OIDC and creates/loads an account by provider subject.
- `POST /api/auth/refresh` — body: `{ "refreshToken": "..." }`; rotates refresh token atomically.
- `POST /api/auth/logout` — revoke current AI I.Z session (Bearer access token).
- `GET /api/me` — current server-derived profile (Bearer access token).

Refresh token values are random and only SHA-256 hashes are stored. Reuse of a rotated token revokes its token family. The API never trusts a client-supplied user ID. Access tokens are short-lived and bound to issuer, audience, subject, and session ID.

## Deployment requirements

Use HTTPS, a managed PostgreSQL instance with encrypted connections, backups and restricted network access. Configure environment variables in the hosting provider's secret manager. Set `ACCESS_TOKEN_ISSUER` to the deployed API origin. Set `GOOGLE_OIDC_CLIENT_ID` to the exact Web OAuth client ID configured in Google Cloud Console. Do not expose database or signing secrets to Android.

## Scope boundary

This is the first backend implementation slice. It does not yet migrate Firestore collections, connect Android Credential Manager to these endpoints, or constitute a production deployment. Do not remove existing Firebase app code until each feature has a tested replacement and data migration plan.
