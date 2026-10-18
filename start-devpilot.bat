@echo off
setlocal

set "ROOT=%~dp0"

where java >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java was not found on PATH. Install Java 21 or add it to PATH.
    pause
    exit /b 1
)

where node >nul 2>&1
if errorlevel 1 (
    echo ERROR: Node.js was not found on PATH.
    pause
    exit /b 1
)

where npm >nul 2>&1
if errorlevel 1 (
    echo ERROR: npm was not found on PATH.
    pause
    exit /b 1
)

if not exist "%ROOT%frontend\node_modules" (
    echo ERROR: Frontend dependencies are missing. Run "npm install" in the frontend folder first.
    pause
    exit /b 1
)

if not exist "%ROOT%discovery-server\mvnw.cmd" (
    echo ERROR: A Maven wrapper is missing from the discovery-server folder.
    pause
    exit /b 1
)
if not exist "%ROOT%orchestrator-service\mvnw.cmd" (
    echo ERROR: A Maven wrapper is missing from the orchestrator-service folder.
    pause
    exit /b 1
)
if not exist "%ROOT%ai-agent-service\mvnw.cmd" (
    echo ERROR: A Maven wrapper is missing from the ai-agent-service folder.
    pause
    exit /b 1
)
if not exist "%ROOT%auth-service\mvnw.cmd" (
    echo ERROR: A Maven wrapper is missing from the auth-service folder.
    pause
    exit /b 1
)
if not exist "%ROOT%gateway-service\mvnw.cmd" (
    echo ERROR: A Maven wrapper is missing from the gateway-service folder.
    pause
    exit /b 1
)

echo Starting DevPilot services...

start "DevPilot - Discovery" /D "%ROOT%discovery-server" cmd.exe /k "mvnw.cmd spring-boot:run"
call :waitForPort 8761 "Discovery server"

start "DevPilot - Orchestrator" /D "%ROOT%orchestrator-service" cmd.exe /k "mvnw.cmd spring-boot:run"
call :waitForPort 8081 "Orchestrator service"

start "DevPilot - AI Agent" /D "%ROOT%ai-agent-service" cmd.exe /k "mvnw.cmd spring-boot:run"
call :waitForPort 8082 "AI agent service"

start "DevPilot - Auth" /D "%ROOT%auth-service" cmd.exe /k "mvnw.cmd spring-boot:run"
call :waitForPort 8083 "Auth service"

start "DevPilot - Gateway" /D "%ROOT%gateway-service" cmd.exe /k "mvnw.cmd spring-boot:run"
call :waitForPort 8080 "Gateway service"

start "DevPilot - Frontend" /D "%ROOT%frontend" cmd.exe /k "npm run dev -- --host 127.0.0.1 --strictPort"
call :waitForPort 5173 "Frontend"

echo.
echo All start commands have been issued.
echo Frontend: http://localhost:5173
echo To stop everything, run stop-devpilot.bat.
pause
exit /b 0

:waitForPort
set "PORT=%~1"
set "SERVICE=%~2"
set /a "ATTEMPTS=0"

:waitLoop
set /a "ATTEMPTS+=1"
powershell -NoProfile -Command "$client = New-Object Net.Sockets.TcpClient; try { $client.Connect('127.0.0.1', %PORT%); exit 0 } catch { exit 1 } finally { $client.Dispose() }" >nul 2>&1
if not errorlevel 1 (
    echo %SERVICE% is accepting connections on port %PORT%.
    exit /b 0
)

if %ATTEMPTS% GEQ 60 (
    echo WARNING: %SERVICE% did not open port %PORT% within 120 seconds. Check its console for errors.
    exit /b 1
)

timeout /t 2 /nobreak >nul
goto waitLoop
