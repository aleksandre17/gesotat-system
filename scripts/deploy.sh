#!/bin/bash
# ============================================================
#  Service Deployer — Professional SSH Remote
#
#  Usage:
#    scripts/deploy.sh                      interactive menu
#    scripts/deploy.sh api                  deploy api only
#    scripts/deploy.sh all                  deploy all services
#    scripts/deploy.sh api --no-build       skip gradle build
#    scripts/deploy.sh api --dev            dev environment
#    scripts/deploy.sh all --prod           prod, all services
#    scripts/deploy.sh api --skip-checks    bypass pre-flight
#
#  Server logs (per service):
#    /home/administrator/geostat/backend/<svc>/logs/build.log
#    /home/administrator/geostat/backend/<svc>/logs/upload.log
#    /home/administrator/geostat/backend/<svc>/logs/compose.log
#    /home/administrator/geostat/backend/<svc>/logs/deploy.log
#
#  Prerequisites (server, one-time):
#    sudo apt install -y python3-yaml
# ============================================================

# ══════════════════════════════════════════════
#  CONFIG
SERVER="administrator@192.168.1.199"
SERVER_BASE="/home/administrator"
PROJECT="geostat"
VERSIONS_KEEP=5          # versioned jars to keep per service on server
HEALTH_RETRIES=24        # × 10s = 4 min max wait (covers start_period: 90s + retries)
# ══════════════════════════════════════════════

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
CRED_PATTERNS="*.p12 *.jks *.keystore *.pem *.crt *credentials*.json *service-account*.json"

# ── Parse args ──
TARGET="backend"
ENVIRONMENT="prod"
SERVICE=""
SKIP_BUILD=0
SKIP_CHECKS=0

for arg in "$@"; do
    case "$arg" in
        --no-build)    SKIP_BUILD=1       ;;
        --skip-checks) SKIP_CHECKS=1      ;;
        --dev)         ENVIRONMENT="dev"  ;;
        --prod)        ENVIRONMENT="prod" ;;
        all)           SERVICE="all"      ;;
        backend)       TARGET="backend"   ;;
        frontend)      TARGET="frontend"  ;;
        *)             SERVICE="$arg"     ;;
    esac
done

COMPOSE_FILE="docker-compose.${ENVIRONMENT}.yml"
ENV_FILE=".env.${ENVIRONMENT}"
REMOTE="$SERVER_BASE/$PROJECT/$TARGET"

cd "$PROJECT_DIR" || exit 1

