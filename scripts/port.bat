@echo off
if "%1"=="" (
    echo Usage: kill-port.bat ^<port^>
    echo Example: kill-port.bat 8081
    exit /b 1
)

set PORT=%1

for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%PORT% "') do (
    echo Killing PID %%a on port %PORT%...
    taskkill /PID %%a /F >nul 2>&1
)

echo Port %PORT% is now free.