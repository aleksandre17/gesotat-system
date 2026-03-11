#!/bin/bash
# ============================================================
#  GeoStat System — Service Manager (Dynamic)
#
#  Discovers services from docker-compose.prod.yml automatically.
#  New services appear in menu without code changes.
#
#  Usage:
#    ./manage.sh                          interactive menu (backend default)
#    ./manage.sh api stop                 stop api (backend)
#    ./manage.sh api stop frontend        stop api (frontend)
#    ./manage.sh all nuke -y              remove everything (no confirm)
#    ./manage.sh api logs errors backend  show error.log (backend)
# ============================================================

SERVER_BASE="/home/administrator"

# ── Parse TARGET (backend/frontend) from any arg ──
TARGET="backend"
for arg in "$@"; do
    case "$arg" in
        backend)  TARGET="backend"  ;;
        frontend) TARGET="frontend" ;;
    esac
done

REMOTE="$SERVER_BASE/geostat/$TARGET"
COMPOSE="docker-compose -f $REMOTE/docker-compose.prod.yml --env-file $REMOTE/.env.prod"
LOGS_DIR="$REMOTE/logs"

# ── Discover services dynamically ──
mapfile -t SERVICES < <($COMPOSE config --services 2>/dev/null)
if [ ${#SERVICES[@]} -eq 0 ]; then
    echo "  ERROR: No services found. Check connection or compose file."
    exit 1
fi

# ── Parse positional args (skip backend/frontend) ──
SVC=""
ACTION=""
ARG3=""
pos=0
for arg in "$@"; do
    case "$arg" in
        backend|frontend|-y|--force|YES) continue ;;
    esac
    pos=$((pos+1))
    case $pos in
        1) SVC="$arg"    ;;
        2) ACTION="$arg" ;;
        3) ARG3="$arg"   ;;
    esac
done

# Parse force flag
FORCE=""
for arg in "$@"; do
    case "$arg" in -y|--force|YES) FORCE="YES" ;; esac
done

# ── Validate service name ──
is_valid_service() {
    [ "$1" = "all" ] && return 0
    for s in "${SERVICES[@]}"; do [ "$s" = "$1" ] && return 0; done
    return 1
}

# ── Interactive menu ──
if [ -z "$SVC" ] || [ -z "$ACTION" ]; then
    echo ""
    echo "  =========================================="
    echo "   GeoStat Service Manager [$TARGET]"
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

# ── Validate ──
if ! is_valid_service "$SVC"; then
    echo "  ERROR: Unknown service '$SVC'"
    echo "  Available: ${SERVICES[*]} all"
    exit 1
fi

echo ""
echo "  [$SVC] $ACTION  ($TARGET)"
echo "  ------------------------------------------"

# ── Helper: run compose for one or all ──
compose_cmd() {
    if [ "$SVC" = "all" ]; then
        $COMPOSE "$@"
    else
        $COMPOSE "$@" "$SVC"
    fi
}

case "$ACTION" in

    stop)
        compose_cmd stop
        echo "  [OK] Stopped."
        ;;

    start)
        compose_cmd up -d
        echo "  [OK] Started."
        ;;

    restart)
        compose_cmd restart
        echo "  [OK] Restarted."
        ;;

    logs)
        case "$ARG3" in
            errors)
                if [ "$SVC" = "all" ]; then
                    for s in "${SERVICES[@]}"; do
                        echo "=== $s ERRORS ===" && tail -100 "$LOGS_DIR/$s/error.log" 2>/dev/null || echo "  (no error.log)"
                        echo ""
                    done
                else
                    tail -f "$LOGS_DIR/$SVC/error.log"
                fi
                ;;
            auth)
                [ "$SVC" = "all" ] && SVC="${SERVICES[0]}"
                tail -f "$LOGS_DIR/$SVC/auth.log"
                ;;
            db)
                [ "$SVC" = "all" ] && SVC="${SERVICES[0]}"
                tail -f "$LOGS_DIR/$SVC/db.log"
                ;;
            files)
                find "$LOGS_DIR" -name '*.log' -exec ls -lh {} \;
                ;;
            *)
                if [ "$SVC" = "all" ]; then
                    $COMPOSE logs --tail=50
                else
                    docker logs --tail=50 "geostat-$SVC"
                fi
                ;;
        esac
        ;;

    status)
        docker ps --filter name=geostat --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
        echo ""
        docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}' $(docker ps -q --filter name=geostat) 2>/dev/null || true
        ;;

    rm)
        if [ "$SVC" = "all" ]; then
            $COMPOSE down
        else
            $COMPOSE stop "$SVC" && $COMPOSE rm -f "$SVC"
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
            $COMPOSE down -v --rmi local
            docker image prune -f
            for s in "${SERVICES[@]}"; do rm -rf "$REMOTE/$s"/; done
            docker run --rm -v "$LOGS_DIR:/logs" alpine rm -rf /logs 2>/dev/null || true
        else
            $COMPOSE stop "$SVC" && $COMPOSE rm -f "$SVC"
            docker volume ls -q --filter "name=geostat_${SVC}" | xargs -r docker volume rm 2>/dev/null || true
            docker rmi "geostat-$SVC" 2>/dev/null || true
            rm -rf "$REMOTE/$SVC"/
            docker run --rm -v "$LOGS_DIR:/logs" alpine rm -rf "/logs/$SVC" 2>/dev/null || true
        fi
        echo "  [OK] Nuked."
        ;;

    rebuild)
        if [ "$SVC" = "all" ]; then
            $COMPOSE down && $COMPOSE build --no-cache && $COMPOSE up -d
        else
            $COMPOSE stop "$SVC" && $COMPOSE rm -f "$SVC"
            $COMPOSE build --no-cache "$SVC" && $COMPOSE up -d "$SVC"
        fi
        echo "  [OK] Rebuilt and started."
        ;;

    *)
        echo "  Unknown action: $ACTION"
        echo "  Available: stop start restart logs status rm nuke rebuild"
        exit 1
        ;;
esac