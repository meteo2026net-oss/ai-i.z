@echo off
setlocal
REM AI I.Z — local Debug build helper (Windows)
REM Requires: JDK 21, Android SDK, and the Gradle Wrapper in the repo root.

cd /d "%~dp0..\.."

if "%AI_IZ_API_BASE_URL%"=="" (
  echo [WARN] AI_IZ_API_BASE_URL is not set. Login against a real API will fail.
)
if "%GOOGLE_WEB_CLIENT_ID%"=="" (
  echo [WARN] GOOGLE_WEB_CLIENT_ID is not set. Google sign-in will fail.
)

echo Running unit tests and assembling Debug APK...
call gradlew.bat --no-daemon testDebugUnitTest assembleDebug
if errorlevel 1 (
  echo BUILD FAILED
  exit /b 1
)

echo.
echo Debug APK: app\build\outputs\apk\debug\app-debug.apk
endlocal
