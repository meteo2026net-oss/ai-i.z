# AI I.Z — No-Firebase Identity and Data Architecture

Status: accepted implementation direction; migration not yet complete.

## Product requirement

AI I.Z must not require Firebase for sign-in, identity, data storage, authorization, or runtime services. After the user signs in, the app must connect to the AI I.Z backend automatically. The user must not paste a backend token, configure a Firebase project, or manually link their app account.

The current Android app already presents Google sign-in. This decision retains Google as the identity provider unless product requirements change; Google is used directly through OpenID Connect, not through Firebase Authentication.

## Target architecture

1. Android obtains a Google OpenID Connect ID token using Android Credential Manager.
2. Android sends that short-lived token to `POST /api/auth/session` over HTTPS.
3. Backend verifies the token signature, issuer, audience (the configured Google OAuth client ID), expiry, and verified-email claim using Google's maintained verification library.
4. Backend resolves or creates the AI I.Z account using the immutable Google subject (`sub`), not the email address. Email is a profile attribute, not the account primary key.
5. Backend issues a short-lived AI I.Z access token and a rotating, revocable refresh session. Refresh tokens are stored hashed server-side; raw tokens are only returned once.
6. Android stores session credentials in Android Keystore-backed encrypted storage and attaches access tokens automatically. Expiry triggers refresh; invalid/revoked sessions return the user to sign-in.
7. All user-owned records are scoped by an immutable `user_id` on the server. The server derives the identity from the verified session, never from a client-supplied owner ID.

## Data platform

- PostgreSQL is the authoritative cloud data store.
- The app may use Room for an encrypted/offline local cache, with explicit synchronization and conflict rules.
- Every user-owned table must include `user_id`; server queries must enforce ownership in repository/service code and preferably PostgreSQL Row-Level Security.
- GitHub, AI provider, and database credentials remain encrypted on device or in a server-side secret manager as appropriate. They must never be copied into ordinary user profile records.
- No Firebase Auth, Firestore, App Check, Firebase AI, Firebase Admin SDK, or Firebase emulator is part of the target runtime.

## Required API surface

- `POST /api/auth/session`: exchange Google ID token for an AI I.Z session.
- `POST /api/auth/refresh`: rotate refresh session.
- `POST /api/auth/logout`: revoke current refresh session.
- `GET /api/me`: return server-derived account profile.
- Authenticated application APIs: derive `user_id` from the verified AI I.Z access token.
- Existing health/schema/read-only APIs remain unavailable until the caller is authenticated and the database role and tenant isolation are verified.

## Security invariants

- Never trust a user ID, email, database target, approval state, or role supplied by the Android client.
- Never accept Google ID tokens as long-lived API bearer tokens.
- Never store raw refresh tokens in the database; hash them and rotate on every refresh.
- Bind refresh sessions to a user and revoke them on logout, account disablement, or detected reuse.
- Enforce HTTPS outside local development. Never ship production secrets in the APK or repository.
- Do not expose arbitrary SQL or cross-user schema/data through a generic read-only endpoint. Replace it with allowlisted metadata and parameterized, user-scoped operations.
- Keep database mutations and remote command execution disabled until immutable server-side approvals, replay protection, audit records, and isolated workers exist.

## Migration order (no partial Firebase removal presented as completion)

1. **Identity backend:** Google OIDC verification, account provisioning by `sub`, session issuance/rotation/revocation, and tests.
2. **Android identity:** replace Firebase sign-in with Credential Manager + AI I.Z session exchange; automatic backend discovery/configuration and encrypted session storage.
3. **Data migration:** inventory every Firestore collection and consumer; define PostgreSQL schema, ownership, indexes, RLS, migrations, and backup/restore.
4. **Repository migration:** replace Firestore repositories and listeners with authenticated API repositories; preserve offline behavior with Room and synchronization.
5. **Remove Firebase:** delete Firebase initialization/App Check, Firestore/Auth/AI dependencies, plugins, config files, emulator workflow, rules, and Firebase-only tests only after replacements pass.
6. **Release validation:** clean build with no Firebase artifacts, API integration tests, account isolation tests, backup/restore tests, and signed release build.

## Current state and gate

The existing Backend Runner PR still uses Firebase Admin token verification and must not be merged as the final no-Firebase implementation. The existing app also contains Firestore repositories, Firebase initialization, Firebase dependencies, emulator tests, and Firestore rules. This architecture document does not claim those dependencies have been removed. The migration is complete only after the clean-build and end-to-end gates above pass.
