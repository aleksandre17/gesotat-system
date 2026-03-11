@echo off
:: ============================================================
::  GeoStat — Deploy to Server (Dynamic)
::
::  Discovers services from docker-compose.prod.yml.
::  Jar files auto-detected from build/libs/*.jar
::
::  Usage:
::    scripts\deploy.bat              deploy all
::    scripts\deploy.bat api          deploy api only
::    scripts\deploy.bat mobile       deploy mobile only
::    scripts\deploy.bat --no-build   skip gradle build
:: ============================================================
setlocal enabledelayedexpansion

set SERVER=administrator@192.168.1.199
set SERVER_BASE=/home/administrator
set PROJECT=%~dp0..
set COMPOSE=docker-compose -f docker-compose.prod.yml --env-file .env.prod

set SERVICE=all
set TARGET=backend
set SKIP_BUILD=0
for %%A in (%*) do (
    if "%%A"=="--no-build"  ( set SKIP_BUILD=1
    ) else if "%%A"=="all"      ( set SERVICE=all
    ) else if "%%A"=="backend"  ( set TARGET=backend
    ) else if "%%A"=="frontend" ( set TARGET=frontend
    ) else ( set SERVICE=%%A )
)

set REMOTE=%SERVER_BASE%/geostat/%TARGET%

echo.
echo  ==========================================
echo   GeoStat Deploy [%SERVICE%] → %TARGET%
echo  ==========================================
echo.
cd /d "%PROJECT%"

:: ── Step 1: Build ──
if %SKIP_BUILD%==1 (
    echo  [1/4] Skipped
    goto :step2
)

:: Ensure SERVICE variable is properly expanded before use.
set SERVICE=%SERVICE: =%

echo Building Service -> [%SERVICE%]
echo  [1/4] Building...
if "%SERVICE%"=="all" (
    call gradlew.bat build -x test --no-daemon -q
) else (
    call gradlew.bat :%SERVICE%:bootJar -x test --no-daemon -q -PactiveModules=%SERVICE%
)
if !ERRORLEVEL! NEQ 0 (
    echo  [FAIL] Build failed!
    exit /b 1
)
echo  [OK] Build done

:: ── Step 2: Find and prepare jars ──
:step2
echo  [2/4] Preparing jars...

:: Create remote dir + upload compose first (needed for service discovery)
ssh %SERVER% "mkdir -p %REMOTE%"
scp "%PROJECT%\docker-compose.prod.yml" "%PROJECT%\.env.prod" %SERVER%:%REMOTE%/ >nul

:: Discover services from compose on server
set SVC_COUNT=0
for /f "tokens=1" %%S in ('ssh %SERVER% "cd %REMOTE% && %COMPOSE% config --services 2>/dev/null"') do (
    set /a SVC_COUNT+=1
    set "SVC_!SVC_COUNT!=%%S"
)

:: For each service, find the boot jar and prepare it
for /L %%i in (1,1,%SVC_COUNT%) do (
    set "S=!SVC_%%i!"
    if "%SERVICE%"=="all" (
        call :prepare_jar "!S!"
    ) else if "%SERVICE%"=="!S!" (
        call :prepare_jar "!S!"
    )
)
goto :step3

:prepare_jar
set "S=%~1"
:: Find boot jar (prefer *-boot.jar, then *.jar excluding *-plain.jar)
set "JAR="
for %%J in ("%PROJECT%\%S%\build\libs\*-boot.jar") do set "JAR=%%J"
if "!JAR!"=="" (
    for %%J in ("%PROJECT%\%S%\build\libs\*.jar") do (
        echo %%~nxJ | findstr /i "plain" >nul || set "JAR=%%J"
    )
)
if "!JAR!"=="" (
    echo  [WARN] No jar found for %S%, skipping.
    goto :eof
)
:: Determine target name from Dockerfile COPY line
set "TARGET="
for /f "tokens=2" %%T in ('findstr /i "^COPY.*\.jar" "%PROJECT%\%S%\Dockerfile" 2^>nul') do set "TARGET_RAW=%%T"
if defined TARGET_RAW (
    :: Extract just the jar filename (before the space/destination)
    for /f "tokens=1" %%F in ('findstr /i "^COPY.*\.jar" "%PROJECT%\%S%\Dockerfile" 2^>nul') do (
        for /f "tokens=2" %%G in ("%%F") do set "FOUND=%%G"
    )
)
:: Fallback: use service name
if not defined TARGET_RAW set "TARGET_RAW=%S%.jar"
:: Copy to expected name
set "DEST=%PROJECT%\%S%\%TARGET_RAW%"
copy /y "!JAR!" "%PROJECT%\%S%\%TARGET_RAW%" >nul 2>nul
echo        %S%\%TARGET_RAW%
goto :eof

:: ── Step 3: Upload ──
:step3
echo  [3/4] Uploading...

:: Create service dirs on server
set "MKDIRS="
for /L %%i in (1,1,%SVC_COUNT%) do set "MKDIRS=!MKDIRS! %REMOTE%/!SVC_%%i!"
ssh %SERVER% "mkdir -p !MKDIRS! %REMOTE%/logs"

:: Upload Dockerfile + jar per service
for /L %%i in (1,1,%SVC_COUNT%) do (
    set "S=!SVC_%%i!"
    if "%SERVICE%"=="all" (
        call :upload_service "!S!"
    ) else if "%SERVICE%"=="!S!" (
        call :upload_service "!S!"
    )
)
echo  [OK] Uploaded
goto :step4

:upload_service
set "S=%~1"
scp "%PROJECT%\%S%\Dockerfile" %SERVER%:%REMOTE%/%S%/ 2>nul
for %%J in ("%PROJECT%\%S%\*.jar") do scp "%%J" %SERVER%:%REMOTE%/%S%/
goto :eof

:: ── Step 4: Docker ──
:step4
echo  [4/4] Starting Docker...
if "%SERVICE%"=="all" (
    ssh %SERVER% "cd %REMOTE% && %COMPOSE% down 2>/dev/null; %COMPOSE% up --build -d 2>&1; sleep 3; docker ps --filter name=geostat"
) else (
    ssh %SERVER% "cd %REMOTE% && %COMPOSE% up --build -d %SERVICE% 2>&1; sleep 3; docker ps --filter name=geostat"
)

echo.
echo  ==========================================
echo   DONE [%SERVICE%]
echo   Manage:  scripts\manage.bat all status
echo  ==========================================

