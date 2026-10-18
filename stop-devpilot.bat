@echo off
setlocal

echo Stopping DevPilot services...
taskkill /FI "WINDOWTITLE eq DevPilot - Frontend" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq DevPilot - Gateway" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq DevPilot - Auth" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq DevPilot - AI Agent" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq DevPilot - Orchestrator" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq DevPilot - Discovery" /T /F >nul 2>&1

echo Stop requests sent to all DevPilot consoles.
pause
