@echo off
:: ============================================================
::  Service Manager — Portable & Dynamic
::
::  Edit CONFIG section to reuse in any project.
::  Services auto-discovered from docker-compose.prod.yml.
::
::  Usage:
::    scripts\manage.bat                          interactive menu (backend default)
::    scripts\manage.bat api stop                 stop api (backend)
::    scripts\manage.bat api stop frontend        stop api (frontend)
::    scripts\manage.bat all nuke -y              nuke all (no confirm)
::    scripts\manage.bat api logs errors          show error.log
::    scripts\manage.bat api logs auth            show auth.log
::    scripts\manage.bat all logs files           list all log files
:: ============================================================
setlocal enabledelayedexpansion

:: ══════════════════════════════════════════════
::  CONFIG — change these 3 lines per project
set SERVER=administrator@192.168.1.199
set SERVER_BASE=/home/administrator
set PROJECT=geostat
:: ══════════════════════════════════════════════

:: ── Target folder (backend / frontend / ...) ──
set TARGET=backend
for %%A in (%*) do (
    if "%%A"=="backend"  set TARGET=backend
    if "%%A"=="frontend" set TARGET=frontend
)

set REMOTE=%SERVER_BASE%/%PROJECT%/%TARGET%
set COMPOSE=docker-compose -f docker-compose.prod.yml --env-file .env.prod

:: ── Discover services from compose ──
set SVC_COUNT=0
for /f "tokens=1" %%S in ('ssh %SERVER% "cd %REMOTE% && %COMPOSE% config --services 2>/dev/null"') do (
    set /a SVC_COUNT+=1
    set "SVC_!SVC_COUNT!=%%S"
)
if %SVC_COUNT%==0 (
    echo  ERROR: No services found. Check SSH or compose file at %REMOTE%.
    exit /b 1
)

:: ── Parse positional args (skip target/flags) ──
set SVC=%~1
if "%SVC%"=="backend"  set SVC=
if "%SVC%"=="frontend" set SVC=
set ACTION=%~2
if "%ACTION%"=="backend"  set ACTION=
if "%ACTION%"=="frontend" set ACTION=
set ARG3=%~3
if "%ARG3%"=="backend"  set ARG3=
if "%ARG3%"=="frontend" set ARG3=

set FORCE=
for %%A in (%*) do (
    if "%%A"=="-y"      set FORCE=YES
    if "%%A"=="--force" set FORCE=YES
)

if "%SVC%"==""    goto :menu
if "%ACTION%"=="" goto :menu
goto :execute

:: ── Interactive Menu ──
:menu
echo.
echo  ==========================================
echo   Service Manager  [%PROJECT%/%TARGET%]
echo  ==========================================
echo.
echo   Services:
for /L %%i in (1,1,%SVC_COUNT%) do echo     %%i^) !SVC_%%i!
set /a ALLIDX=%SVC_COUNT%+1
echo     %ALLIDX%^) all
echo.
set /p CHOICE="  Service [1-%ALLIDX%]: "
if "%CHOICE%"=="%ALLIDX%" ( set SVC=all ) else ( set SVC=!SVC_%CHOICE%! )
if "%SVC%"=="" echo  Invalid. & goto :menu

echo.
echo   Action for [%SVC%]:
echo     1) stop       5) status
echo     2) start      6) rm
echo     3) restart    7) nuke
echo     4) logs       8) rebuild
echo.
set /p AC="  Action [1-8]: "
if "%AC%"=="1" set ACTION=stop
if "%AC%"=="2" set ACTION=start
if "%AC%"=="3" set ACTION=restart
if "%AC%"=="4" set ACTION=logs
if "%AC%"=="5" set ACTION=status
if "%AC%"=="6" set ACTION=rm
if "%AC%"=="7" set ACTION=nuke
if "%AC%"=="8" set ACTION=rebuild
if "%ACTION%"=="" echo  Invalid. & goto :menu

:: ── Execute ──
:execute
echo.
echo  [%SVC%] %ACTION%  (%PROJECT%/%TARGET%)
echo  ------------------------------------------

if "%ACTION%"=="stop"    goto :do_stop
if "%ACTION%"=="start"   goto :do_start
if "%ACTION%"=="restart" goto :do_restart
if "%ACTION%"=="logs"    goto :do_logs
if "%ACTION%"=="status"  goto :do_status
if "%ACTION%"=="rm"      goto :do_rm
if "%ACTION%"=="nuke"    goto :do_nuke
if "%ACTION%"=="rebuild" goto :do_rebuild
echo  Unknown action: %ACTION%
goto :eof

