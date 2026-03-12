@echo off
:: ============================================================
::  Service Deployer — wrapper for deploy.sh
::  Requires Git for Windows (Git Bash).
::
::  Usage:
::    scripts\deploy.bat                deploy all (backend)
::    scripts\deploy.bat api            deploy api only
::    scripts\deploy.bat --no-build     skip gradle build
::    scripts\deploy.bat api frontend   deploy api to frontend
::
::  Prerequisites (server, one-time):
::    sudo apt install -y python3-yaml
::    docker network create geostat-net
:: ============================================================
set "BASH=%ProgramFiles%\Git\bin\bash.exe"
if not exist "%BASH%" set "BASH=%ProgramFiles%\Git\usr\bin\bash.exe"
if not exist "%BASH%" ( echo ERROR: bash.exe not found. Install Git for Windows. & exit /b 1 )
"%BASH%" "%~dp0deploy.sh" %*