# ── Discover services from local compose file ──
mapfile -t SERVICES < <(
    awk '/^services:/{f=1;next} f && /^[^ ]/{f=0} f && /^  [a-zA-Z0-9_-]+:/{gsub(/[ :]/,""); print}' \
    "$PROJECT_DIR/$COMPOSE_FILE" 2>/dev/null
)
if [ ${#SERVICES[@]} -eq 0 ]; then
    echo "  ERROR: No services found in $COMPOSE_FILE"
    exit 1
fi

# ════════════════════════════════════════════════
#  LEVEL 1: Service selection
# ════════════════════════════════════════════════
if [ -z "$SERVICE" ]; then
    echo ""
    echo "  =========================================="
    echo "   Deployer  [$PROJECT/$TARGET] [$ENVIRONMENT]"
    echo "  =========================================="
    echo ""
    echo "   Service:"
    for i in "${!SERVICES[@]}"; do
        echo "     $((i+1))) ${SERVICES[$i]}"
    done
    echo "     $((${#SERVICES[@]}+1))) all"
    echo ""
    read -rp "  Service [1-$((${#SERVICES[@]}+1))]: " choice
    if [ "$choice" = "$((${#SERVICES[@]}+1))" ]; then
        SERVICE="all"
    elif [[ "$choice" -ge 1 && "$choice" -le "${#SERVICES[@]}" ]] 2>/dev/null; then
        SERVICE="${SERVICES[$((choice-1))]}"
    else
        echo "  Invalid."; exit 1
    fi
fi

[ -z "$SERVICE" ] && SERVICE="all"

# ════════════════════════════════════════════════
#  LEVEL 2: Build options (only in interactive mode)
# ════════════════════════════════════════════════
if [ "$SKIP_BUILD" = "0" ] && [ $# -eq 0 ]; then
    echo ""
    echo "   Build:"
    echo "     1) yes  — gradle build + deploy"
    echo "     2) no   — upload existing jar (--no-build)"
    echo ""
    read -rp "  Build [1-2]: " bc
    case "$bc" in 2) SKIP_BUILD=1 ;; esac
fi

# ════════════════════════════════════════════════
#  Optional module includes (findProject in build.gradle)
# ════════════════════════════════════════════════
GRADLE_PROPS=""
if [ "$SKIP_BUILD" = "0" ]; then
    if [ "$SERVICE" = "all" ]; then
        active_modules=$(IFS=','; echo "${SERVICES[*]}")
    else
        active_modules="$SERVICE"
    fi

    mapfile -t gradle_files < <(find "$PROJECT_DIR" -maxdepth 2 -name "build.gradle" ! -path "*/build/*")
    for gradle_file in "${gradle_files[@]}"; do
        svc_name=$(basename "$(dirname "$gradle_file")")
        [ "$SERVICE" != "all" ] && [ "$SERVICE" != "$svc_name" ] && continue
        mapfile -t opts < <(grep -oE "findProject\(':([^']+)'\)" "$gradle_file" | grep -oE "':[^']+'" | tr -d "':")
        for opt in "${opts[@]}"; do
            read -rp "   Include :$opt in $svc_name? [Y/n]: " inc
            if [[ "$inc" =~ ^[Nn] ]]; then
                active_modules=$(echo "$active_modules" | tr ',' '\n' | grep -v "^${opt}$" | tr '\n' ',' | sed 's/,$//')
                echo "     → skipped"
            else
                echo "     → included"
            fi
        done
    done
    GRADLE_PROPS="-PactiveModules=$active_modules"
fi

# ════════════════════════════════════════════════
#  Pre-flight checks
# ════════════════════════════════════════════════
if [ "$SKIP_CHECKS" = "0" ]; then
    CHECK_ARGS="$SERVICE $TARGET"
    [ "$SKIP_BUILD" = "1" ] && CHECK_ARGS="$CHECK_ARGS --no-build"
    bash "$(dirname "$0")/check.sh" $CHECK_ARGS || exit 1
fi

# ════════════════════════════════════════════════
#  Version tag (for jar snapshots only — logs go to server)
# ════════════════════════════════════════════════
DEPLOY_VERSION="$(date +%Y%m%d-%H%M%S)"

log() { echo "$1"; }

echo ""
echo "  =========================================="
echo "   Deploy [$SERVICE] → $PROJECT/$TARGET [$ENVIRONMENT]"
echo "  =========================================="
echo ""

# ════════════════════════════════════════════════
#  STEP 1: Build
# ════════════════════════════════════════════════
if [ "$SKIP_BUILD" = "1" ]; then
    log "  [1/5] Build skipped"
else
    log "  [1/5] Building..."
    for s in "${SERVICES[@]}"; do
        [ "$SERVICE" != "all" ] && [ "$SERVICE" != "$s" ] && continue
        # Build locally — log saved to temp, uploaded to server in step 3
        local_build_log="/tmp/geostat-build-$s.log"
        {
            echo "=== Build [$s] $(date) ==="
        } > "$local_build_log"

        # shellcheck disable=SC2086
        ./gradlew ":$s:bootJar" -x test --no-daemon $GRADLE_PROPS 2>&1 | tee -a "$local_build_log"
        if [ "${PIPESTATUS[0]}" -ne 0 ]; then
            log "  [FAIL] Build failed: $s"
            exit 1
        fi
        log "  [OK]   Build: $s"
    done
fi

# ════════════════════════════════════════════════
#  STEP 2: Prepare jars
# ════════════════════════════════════════════════
log "  [2/5] Preparing jars..."
ssh -n "$SERVER" "mkdir -p $REMOTE"

for s in "${SERVICES[@]}"; do
    [ "$SERVICE" != "all" ] && [ "$SERVICE" != "$s" ] && continue
    jar=$(find "$PROJECT_DIR/$s/build/libs/" -name "*-boot.jar" 2>/dev/null | head -1)
    [ -z "$jar" ] && jar=$(find "$PROJECT_DIR/$s/build/libs/" -name "*.jar" 2>/dev/null | grep -iv plain | head -1)
    if [ -z "$jar" ]; then
        log "  [WARN] No jar for $s — skipping"
        continue
    fi
    cp "$jar" "$PROJECT_DIR/$s/app.jar"
    size=$(du -h "$jar" | cut -f1)
    log "  [OK]   $s/app.jar  ($size)"
done

# ════════════════════════════════════════════════
#  STEP 3: Upload
# ════════════════════════════════════════════════
log "  [3/5] Uploading..."

upload_service() {
    local s="$1"
    local srv_log="$REMOTE/$s/logs"

    ssh -n "$SERVER" "mkdir -p $REMOTE/$s/logs $REMOTE/$s/versions"

    {
        echo "=== Upload [$s] $(date) ==="
        scp "$PROJECT_DIR/$s/Dockerfile" "$SERVER:$REMOTE/$s/" 2>&1
        scp "$PROJECT_DIR/$s/app.jar"    "$SERVER:$REMOTE/$s/" 2>&1
        # env file in service dir (for env_file: reference in compose)
        scp "$PROJECT_DIR/$ENV_FILE" "$SERVER:$REMOTE/$s/$ENV_FILE" 2>&1
    } | ssh "$SERVER" "cat >> $srv_log/upload.log"

    # Upload build log (if it exists from step 1)
    if [ -f "/tmp/geostat-build-$s.log" ]; then
        scp "/tmp/geostat-build-$s.log" "$SERVER:$srv_log/build.log" >/dev/null 2>&1
        rm -f "/tmp/geostat-build-$s.log"
    fi

    # Version snapshot (prod only)
    if [ "$ENVIRONMENT" = "prod" ]; then
        ssh -n "$SERVER" "
            cp $REMOTE/$s/app.jar $REMOTE/$s/versions/app-$DEPLOY_VERSION.jar
            ls -t $REMOTE/$s/versions/app-*.jar 2>/dev/null | tail -n +$((VERSIONS_KEEP+1)) | xargs rm -f
        "
    fi

    # Credential files
    for pattern in $CRED_PATTERNS; do
        for f in "$PROJECT_DIR/$s/"$pattern; do
            [ -f "$f" ] || continue
            scp "$f" "$SERVER:$REMOTE/$s/" 2>/dev/null
            echo "  [cred] $(basename "$f")"
        done
    done

    log "  [OK]   $s uploaded  (version: $DEPLOY_VERSION)"
}

for s in "${SERVICES[@]}"; do
    [ "$SERVICE" != "all" ] && [ "$SERVICE" != "$s" ] && continue
    upload_service "$s"
done

# ════════════════════════════════════════════════
#  STEP 4: Generate per-service compose on server
# ════════════════════════════════════════════════
log "  [4/5] Generating compose files..."

scp "$PROJECT_DIR/$COMPOSE_FILE" "$SERVER:/tmp/compose-src.yml" >/dev/null 2>&1
scp "$PROJECT_DIR/$ENV_FILE"     "$SERVER:$REMOTE/"             >/dev/null 2>&1

gen_compose() {
    local s="$1"
    ssh "$SERVER" "python3 - 2>&1 | tee $REMOTE/$s/logs/compose.log" <<PYEOF
import yaml, copy
with open('/tmp/compose-src.yml') as f:
    src = yaml.safe_load(f)
svc_name = '$s'
svc_cfg = copy.deepcopy(src['services'][svc_name])
if 'build' in svc_cfg:
    svc_cfg['build']['context'] = '.'
named_vols = {}
if 'volumes' in src:
    for v in svc_cfg.get('volumes', []):
        vol_name = v.split(':')[0] if ':' in v else v
        if not vol_name.startswith('.') and not vol_name.startswith('/'):
            if vol_name in src['volumes']:
                named_vols[vol_name] = src['volumes'][vol_name] or {}
out = {'services': {svc_name: svc_cfg}}
if named_vols:
    out['volumes'] = named_vols
if 'networks' in svc_cfg:
    nets = svc_cfg['networks'] if isinstance(svc_cfg['networks'], list) else list(svc_cfg['networks'].keys())
    out['networks'] = {n: {'external': True} for n in nets}
with open('$REMOTE/$s/docker-compose.${ENVIRONMENT}.yml', 'w') as f:
    yaml.dump(out, f, default_flow_style=False, allow_unicode=True)
print('  [OK] $s/docker-compose.${ENVIRONMENT}.yml')
PYEOF
}

for s in "${SERVICES[@]}"; do
    [ "$SERVICE" != "all" ] && [ "$SERVICE" != "$s" ] && continue
    gen_compose "$s"
done

# Auto-create shared networks
ssh "$SERVER" "python3 -" <<PYEOF >/dev/null 2>&1
import yaml, subprocess
with open('/tmp/compose-src.yml') as f:
    src = yaml.safe_load(f)
for n in src.get('networks', {}).keys():
    r = subprocess.run(['docker', 'network', 'create', n], capture_output=True)
    print(f'  [net] {"created" if r.returncode==0 else "exists"}: {n}')
PYEOF
ssh -n "$SERVER" "rm -f /tmp/compose-src.yml"
log "  [OK]   Compose files ready"

# ════════════════════════════════════════════════
#  STEP 5: Docker up + Health check + Rollback
# ════════════════════════════════════════════════
log "  [5/5] Starting containers..."

# Container start selection (multi-service)
TO_START=()
for s in "${SERVICES[@]}"; do
    [ "$SERVICE" != "all" ] && [ "$SERVICE" != "$s" ] && continue
    TO_START+=("$s")
done

if [ "${#TO_START[@]}" -gt 1 ]; then
    echo ""
    echo "   Which containers to start?"
    for i in "${!TO_START[@]}"; do
        echo "     $((i+1))) ${TO_START[$i]}"
    done
    echo "     $((${#TO_START[@]}+1))) all"
    echo "     0) none (start manually later)"
    echo ""
    read -rp "  Start [0-$((${#TO_START[@]}+1))]: " sc
    if [ "$sc" = "0" ]; then
        TO_START=()
    elif [ "$sc" != "$((${#TO_START[@]}+1))" ]; then
        if [[ "$sc" -ge 1 && "$sc" -le "${#TO_START[@]}" ]] 2>/dev/null; then
            TO_START=("${TO_START[$((sc-1))]}")
        else
            echo "  Invalid."; exit 1
        fi
    fi
fi

docker_up() {
    local s="$1"
    local srv_log="$REMOTE/$s/logs"
    local container="${PROJECT}-${s}"

    # Pre-create logs dir as deploy user so Docker bind-mount doesn't create it as root
    ssh -n "$SERVER" "mkdir -p $REMOTE/$s/logs"

    ssh "$SERVER" "
        echo '=== Deploy [$s] \$(date) ===' >> $srv_log/deploy.log
        cd $REMOTE/$s
        docker-compose -f docker-compose.${ENVIRONMENT}.yml --env-file ../$ENV_FILE up --build -d 2>&1 | tee -a $srv_log/deploy.log
    "

    # ── Health check ──
    log "  ... waiting for $s to become healthy"
    local healthy=0
    for i in $(seq 1 "$HEALTH_RETRIES"); do
        local status
        status=$(ssh -n "$SERVER" \
            "docker inspect --format='{{.State.Health.Status}}' $container 2>/dev/null || echo no-healthcheck")
        case "$status" in
            healthy)
                log "  [OK]   $s healthy  ✓"
                healthy=1
                break
                ;;
            unhealthy)
                log "  [FAIL] $s unhealthy after $((i*10))s"
                break
                ;;
            no-healthcheck)
                local running
                running=$(ssh -n "$SERVER" \
                    "docker inspect --format='{{.State.Running}}' $container 2>/dev/null")
                if [ "$running" = "true" ]; then
                    log "  [OK]   $s running (no healthcheck defined)"
                    healthy=1
                else
                    log "  [FAIL] $s container is not running"
                fi
                break
                ;;
            *)
                echo "  ... $s starting ($((i*10))s / $((HEALTH_RETRIES*10))s)"
                sleep 10
                ;;
        esac
    done

    # ── Rollback (prod only) ──
    if [ "$healthy" = "0" ] && [ "$ENVIRONMENT" = "prod" ]; then
        local prev_jar
        prev_jar=$(ssh -n "$SERVER" \
            "ls -t $REMOTE/$s/versions/app-*.jar 2>/dev/null | sed -n '2p'")
        if [ -n "$prev_jar" ]; then
            log "  [ROLLBACK] $s → restoring $(basename "$prev_jar")..."
            ssh "$SERVER" "
                set -e
                cp $prev_jar $REMOTE/$s/app.jar
                cd $REMOTE/$s
                docker-compose -f docker-compose.${ENVIRONMENT}.yml --env-file ../$ENV_FILE up --build -d 2>&1 | tee -a $srv_log/deploy.log
            "
            log "  [ROLLBACK] $s restored"
        else
            log "  [WARN] $s — no previous version to roll back to"
        fi
    fi

    # Show running container
    ssh -n "$SERVER" \
        "docker ps --filter name=$container --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'"

    # ── Write info.log to server ──
    ssh -n "$SERVER" "
        f='$REMOTE/$s/info.log'
        ctr='$container'
        rp='$REMOTE/$s'

        c_status=\$(docker inspect --format='{{.State.Status}}'      \"\$ctr\" 2>/dev/null || echo unknown)
        c_health=\$(docker inspect --format='{{.State.Health.Status}}' \"\$ctr\" 2>/dev/null)
        [ -z \"\$c_health\" ] && c_health='no healthcheck'
        c_image=\$(docker inspect  --format='{{.Config.Image}}'      \"\$ctr\" 2>/dev/null || echo unknown)
        c_started=\$(docker inspect --format='{{.State.StartedAt}}'  \"\$ctr\" 2>/dev/null || echo unknown)
        c_ports=\$(docker port \"\$ctr\" 2>/dev/null | paste -sd '  ' || echo none)
        java_ver=\$(docker exec \"\$ctr\" java -version 2>&1 | head -1 || echo unknown)
        spring_profile=\$(docker inspect --format='{{range .Config.Env}}{{println .}}{{end}}' \"\$ctr\" 2>/dev/null | grep ^SPRING_PROFILES_ACTIVE= | cut -d= -f2)
        server_port=\$(docker inspect --format='{{range .Config.Env}}{{println .}}{{end}}' \"\$ctr\" 2>/dev/null | grep ^SERVER_PORT= | cut -d= -f2)
        [ -z \"\$server_port\" ] && server_port=\$(docker inspect --format='{{range \$p,\$b := .NetworkSettings.Ports}}{{(index \$b 0).HostPort}}{{end}}' \"\$ctr\" 2>/dev/null)
        [ -z \"\$spring_profile\" ] && spring_profile=unknown
        [ -z \"\$server_port\" ]    && server_port=unknown
        jar_size=\$(du -h \"\$rp/app.jar\" 2>/dev/null | cut -f1 || echo unknown)
        srv_host=\$(hostname)
        srv_ip=\$(hostname -I | awk '{print \$1}')

        {
        echo '=========================================='
        echo '  Application Info — $s'
        echo '=========================================='
        echo \"  Service          : $s\"
        echo \"  Environment      : $ENVIRONMENT\"
        echo \"  Spring profile   : \$spring_profile\"
        echo \"  Deploy version   : $DEPLOY_VERSION\"
        echo \"  Deploy time      : \$(date)\"
        echo '------------------------------------------'
        echo \"  Server host      : \$srv_host (\$srv_ip)\"
        echo \"  Remote path      : \$rp\"
        echo \"  Deploy user      : \$(whoami)\"
        echo '------------------------------------------'
        echo \"  Container        : \$ctr\"
        echo \"  Status           : \$c_status\"
        echo \"  Health           : \$c_health\"
        echo \"  Started at       : \$c_started\"
        echo \"  Image            : \$c_image\"
        echo \"  Ports            : \$c_ports\"
        echo \"  Server port      : \$server_port\"
        echo '------------------------------------------'
        echo \"  Jar size         : \$jar_size\"
        echo \"  Java             : \$java_ver\"
        echo \"  App logs         : $REMOTE/$s/logs/\"
        echo '=========================================='
        } > \"\$f\"
    "
}

for s in "${TO_START[@]}"; do
    docker_up "$s"
done

echo ""
echo "  =========================================="
echo "   DONE [$SERVICE] [$ENVIRONMENT]"
echo "   Server logs: $REMOTE/<svc>/logs/"
echo "   Manage:      scripts/manage.sh all status"
echo "  =========================================="