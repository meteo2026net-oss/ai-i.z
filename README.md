# AI I.Z

AI I.Z is an Android-first AI-assisted software creation workspace built with Kotlin and Jetpack Compose.

The application is designed to guide a project from idea discovery and requirements through UI/UX planning, technical architecture, implementation coordination, testing, and release preparation.

## Current repository scope

- Native Android application (Kotlin + Jetpack Compose).
- Google sign-in through Android Credential Manager, exchanged directly with the AI I.Z API for app-owned sessions (Firebase Authentication is not used by the login flow).
- User settings, tasks, approvals, chat messages, and operation logs now use the AI I.Z PostgreSQL API with session-derived ownership and periodic polling. Firebase Authentication, Firestore persistence, and App Check have been removed from the Android login/data path. Firebase AI remains as the existing Gemini provider integration; it is not used for identity or user-data storage.
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
- Firebase project configuration is currently required only by the Firebase AI Gemini provider integration, not by login or user-data persistence.
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


## Firebase boundary

- Google Credential Manager obtains a Google ID token; the AI I.Z API verifies it directly and issues app-owned sessions. Firebase Authentication and Firebase Admin are not part of login.
- The Android user-data repository uses PostgreSQL API endpoints. Firestore repository code, models, rules, emulator tests, and App Check initialization have been removed from this branch.
- Firebase AI remains for the existing Gemini provider integration. The Google Services Gradle plugin and Firebase AI dependency are retained until that provider is replaced with direct Gemini API calls.
- Historical Firestore records are not imported by this code and require a separate controlled export/import process if they must be retained.
- The repository intentionally contains no production Firebase service-account key or user credentials. Never put privileged service-account credentials in the Android application.

## Backend Runner

The Android client only defines the Backend Runner API contract. A compatible server must be deployed separately over HTTPS. See:

- [Backend API and isolated PostgreSQL contract](docs/BACKEND_API_AND_POSTGRES.md)
- [Setup and deployment guide](docs/SETUP_AND_DEPLOY_GUIDE.md)

The Android Backend Runner client now sends the current AI I.Z access token instead of a separate static token. A compatible Runner server must validate the session against the AI I.Z identity API; no deployable Runner server is included yet. The new `backend/` API independently verifies Google OIDC and issues AI I.Z sessions. Do not assume the Backend Runner is protected by the new session system until that integration is completed.

## Security and approvals

- Keep secrets in the platform secret store; never commit them.
- Keep production credentials and signing keys outside source control.
- Treat AI-generated commands and SQL as untrusted input.
- Enforce Tier 2 and Tier 3 approvals on the backend, not only in the Android client.
- Keep legacy/protected database targets isolated.
- Do not run destructive operations or production deployments without the required explicit approval.

## CI

GitHub Actions runs Android unit tests, assembles a debug APK, and runs backend session and record-validation tests without starting Firebase emulators. These checks do not verify production Google OAuth configuration or a deployed PostgreSQL-backed API.

## Product requirements and design references

- [Project lifecycle](docs/AI_IZ_PROJECT_LIFECYCLE_AR.md)
- [Master implementation requirements](docs/GOOGLE_AI_STUDIO_MASTER_IMPLEMENTATION_PROMPT_AR.md)
- [Marketing and audience-growth requirements](docs/MARKETING_AND_AUDIENCE_GROWTH_REQUIREMENTS_AR.md)
- [Mobile UI reference](docs/design-reference/AI_IZ_MOBILE_UI_REFERENCE.svg)

## Status

A successful CI build validates compilation and configured automated tests only. It does not prove that the Google OAuth client, PostgreSQL production database, AI I.Z API deployment, Backend Runner, provider credentials, Play distribution, or every end-to-end workflow is ready.
