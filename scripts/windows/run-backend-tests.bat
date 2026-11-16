@echo off
setlocal
REM AI I.Z — run Backend + Runner tests (Windows)
REM Requires: Node.js 22+

cd /d "%~dp0..\.."

echo Installing backend dependencies...
call npm install --prefix backend --no-audit --no-fund
if errorlevel 1 exit /b 1

echo Running backend tests...
call npm test --prefix backend
if errorlevel 1 exit /b 1

echo Installing runner dependencies...
call npm install --prefix runner --no-audit --no-fund
if errorlevel 1 exit /b 1

echo Running runner tests...
call npm test --prefix runner
if errorlevel 1 exit /b 1

echo All backend/runner tests passed.
endlocal
