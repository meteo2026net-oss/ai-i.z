@echo off
REM Copy this file to setup-env.local.bat (untracked) and fill real values.
REM Never commit real secrets.

set AI_IZ_API_BASE_URL=https://your-deployed-ai-iz-api.example
set GOOGLE_WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com

REM Optional for local backend/runner development only
REM set DATABASE_URL=postgresql://aiiz:change-me@localhost:5432/aiiz
REM set ACCESS_TOKEN_HMAC_SECRET_HEX=
REM set GOOGLE_OIDC_CLIENT_ID=%GOOGLE_WEB_CLIENT_ID%

echo Environment placeholders loaded for this shell session.
