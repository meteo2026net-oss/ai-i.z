# Windows helper scripts

These scripts assume you are running from a developer machine with:

- JDK 21
- Android SDK (for Android builds)
- Node.js 22+ (for backend/runner tests)
- The repository root containing `gradlew.bat`

## Scripts

- `build-debug.bat` — runs `testDebugUnitTest` + `assembleDebug` via the Gradle Wrapper.
- `run-backend-tests.bat` — installs dependencies and runs backend + runner tests.
- `set-build-env.example.bat` / `setup-env.example.bat` — templates for build-time env vars (copy and customize; do not commit real values).
- `build-release-ci.bat` — minified Release APK/AAB using a **temporary** local keystore for validation only (not production).

Set environment variables before building the app for real login:

```bat
set AI_IZ_API_BASE_URL=https://your-deployed-api
set GOOGLE_WEB_CLIENT_ID=your-client-id.apps.googleusercontent.com
scripts\windows\build-debug.bat
```

Never put real secrets into these scripts or commit `.env` files.
