# Windows helper scripts

These scripts assume you are running from a developer machine with:

- JDK 21
- Android SDK (for Android builds)
- Node.js 22+ (for backend/runner tests)
- The repository root containing `gradlew.bat`

## Scripts

- `build-debug.bat` — runs `testDebugUnitTest` + `assembleDebug` via the Gradle Wrapper.
- `run-backend-tests.bat` — installs dependencies and runs backend + runner tests.
- `set-build-env.example.bat` — template for setting `AI_IZ_API_BASE_URL` and `GOOGLE_WEB_CLIENT_ID` in the current shell (copy and customize; do not commit real values).

Set environment variables before building the app for real login:

```bat
set AI_IZ_API_BASE_URL=https://your-deployed-api
set GOOGLE_WEB_CLIENT_ID=your-client-id.apps.googleusercontent.com
scripts\windows\build-debug.bat
```

Never put real secrets into these scripts or commit `.env` files.
