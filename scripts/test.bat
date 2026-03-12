@echo off
:: ============================================================
::  Deployment Smoke Tester — Local Dry-Run
::
::  Tests deployment prerequisites and configuration locally
::  WITHOUT actually deploying anything.
::
::  Usage:
::    scripts\test.bat              run all checks
::    scripts\test.bat --ssh        include SSH/server checks
::    scripts\test.bat --build      include gradle build check
:: ============================================================
setlocal enabledelayedexpansion

:: ══════════════════════════════════════════════
::  CONFIG — must match deploy.bat
set SERVER=administrator@192.168.1.199
set SERVER_BASE=/home/administrator
set PROJECT=geostat
:: ══════════════════════════════════════════════

set PROJECT_DIR=%~dp0..
set COMPOSE_FILE=%PROJECT_DIR%\docker-compose.prod.yml
set CHECK_SSH=0
set CHECK_BUILD=0

for %%A in (%*) do (
    if "%%A"=="--ssh"   set CHECK_SSH=1
    if "%%A"=="--build" set CHECK_BUILD=1
)

set PASS=0
set FAIL=0
set WARN=0

echo.
echo  ==========================================
echo   Deployment Smoke Test  [%PROJECT%]
echo  ==========================================
echo.

:: ──────────────────────────────────────────────
:: [1] Python available?
:: ──────────────────────────────────────────────
echo  [CHECK] Python availability...
set PYTHON=
where python >nul 2>&1 && set PYTHON=python
if "!PYTHON!"=="" where python3 >nul 2>&1 && set PYTHON=python3
if "!PYTHON!"=="" (
    echo         [FAIL] Python not found. Install Python 3 and ensure it's in PATH.
    echo               Server deploy uses: sudo apt install -y python3-yaml
    set /a FAIL+=1
) else (
    for /f "tokens=*" %%V in ('!PYTHON! --version 2^>^&1') do set PYVER=%%V
    echo         [OK]   !PYVER! ^(!PYTHON!^)
    set /a PASS+=1
)

:: ──────────────────────────────────────────────
:: [2] PyYAML available?
:: ──────────────────────────────────────────────
if not "!PYTHON!"=="" (
    echo  [CHECK] PyYAML availability...
    !PYTHON! -c "import yaml" >nul 2>&1
    if !ERRORLEVEL! NEQ 0 (
        echo         [WARN]  PyYAML not installed. Install: pip install pyyaml
        echo                 Server deploy uses: sudo apt install -y python3-yaml
        set /a WARN+=1
    ) else (
        echo         [OK]   PyYAML available
        set /a PASS+=1
    )
)

:: ──────────────────────────────────────────────
:: [3] docker-compose.prod.yml exists and valid?
:: ──────────────────────────────────────────────
echo  [CHECK] docker-compose.prod.yml...
if not exist "%COMPOSE_FILE%" (
    echo         [FAIL] Not found: %COMPOSE_FILE%
    set /a FAIL+=1
    goto :skip_compose_parse
)
echo         [OK]   File exists

if not "!PYTHON!"=="" (
    !PYTHON! -c "import yaml; yaml.safe_load(open(r'%COMPOSE_FILE%'))" >nul 2>&1
    if !ERRORLEVEL! NEQ 0 (
        echo         [FAIL] YAML parse error in docker-compose.prod.yml
        set /a FAIL+=1
    ) else (
        echo         [OK]   YAML is valid
        set /a PASS+=1
    )
)
:skip_compose_parse

:: ──────────────────────────────────────────────
:: [4] Discover services from compose
:: ──────────────────────────────────────────────
echo  [CHECK] Service discovery...
set SVC_COUNT=0
if not "!PYTHON!"=="" (
    for /f "tokens=1" %%S in ('!PYTHON! -c "import yaml; [print(s) for s in yaml.safe_load(open(r'%COMPOSE_FILE%'))['services']]" 2^>nul') do (
        set /a SVC_COUNT+=1
        set "SVC_!SVC_COUNT!=%%S"
    )
)
if %SVC_COUNT%==0 (
    echo         [FAIL] No services discovered ^(Python or PyYAML required^)
    set /a FAIL+=1
) else (
    set SVCS_FOUND=
    for /L %%i in (1,1,%SVC_COUNT%) do set "SVCS_FOUND=!SVCS_FOUND! !SVC_%%i!"
    echo         [OK]   Found %SVC_COUNT% service^(s^):%SVCS_FOUND%
    set /a PASS+=1
)

:: ──────────────────────────────────────────────
:: [5] Dockerfile per service?
:: ──────────────────────────────────────────────
echo  [CHECK] Dockerfiles...
set DF_OK=1
for /L %%i in (1,1,%SVC_COUNT%) do (
    set "S=!SVC_%%i!"
    if exist "%PROJECT_DIR%\!S!\Dockerfile" (
        echo         [OK]   !S!\Dockerfile
        set /a PASS+=1
    ) else (
        echo         [FAIL] Missing: !S!\Dockerfile
        set /a FAIL+=1
        set DF_OK=0
    )
)
if %SVC_COUNT%==0 echo         [SKIP] ^(no services discovered^)

