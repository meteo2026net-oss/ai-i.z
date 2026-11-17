@echo off
REM Example only — copy to a local untracked file and fill real values.
REM Never commit real URLs or client IDs if they are sensitive in your environment.

set AI_IZ_API_BASE_URL=https://your-deployed-ai-iz-api.example
set GOOGLE_WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com

echo Build environment variables set for this shell session.
echo AI_IZ_API_BASE_URL=%AI_IZ_API_BASE_URL%
echo GOOGLE_WEB_CLIENT_ID=%GOOGLE_WEB_CLIENT_ID%
