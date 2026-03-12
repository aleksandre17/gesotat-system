#!/bin/bash
# ============================================================
#  Service Manager — SSH Remote
#
#  Runs on local machine (Git Bash / WSL).
#  All operations execute on the remote server via SSH.
#
#  Usage:
#    scripts/manage.sh                          interactive menu
#    scripts/manage.sh api stop                 stop api (backend)
#    scripts/manage.sh api stop frontend        stop api (frontend)
#    scripts/manage.sh all nuke -y              nuke all (no confirm)
#    scripts/manage.sh api logs errors          show error.log
#    scripts/manage.sh all logs files           list all log files
# ============================================================

# ══════════════════════════════════════════════
#  CONFIG — change these 3 lines per project
SERVER="administrator@192.168.1.199"
SERVER_BASE="/home/administrator"
PROJECT="geostat"
# ══════════════════════════════════════════════

# ── Parse TARGET from any arg ──
TARGET="backend"
for arg in "$@"; do
    case "$arg" in
        backend)  TARGET="backend"  ;;
        frontend) TARGET="frontend" ;;
    esac
done

REMOTE="$SERVER_BASE/$PROJECT/$TARGET"
COMPOSE="docker-compose -f docker-compose.prod.yml --env-file ../.env.prod"

# ── Discover services via SSH ──
mapfile -t SERVICES < <(
    ssh -n "$SERVER" \
        "python3 -c \"import os; [print(d) for d in sorted(os.listdir('$REMOTE')) if os.path.isfile('$REMOTE/'+d+'/docker-compose.prod.yml')]\" 2>/dev/null" \
    | tr -d '\r'
)
if [ ${#SERVICES[@]} -eq 0 ]; then
    echo "  ERROR: No services found at $REMOTE. Deploy first."
    exit 1
fi

# ── Parse positional args (skip target/flags) ──
SVC="" ACTION="" ARG3=""
pos=0
for arg in "$@"; do
    case "$arg" in
        backend|frontend|-y|--force) continue ;;
    esac
    pos=$((pos+1))
    case $pos in
        1) SVC="$arg"    ;;
        2) ACTION="$arg" ;;
        3) ARG3="$arg"   ;;
    esac
done

FORCE=""
for arg in "$@"; do
    case "$arg" in -y|--force) FORCE="YES" ;; esac
done

# ── Validate service ──
is_valid_service() {
    [ "$1" = "all" ] && return 0
    for s in "${SERVICES[@]}"; do [ "$s" = "$1" ] && return 0; done
    return 1
}

# ── Level 1: Service selection ──
if [ -z "$SVC" ]; then
    echo ""
    echo "  =========================================="
    echo "   Service Manager  [$PROJECT/$TARGET]"
    echo "  =========================================="
    echo ""
    echo "   Services:"
    for i in "${!SERVICES[@]}"; do
        echo "     $((i+1))) ${SERVICES[$i]}"
    done
    echo "     $((${#SERVICES[@]}+1))) all"
    echo ""
    read -rp "  Service [1-$((${#SERVICES[@]}+1))]: " choice
    if [ "$choice" = "$((${#SERVICES[@]}+1))" ]; then
        SVC="all"
    elif [ "$choice" -ge 1 ] 2>/dev/null && [ "$choice" -le "${#SERVICES[@]}" ]; then
        SVC="${SERVICES[$((choice-1))]}"
    else
        echo "  Invalid."; exit 1
    fi
fi

# ── Level 2: Action selection ──
if [ -z "$ACTION" ]; then
    echo ""
    echo "   Action for [$SVC]:"
    echo "     1) stop       5) status"
    echo "     2) start      6) rm"
    echo "     3) restart    7) nuke"
    echo "     4) logs       8) rebuild"
    echo ""
    read -rp "  Action [1-8]: " ac
    case $ac in
        1) ACTION="stop"    ;; 2) ACTION="start"   ;;
        3) ACTION="restart" ;; 4) ACTION="logs"    ;;
        5) ACTION="status"  ;; 6) ACTION="rm"      ;;
        7) ACTION="nuke"    ;; 8) ACTION="rebuild" ;;
        *) echo "  Invalid."; exit 1 ;;
    esac
fi

if ! is_valid_service "$SVC"; then
    echo "  ERROR: Unknown service '$SVC'"
    echo "  Available: ${SERVICES[*]} all"
    exit 1
fi

echo ""
echo "  [$SVC] $ACTION  ($PROJECT/$TARGET)"
echo "  ------------------------------------------"

# ── SSH helpers ──
ssh_svc()  { ssh "$SERVER" "cd $REMOTE/$SVC && $COMPOSE $*"; }
ssh_all()  {
    for s in "${SERVICES[@]}"; do
        ssh "$SERVER" "cd $REMOTE/$s && $COMPOSE $*"
    done
}