:: ──────────────────────────────────────────────
:: [6] app.jar presence check (post-build)
:: ──────────────────────────────────────────────
echo  [CHECK] Built JARs...
set JAR_FOUND=0
set JAR_MISSING=0
for /L %%i in (1,1,%SVC_COUNT%) do (
    set "S=!SVC_%%i!"
    set "JAR="
    for %%J in ("%PROJECT_DIR%\!S!\build\libs\*-boot.jar") do set "JAR=%%J"
    if "!JAR!"=="" (
        for %%J in ("%PROJECT_DIR%\!S!\build\libs\*.jar") do (
            echo %%~nxJ | findstr /i "plain" >nul || set "JAR=%%J"
        )
    )
    if "!JAR!"=="" (
        echo         [WARN]  !S!\build\libs\ — no jar ^(run: gradlew :!S!:bootJar^)
        set /a WARN+=1
        set /a JAR_MISSING+=1
    ) else (
        for %%F in ("!JAR!") do set JAR_SIZE=%%~zF
        set /a JAR_KB=!JAR_SIZE!/1024
        echo         [OK]   !S! — !JAR_KB! KB
        set /a PASS+=1
        set /a JAR_FOUND+=1
    )
)
if %SVC_COUNT%==0 echo         [SKIP] ^(no services discovered^)

:: ──────────────────────────────────────────────
:: [7] Compose split simulation (dry-run)
:: ──────────────────────────────────────────────
echo  [CHECK] Compose split simulation...
if "!PYTHON!"=="" goto :skip_split
if %SVC_COUNT%==0 goto :skip_split

for /L %%i in (1,1,%SVC_COUNT%) do (
    set "S=!SVC_%%i!"
    !PYTHON! -c ^"^
import yaml, copy, sys; ^
src=yaml.safe_load(open(r'%COMPOSE_FILE%')); ^
svc=copy.deepcopy(src['services'].get('!S!')); ^
sys.exit(0 if svc else 1)^" >nul 2>&1
    if !ERRORLEVEL! EQU 0 (
        echo         [OK]   !S! — compose slice valid
        set /a PASS+=1
    ) else (
        echo         [FAIL] !S! — not found in compose services
        set /a FAIL+=1
    )
)
goto :after_split

:skip_split
if %SVC_COUNT%==0 ( echo         [SKIP] ^(no services discovered^)
) else ( echo         [SKIP] ^(Python/PyYAML required^) )

:after_split

:: ──────────────────────────────────────────────
:: [8] .env.prod or .env.example present?
:: ──────────────────────────────────────────────
echo  [CHECK] Environment files...
if exist "%PROJECT_DIR%\.env.prod" (
    echo         [OK]   .env.prod found
    set /a PASS+=1
) else if exist "%PROJECT_DIR%\.env.example" (
    echo         [WARN]  .env.prod missing ^(copy .env.example → .env.prod and fill values^)
    set /a WARN+=1
) else (
    echo         [FAIL] Neither .env.prod nor .env.example found
    set /a FAIL+=1
)

:: ──────────────────────────────────────────────
:: [9] Gradle wrapper?
:: ──────────────────────────────────────────────
echo  [CHECK] Gradle wrapper...
if exist "%PROJECT_DIR%\gradlew.bat" (
    echo         [OK]   gradlew.bat found
    set /a PASS+=1
) else (
    echo         [FAIL] gradlew.bat not found
    set /a FAIL+=1
)

:: ──────────────────────────────────────────────
:: [10] SSH connectivity (optional)
:: ──────────────────────────────────────────────
if %CHECK_SSH%==0 goto :skip_ssh
echo  [CHECK] SSH connectivity to %SERVER%...
ssh -o ConnectTimeout=5 -o BatchMode=yes %SERVER% "echo ok" >nul 2>&1
if !ERRORLEVEL! EQU 0 (
    echo         [OK]   SSH connection successful
    set /a PASS+=1
) else (
    echo         [FAIL] Cannot SSH to %SERVER% ^(check keys/VPN^)
    set /a FAIL+=1
)

echo  [CHECK] Python3 on server...
ssh -o ConnectTimeout=5 %SERVER% "python3 --version" >nul 2>&1
if !ERRORLEVEL! EQU 0 (
    echo         [OK]   python3 available on server
    set /a PASS+=1
) else (
    echo         [FAIL] python3 not found on server ^(run: sudo apt install -y python3-yaml^)
    set /a FAIL+=1
)

echo  [CHECK] PyYAML on server...
ssh -o ConnectTimeout=5 %SERVER% "python3 -c 'import yaml'" >nul 2>&1
if !ERRORLEVEL! EQU 0 (
    echo         [OK]   pyyaml available on server
    set /a PASS+=1
) else (
    echo         [FAIL] pyyaml not on server ^(run: sudo apt install -y python3-yaml^)
    set /a FAIL+=1
)
:skip_ssh

:: ──────────────────────────────────────────────
:: [11] Gradle build check (optional)
:: ──────────────────────────────────────────────
if %CHECK_BUILD%==0 goto :skip_build
echo  [CHECK] Gradle build ^(all services^)...
cd /d "%PROJECT_DIR%"
call gradlew.bat build -x test --no-daemon -q
if !ERRORLEVEL! EQU 0 (
    echo         [OK]   Build succeeded
    set /a PASS+=1
) else (
    echo         [FAIL] Build failed
    set /a FAIL+=1
)
:skip_build

:: ──────────────────────────────────────────────
::  Summary
:: ──────────────────────────────────────────────
echo.
echo  ==========================================
echo   Results: %PASS% passed  /  %WARN% warnings  /  %FAIL% failed
echo  ==========================================

if %FAIL% GTR 0 (
    echo.
    echo  [!] Fix failures before deploying.
    echo.
    if %CHECK_SSH%==0 echo   Tip: scripts\test.bat --ssh    ^(add server checks^)
    if %CHECK_BUILD%==0 echo   Tip: scripts\test.bat --build  ^(add build check^)
    exit /b 1
) else if %WARN% GTR 0 (
    echo.
    echo  [~] Warnings present — review before deploying.
    echo.
    exit /b 0
) else (
    echo.
    echo  [OK] All checks passed. Ready to deploy.
    echo.
    exit /b 0
)