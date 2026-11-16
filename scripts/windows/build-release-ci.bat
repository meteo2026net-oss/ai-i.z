@echo off
setlocal
REM Builds minified Release APK/AAB using a temporary local keystore for CI-style validation.
REM This is NOT a production signing key. Do not distribute these outputs as production releases.

cd /d "%~dp0..\.."

if "%AI_IZ_API_BASE_URL%"=="" (
  echo [WARN] AI_IZ_API_BASE_URL is not set.
)
if "%GOOGLE_WEB_CLIENT_ID%"=="" (
  echo [WARN] GOOGLE_WEB_CLIENT_ID is not set.
)

set "TMP_JKS=%TEMP%\ai-iz-ci-upload.jks"
if exist "%TMP_JKS%" del /f /q "%TMP_JKS%" >nul 2>&1

where keytool >nul 2>&1
if errorlevel 1 (
  echo keytool not found. Ensure JDK 21 bin is on PATH.
  exit /b 1
)

echo Generating temporary CI keystore at %TMP_JKS% ...
keytool -genkeypair -keystore "%TMP_JKS%" -storepass android -keypass android -alias androidci -keyalg RSA -keysize 2048 -validity 30 -dname "CN=AI-IZ-CI,O=AI-IZ,C=US" >nul
if errorlevel 1 (
  echo Failed to generate temporary keystore.
  exit /b 1
)

set "KEYSTORE_PATH=%TMP_JKS%"
set "STORE_PASSWORD=android"
set "KEY_PASSWORD=android"

echo Running unit tests and minified release assemble/bundle...
call gradlew.bat --no-daemon testDebugUnitTest assembleRelease bundleRelease
set "EC=%ERRORLEVEL%"

del /f /q "%TMP_JKS%" >nul 2>&1

if not "%EC%"=="0" (
  echo BUILD FAILED
  exit /b %EC%
)

echo.
echo Release APK: app\build\outputs\apk\release\
echo Release AAB: app\build\outputs\bundle\release\
echo NOTE: Signed with a temporary local keystore only. Not for Play production upload.
endlocal
