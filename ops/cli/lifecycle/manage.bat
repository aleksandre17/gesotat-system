@echo off
:: ============================================================
::  Service Manager — wrapper for manage.sh
::  Requires Git for Windows (Git Bash).
::
::  Usage:
::    scripts\manage.bat                          interactive menu
::    scripts\manage.bat api stop                 stop api (backend)
::    scripts\manage.bat api stop frontend        stop api (frontend)
::    scripts\manage.bat all nuke -y              nuke all (no confirm)
::    scripts\manage.bat api logs errors          show error.log
::    scripts\manage.bat all logs files           list all log files
:: ============================================================
set "BASH=%ProgramFiles%\Git\bin\bash.exe"
if not exist "%BASH%" set "BASH=%ProgramFiles%\Git\usr\bin\bash.exe"
if not exist "%BASH%" ( echo ERROR: bash.exe not found. Install Git for Windows. & exit /b 1 )
"%BASH%" "%~dp0manage.sh" %*