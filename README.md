# AI I.Z

AI I.Z is an Android-first AI-assisted software creation workspace built with Kotlin and Jetpack Compose.

The application is designed to guide a project from idea discovery and requirements through UI/UX planning, technical architecture, implementation coordination, testing, and release preparation.

## Current repository scope

- Native Android application (Kotlin + Jetpack Compose).
- Google sign-in through Android Credential Manager, exchanged directly with the AI I.Z API for app-owned sessions (Firebase Authentication is not used by the login flow).
- User settings, tasks, approvals, chat messages, and operation logs are being moved to the AI I.Z PostgreSQL API with session-derived ownership and periodic polling. Legacy Firestore models, rules, App Check, Firebase AI, dependencies, and project configuration still remain in the repository and require final cleanup/migration.
- AI orchestration and provider failover components.
- GitHub repository, branch, commit, pull-request, and Actions integration.
- Backend Runner API client for health checks, tests, schema inspection, read-only SQL, and explicitly approved database operations.
- Local Room persistence and WorkManager task coordination.
- Arabic RTL-first interface. Full English translation and dynamic LTR switching remain unfinished and must be verified before claiming bilingual support.
- Project lifecycle, security, setup, backend API, and marketing requirements in `docs/`.

## Requirements

- JDK 21 (matching the GitHub Actions build environment).
- Android SDK / Android Studio capable of compiling SDK 36.
- Gradle 9.3.1 or newer. The repository currently does not include the Gradle Wrapper scripts/JAR, so use a locally installed Gradle or the managed Gradle installation in CI.
- Firebase project configuration for the still-Firestore-backed data features and App Check (not for login).
- A Google OAuth Web client ID and deployed HTTPS AI I.Z API with PostgreSQL for the new login/session flow.
- A deployed HTTPS Backend Runner for remote test execution and database operations.
- User-supplied provider credentials where applicable. Never commit API keys, access tokens, signing keys, or `.env` files.

## Build locally

On Windows:

```powershell
gradle testDebugUnitTest
gradle assembleDebug
```

On macOS/Linux:

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

For the app-owned login flow, provide these build-time values (do not hardcode them):

```powershell
$env:AI_IZ_API_BASE_URL = "https://your-deployed-ai-iz-api"
$env:GOOGLE_WEB_CLIENT_ID = "your-web-client-id.apps.googleusercontent.com"
gradle testDebugUnitTest
 gradle assembleDebug
```

The backend setup and required server environment are documented in `backend/README.md`.

Some repository tests use Firebase Auth and Firestore emulators. Start the configured emulators before running the complete unit-test suite:

```bash
firebase emulators:start --only auth,firestore --project demo-no-project
```

Then run the Gradle test command in another terminal. GitHub Actions performs this using `firebase emulators:exec`.

## Remaining Firebase-backed data setup

Firebase Authentication is no longer part of the login flow on the no-Firebase session branch. Google identity is verified by the AI I.Z API. The main app data repository now targets the AI I.Z PostgreSQL API, but legacy Firebase models, App Check and Firebase AI integrations remain until their usages and data migration are completed.

1. Keep the existing Firebase project configuration only while Firestore/App Check-backed features remain in use.
2. Do not configure Firebase Authentication as the app login provider.
3. Deploy and test `firestore.rules` for the existing Firestore data until the PostgreSQL data migration is complete.
4. Do not remove `google-services.json`, Firebase dependencies, App Check, or Firestore rules until all remaining Firebase AI/App Check/model usages are replaced and historical data migration is verified.

The repository intentionally does not contain a production Firebase service-account key or user credentials. Never put privileged service-account credentials in the Android application.

## Backend Runner

The Android client only defines the Backend Runner API contract. A compatible server must be deployed separately over HTTPS. See:

- [Backend API and isolated PostgreSQL contract](docs/BACKEND_API_AND_POSTGRES.md)
- [Setup and deployment guide](docs/SETUP_AND_DEPLOY_GUIDE.md)

The legacy Backend Runner client still has its own token-based contract and is not yet migrated to AI I.Z sessions. The new `backend/` API independently verifies Google OIDC and issues AI I.Z sessions. Do not assume the Backend Runner is protected by the new session system until that integration is completed.

## Security and approvals

- Keep secrets in the platform secret store; never commit them.
- Keep production credentials and signing keys outside source control.
- Treat AI-generated commands and SQL as untrusted input.
- Enforce Tier 2 and Tier 3 approvals on the backend, not only in the Android client.
- Keep legacy/protected database targets isolated.
- Do not run destructive operations or production deployments without the required explicit approval.

## CI

GitHub Actions runs Android unit tests with the existing Firestore emulator suite, assembles a debug APK, and runs backend session-security unit tests. These checks do not verify production Google OAuth configuration or a deployed PostgreSQL-backed API.

## Product requirements and design references

- [Project lifecycle](docs/AI_IZ_PROJECT_LIFECYCLE_AR.md)
- [Master implementation requirements](docs/GOOGLE_AI_STUDIO_MASTER_IMPLEMENTATION_PROMPT_AR.md)
- [Marketing and audience-growth requirements](docs/MARKETING_AND_AUDIENCE_GROWTH_REQUIREMENTS_AR.md)
- [Mobile UI reference](docs/design-reference/AI_IZ_MOBILE_UI_REFERENCE.svg)

## Status

A successful CI build validates compilation and the configured automated tests only. It does not, by itself, prove that Firebase production configuration, a Backend Runner deployment, provider credentials, Play distribution, or every end-to-end workflow is ready.
