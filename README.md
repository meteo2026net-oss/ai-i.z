# AI I.Z

AI I.Z is an Android-first AI-assisted software creation workspace built with Kotlin and Jetpack Compose.

The application is designed to guide a project from idea discovery and requirements through UI/UX planning, technical architecture, implementation coordination, testing, and release preparation.

## Current repository scope

- Native Android application (Kotlin + Jetpack Compose).
- Firebase Authentication, Firestore persistence, and App Check integration points.
- AI orchestration and provider failover components.
- GitHub repository, branch, commit, pull-request, and Actions integration.
- Backend Runner API client for health checks, tests, schema inspection, read-only SQL, and explicitly approved database operations.
- Local Room persistence and WorkManager task coordination.
- Arabic RTL-first experience with English/LTR support in the UI layer.
- Project lifecycle, security, setup, backend API, and marketing requirements in `docs/`.

## Requirements

- JDK 17.
- Android SDK / Android Studio capable of compiling SDK 36.
- Gradle Wrapper (included).
- Firebase project configuration for live authentication and Firestore features.
- A deployed HTTPS Backend Runner for remote test execution and database operations.
- User-supplied provider credentials where applicable. Never commit API keys, access tokens, signing keys, or `.env` files.

## Build locally

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

Some repository tests use Firebase Auth and Firestore emulators. Start the configured emulators before running the complete unit-test suite:

```bash
firebase emulators:start --only auth,firestore --project demo-no-project
```

Then run the Gradle test command in another terminal. GitHub Actions performs this using `firebase emulators:exec`.

## Firebase setup

1. Create or select the Firebase project intended for this app.
2. Register the Android application using the application ID in `app/build.gradle.kts`.
3. Download the matching `google-services.json` and place it in `app/google-services.json`.
4. Configure Authentication providers, Firestore, and App Check in the Firebase console.
5. Configure the production App Check provider and site key for the registered app.
6. Deploy and test the Firestore rules from `firestore.rules` against the intended Firebase project before enabling live user data.

The repository intentionally does not contain a production Firebase service-account key or user credentials. Never put privileged service-account credentials in the Android application.

## Backend Runner

The Android client only defines the Backend Runner API contract. A compatible server must be deployed separately over HTTPS. See:

- [Backend API and isolated PostgreSQL contract](docs/BACKEND_API_AND_POSTGRES.md)
- [Setup and deployment guide](docs/SETUP_AND_DEPLOY_GUIDE.md)

The server must authenticate each request, bind the caller identity to the Firebase ID token, enforce approval state on the server, use least-privilege database roles, and reject unsafe targets. A client-provided non-empty approval ID is not proof of approval.

## Security and approvals

- Keep secrets in the platform secret store; never commit them.
- Keep production credentials and signing keys outside source control.
- Treat AI-generated commands and SQL as untrusted input.
- Enforce Tier 2 and Tier 3 approvals on the backend, not only in the Android client.
- Keep legacy/protected database targets isolated.
- Do not run destructive operations or production deployments without the required explicit approval.

## CI

GitHub Actions runs Android unit tests with Firebase Auth/Firestore emulators and assembles a debug APK on pushes and pull requests to `main`. The generated APK is uploaded as a short-lived workflow artifact.

## Product requirements and design references

- [Project lifecycle](docs/AI_IZ_PROJECT_LIFECYCLE_AR.md)
- [Master implementation requirements](docs/GOOGLE_AI_STUDIO_MASTER_IMPLEMENTATION_PROMPT_AR.md)
- [Marketing and audience-growth requirements](docs/MARKETING_AND_AUDIENCE_GROWTH_REQUIREMENTS_AR.md)
- [Mobile UI reference](docs/design-reference/AI_IZ_MOBILE_UI_REFERENCE.svg)

## Status

A successful CI build validates compilation and the configured automated tests only. It does not, by itself, prove that Firebase production configuration, a Backend Runner deployment, provider credentials, Play distribution, or every end-to-end workflow is ready.