:do_stop
if "%SVC%"=="all" ( ssh %SERVER% "cd %REMOTE% && %COMPOSE% stop"
) else ( ssh %SERVER% "cd %REMOTE% && %COMPOSE% stop %SVC%" )
echo  [OK] Stopped.
goto :eof

:do_start
if "%SVC%"=="all" ( ssh %SERVER% "cd %REMOTE% && %COMPOSE% up -d"
) else ( ssh %SERVER% "cd %REMOTE% && %COMPOSE% up -d %SVC%" )
echo  [OK] Started.
goto :eof

:do_restart
if "%SVC%"=="all" ( ssh %SERVER% "cd %REMOTE% && %COMPOSE% restart"
) else ( ssh %SERVER% "cd %REMOTE% && %COMPOSE% restart %SVC%" )
echo  [OK] Restarted.
goto :eof

:do_logs
if "%ARG3%"=="errors" (
    if "%SVC%"=="all" (
        for /L %%i in (1,1,%SVC_COUNT%) do ssh %SERVER% "echo === !SVC_%%i! ERRORS === && tail -100 %REMOTE%/logs/!SVC_%%i!/error.log 2>/dev/null || echo  (no error.log); echo"
    ) else (
        ssh %SERVER% "tail -f %REMOTE%/logs/%SVC%/error.log"
    )
    goto :eof
)
if "%ARG3%"=="auth" (
    if "%SVC%"=="all" ( ssh %SERVER% "tail -f %REMOTE%/logs/!SVC_1!/auth.log"
    ) else ( ssh %SERVER% "tail -f %REMOTE%/logs/%SVC%/auth.log" )
    goto :eof
)
if "%ARG3%"=="db" (
    if "%SVC%"=="all" ( ssh %SERVER% "tail -f %REMOTE%/logs/!SVC_1!/db.log"
    ) else ( ssh %SERVER% "tail -f %REMOTE%/logs/%SVC%/db.log" )
    goto :eof
)
if "%ARG3%"=="files" (
    ssh %SERVER% "find %REMOTE%/logs -name '*.log' -exec ls -lh {} \;"
    goto :eof
)
:: Default: docker logs
if "%SVC%"=="all" ( ssh %SERVER% "cd %REMOTE% && %COMPOSE% logs --tail=50"
) else ( ssh %SERVER% "docker logs --tail=50 %PROJECT%-%SVC%" )
goto :eof

:do_status
ssh %SERVER% "docker ps --filter name=%PROJECT% --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'; echo; docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}' $(docker ps -q --filter name=%PROJECT%) 2>/dev/null"
goto :eof

:do_rm
if "%SVC%"=="all" ( ssh %SERVER% "cd %REMOTE% && %COMPOSE% down"
) else ( ssh %SERVER% "cd %REMOTE% && %COMPOSE% stop %SVC% && %COMPOSE% rm -f %SVC%" )
echo  [OK] Removed.
goto :eof

:do_nuke
echo.
echo  WARNING: Removes container(s), volumes, images, files and logs!
if not "%FORCE%"=="YES" (
    set /p CONFIRM="  Type YES to confirm: "
    if not "!CONFIRM!"=="YES" echo  Cancelled. & goto :eof
)
if "%SVC%"=="all" (
    ssh %SERVER% "cd %REMOTE% && %COMPOSE% down -v --rmi local && docker image prune -f"
    set "FOLDERS="
    for /L %%i in (1,1,%SVC_COUNT%) do set "FOLDERS=!FOLDERS! !SVC_%%i!/"
    ssh %SERVER% "cd %REMOTE% && rm -rf !FOLDERS! && docker run --rm -v %REMOTE%/logs:/logs alpine rm -rf /logs 2>/dev/null; true"
) else (
    ssh %SERVER% "cd %REMOTE% && %COMPOSE% stop %SVC% && %COMPOSE% rm -f %SVC% && docker volume ls -q --filter name=%PROJECT%_%SVC% | xargs -r docker volume rm 2>/dev/null; docker rmi %PROJECT%-%SVC% 2>/dev/null; rm -rf %SVC%/ && docker run --rm -v %REMOTE%/logs:/logs alpine rm -rf /logs/%SVC% 2>/dev/null; true"
)
echo  [OK] Nuked.
goto :eof

:do_rebuild
if "%SVC%"=="all" (
    ssh %SERVER% "cd %REMOTE% && %COMPOSE% down && %COMPOSE% build --no-cache && %COMPOSE% up -d"
) else (
    ssh %SERVER% "cd %REMOTE% && %COMPOSE% stop %SVC% && %COMPOSE% rm -f %SVC% && %COMPOSE% build --no-cache %SVC% && %COMPOSE% up -d %SVC%"
)
echo  [OK] Rebuilt and started.
goto :eof
