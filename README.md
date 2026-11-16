# AI I.Z

AI I.Z is an Android-first AI-assisted software creation workspace built with Kotlin and Jetpack Compose.

The application is designed to guide a project from idea discovery and requirements through UI/UX planning, technical architecture, implementation coordination, testing, and release preparation.

## Current repository scope

- Native Android application (Kotlin + Jetpack Compose).
- Google sign-in through Android Credential Manager, exchanged directly with the AI I.Z API for app-owned sessions (Firebase Authentication is not used by the login flow).
- User settings, tasks, approvals, chat messages, and operation logs use the AI I.Z PostgreSQL API with session-derived ownership and periodic polling.
- AI orchestration and provider failover components.
- GitHub repository, branch, commit, pull-request, and Actions integration.
- Separate Backend Runner client and Node.js service for authenticated health checks and restricted PostgreSQL schema inspection.
- Local Room persistence and WorkManager task coordination.
- Arabic RTL-first interface. Full English translation and dynamic LTR switching remain unfinished and must be verified before claiming bilingual support.
- Project lifecycle, security, setup, backend API, implementation-readiness, and marketing requirements in `docs/`.

## Requirements

- JDK 21 (matching the GitHub Actions build environment).
- Android SDK / Android Studio capable of compiling SDK 36.
- Gradle 9.3.1 or newer. The repository includes the Gradle Wrapper (`./gradlew` / `gradlew.bat`). Prefer the wrapper for local builds; CI also uses a managed Gradle installation.
- Firebase project configuration is currently required only by the Firebase AI Gemini provider integration, not by login or user-data persistence.
- A Google OAuth Web client ID and deployed HTTPS AI I.Z API with PostgreSQL for the app-owned login/session flow.
- A separately deployed HTTPS Backend Runner and dedicated isolated PostgreSQL database for the limited Runner capabilities currently implemented.
- User-supplied provider credentials where applicable. Never commit API keys, access tokens, signing keys, or `.env` files.

## Signed release build

A manual GitHub Actions workflow is provided at **Actions → Android signed release build → Run workflow**. Before running it, configure these repository secrets:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded Android upload/release keystore.
- `ANDROID_STORE_PASSWORD`: keystore password.
- `ANDROID_KEY_PASSWORD`: key password.

Configure these repository variables:

- `AI_IZ_API_BASE_URL`: deployed HTTPS AI I.Z API origin.
- `GOOGLE_WEB_CLIENT_ID`: matching Google Web OAuth client ID.

The workflow runs the Android unit-test task supported by the current Gradle configuration (`testDebugUnitTest`), then builds the signed Release APK and AAB and uploads them as the `ai-iz-signed-release` artifact. A separate CI workflow also validates minified Release APK/AAB builds with an ephemeral CI-only key; those artifacts are not production-signed. It does **not** publish to Google Play or create a GitHub release. Keep the keystore and passwords in GitHub Actions secrets only; never paste them into chat or commit them. The signing alias expected by the current Gradle configuration is `upload`.

## Build locally

Prefer the Gradle Wrapper included in the repository.

On Windows:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

On macOS/Linux:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

For the app-owned login flow, provide these build-time values (do not hardcode them):

```powershell
$env:AI_IZ_API_BASE_URL = "https://your-deployed-ai-iz-api"
$env:GOOGLE_WEB_CLIENT_ID = "your-web-client-id.apps.googleusercontent.com"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

The backend setup and required server environment are documented in `backend/README.md`.

## Firebase boundary

- Google Credential Manager obtains a Google ID token; the AI I.Z API verifies it directly and issues app-owned sessions. Firebase Authentication and Firebase Admin are not part of login.
- Firebase Authentication, Firestore persistence, and App Check have been removed from the identity/user-data path in this branch.
- Firebase AI remains for the existing Gemini provider integration. The Google Services Gradle plugin and Firebase AI dependency are retained until that provider is replaced with direct Gemini API calls.
- Historical Firestore records are not imported by this code and require a separate controlled export/import process if they must be retained.
- The repository intentionally contains no production Firebase service-account key or user credentials. Never put privileged service-account credentials in the Android application.

## Backend Runner — current limits

The Android client sends its current AI I.Z access token to the separately deployed Runner. The Runner validates the token against the AI I.Z identity API. The identity API exposes PostgreSQL-backed approval requests, decisions, listing, and one-time operation-bound consumption.

The Runner currently implements health checks and restricted PostgreSQL schema metadata inspection. Remote test execution, read-only SQL execution, and approved privileged execution routes deliberately return HTTP 501. They must remain disabled until an isolated worker, parser-backed SQL policy, least-privilege credentials, and audited server-side approval consumption are implemented.

The Android approvals screen uses server-owned approval records. The client can record approval or rejection, but **does not execute privileged GitHub or database operations after approval**. Client-side reports are not proof of execution.

See:

- [Backend API and isolated PostgreSQL contract](docs/BACKEND_API_AND_POSTGRES.md)
- [Setup and deployment guide](docs/SETUP_AND_DEPLOY_GUIDE.md)
- [Runner service and current execution limits](runner/README.md)
- [Implementation readiness register](docs/IMPLEMENTATION_READINESS_AR.md)

## Security and approvals

- Keep secrets in the platform secret store; never commit them.
- Keep production credentials and signing keys outside source control.
- Treat AI-generated commands and SQL as untrusted input.
- Approval ownership, expiry, action, target, and operation hash are enforced by the identity backend.
- The Runner must independently consume the exact approval before any future execution.
- Keep legacy/protected database targets isolated.
- Do not enable destructive operations or production deployments until isolated execution, least-privilege credentials, and server-side audit verification are in place.

## CI and verified build

GitHub Actions runs Android unit tests, assembles a debug APK, and runs backend session, PostgreSQL API, and Runner security tests without starting Firebase emulators.

Latest verified run on 2026-11-16: [GitHub Actions run 43835553782](https://github.com/meteo2026net-oss/ai-i.z/actions/runs/43835553782) passed both Android and Backend session/API integration jobs for commit `b5ae94f6e6ad512bb4c78622109134b1f04d0226`. It uploaded `ai-iz-debug-apk` (artifact ID `14210492810`, SHA-256 `8ef7d2bf4b1a9519747510d5c0aa2d2f8ec0d0f0d4637e459c3a943208a90262`, 24,148,506 bytes; expires 2026-11-23 09:01:24 UTC). This CI run includes the latest Universal Studio stale-response and failed-delete recovery fixes. The artifact is a temporary debug build, not a signed production release. CI does not verify production OAuth configuration, deployed services, physical-device behavior, or every end-to-end workflow.

## Product requirements and design references

- [Project lifecycle](docs/AI_IZ_PROJECT_LIFECYCLE_AR.md)
- [Master implementation requirements](docs/GOOGLE_AI_STUDIO_MASTER_IMPLEMENTATION_PROMPT_AR.md)
- [Marketing and audience-growth requirements](docs/MARKETING_AND_AUDIENCE_GROWTH_REQUIREMENTS_AR.md)
- [Mobile UI reference](docs/design-reference/AI_IZ_MOBILE_UI_REFERENCE.svg)

## Status

See [docs/IMPLEMENTATION_READINESS_AR.md](docs/IMPLEMENTATION_READINESS_AR.md) for verified implementation, test evidence, and remaining release gates. A successful CI build validates compilation and configured automated tests only; it does not prove production readiness.