# ── Actions ──
case "$ACTION" in

    stop)
        if [ "$SVC" = "all" ]; then ssh_all stop
        else ssh_svc stop; fi
        echo "  [OK] Stopped."
        ;;

    start)
        if [ "$SVC" = "all" ]; then ssh_all up -d
        else ssh_svc up -d; fi
        echo "  [OK] Started."
        ;;

    restart)
        if [ "$SVC" = "all" ]; then ssh_all restart
        else ssh_svc restart; fi
        echo "  [OK] Restarted."
        ;;

    logs)
        if [ -z "$ARG3" ]; then
            echo ""
            echo "   Log source:"
            echo "     1) docker  — live container output"
            echo "     2) app     — app.log (all)"
            echo "     3) errors  — error.log"
            echo "     4) auth    — auth.log"
            echo "     5) db      — db.log"
            echo "     6) files   — list log files"
            echo ""
            read -rp "  Source [1-6]: " lc
            case "$lc" in
                1) ARG3="docker" ;; 2) ARG3="app"    ;;
                3) ARG3="errors" ;; 4) ARG3="auth"   ;;
                5) ARG3="db"     ;; 6) ARG3="files"  ;;
                *) echo "  Invalid."; exit 1 ;;
            esac

            if [[ "$ARG3" != "files" && "$ARG3" != "docker" ]]; then
                echo ""
                echo "   Level filter:"
                echo "     1) ALL"
                echo "     2) ERROR"
                echo "     3) WARN"
                echo "     4) INFO"
                echo ""
                read -rp "  Level [1-4, enter=ALL]: " lvl
                case "$lvl" in
                    2) LOG_LEVEL="ERROR" ;; 3) LOG_LEVEL="WARN" ;;
                    4) LOG_LEVEL="INFO"  ;; *) LOG_LEVEL=""      ;;
                esac
            fi
        fi

        # ── Build grep filter ──
        GREP_FILTER=""
        if [ -n "$LOG_LEVEL" ]; then
            GREP_FILTER="| grep \" $LOG_LEVEL \""
        fi

        case "$ARG3" in
            errors)
                if [ "$SVC" = "all" ]; then
                    for s in "${SERVICES[@]}"; do
                        ssh "$SERVER" "echo '=== $s ERRORS ===' && tail -100 $REMOTE/$s/logs/error.log 2>/dev/null $GREP_FILTER || echo '  (no error.log)'; echo"
                    done
                else
                    ssh "$SERVER" "tail -f $REMOTE/$SVC/logs/error.log $GREP_FILTER"
                fi
                ;;
            app)
                if [ "$SVC" = "all" ]; then
                    for s in "${SERVICES[@]}"; do
                        ssh "$SERVER" "echo '=== $s ===' && tail -100 $REMOTE/$s/logs/app.log 2>/dev/null $GREP_FILTER || echo '  (no app.log)'; echo"
                    done
                else
                    ssh "$SERVER" "tail -f $REMOTE/$SVC/logs/app.log $GREP_FILTER"
                fi
                ;;
            auth)
                local_svc="$SVC"
                [ "$SVC" = "all" ] && local_svc="${SERVICES[0]}"
                ssh "$SERVER" "tail -f $REMOTE/$local_svc/logs/auth.log $GREP_FILTER"
                ;;
            db)
                local_svc="$SVC"
                [ "$SVC" = "all" ] && local_svc="${SERVICES[0]}"
                ssh "$SERVER" "tail -f $REMOTE/$local_svc/logs/db.log $GREP_FILTER"
                ;;
            files)
                ssh "$SERVER" "find $REMOTE -name '*.log' -exec ls -lh {} \;"
                ;;
            *)
                if [ "$SVC" = "all" ]; then ssh_all logs --tail=50
                else ssh "$SERVER" "docker logs --tail=50 $PROJECT-$SVC"
                fi
                ;;
        esac
        ;;

    status)
        ssh "$SERVER" "docker ps --filter name=$PROJECT --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'; echo; docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}' \$(docker ps -q --filter name=$PROJECT) 2>/dev/null || true"
        ;;

    rm)
        if [ "$SVC" = "all" ]; then
            ssh_all down
        else
            ssh_svc stop
            ssh_svc rm -f
        fi
        echo "  [OK] Removed."
        ;;

    nuke)
        echo ""
        echo "  WARNING: Removes container(s), volumes, images, files and logs!"
        if [ "$FORCE" != "YES" ]; then
            read -rp "  Type YES to confirm: " confirm
            [ "$confirm" != "YES" ] && echo "  Cancelled." && exit 0
        fi
        if [ "$SVC" = "all" ]; then
            for s in "${SERVICES[@]}"; do
                ssh "$SERVER" "bash -c 'cd $REMOTE/$s && $COMPOSE down -v --rmi local 2>/dev/null; true'"
            done
            ssh "$SERVER" "docker image prune -f"
            for s in "${SERVICES[@]}"; do
                ssh "$SERVER" "docker run --rm -v $REMOTE/$s:/target alpine find /target -mindepth 1 -delete"
                ssh "$SERVER" "rm -rf $REMOTE/$s"
            done
        else
            ssh "$SERVER" "bash -c 'cd $REMOTE/$SVC && $COMPOSE down -v --rmi local 2>/dev/null; true'"
            ssh "$SERVER" "docker run --rm -v $REMOTE/$SVC:/target alpine find /target -mindepth 1 -delete"
            ssh "$SERVER" "rm -rf $REMOTE/$SVC"
        fi
        echo "  [OK] Nuked."
        ;;

    rebuild)
        if [ "$SVC" = "all" ]; then
            for s in "${SERVICES[@]}"; do
                ssh "$SERVER" "cd $REMOTE/$s && $COMPOSE down && $COMPOSE build --no-cache && $COMPOSE up -d"
            done
        else
            ssh_svc down
            ssh_svc build --no-cache
            ssh_svc up -d
        fi
        echo "  [OK] Rebuilt and started."
        ;;

    *)
        echo "  Unknown action: $ACTION"
        exit 1
        ;;
esac