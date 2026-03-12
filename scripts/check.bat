@echo off
set "BASH=%ProgramFiles%\Git\bin\bash.exe"
if not exist "%BASH%" set "BASH=%ProgramFiles%\Git\usr\bin\bash.exe"
if not exist "%BASH%" ( echo ERROR: bash.exe not found. Install Git for Windows. & exit /b 1 )
"%BASH%" "%~dp0check.sh" %*