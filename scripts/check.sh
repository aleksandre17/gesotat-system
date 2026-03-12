#!/bin/bash
# ============================================================
#  Pre-flight Checks — validates all prerequisites before deploy
#
#  Usage:
#    scripts/check.sh                        check all services
#    scripts/check.sh api                    check api only
#    scripts/check.sh api frontend           check api on frontend target
#    scripts/check.sh --no-build             skip build-related checks
#
#  Exit codes:
#    0 — all checks passed (warnings allowed)
#    1 — one or more errors found
# ============================================================

# ══════════════════════════════════════════════
#  CONFIG — must match deploy.sh
SERVER="administrator@192.168.1.199"
SERVER_BASE="/home/administrator"
PROJECT="geostat"
# ══════════════════════════════════════════════

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ERRORS=0
WARNINGS=0

# ── Parse args ──
SERVICE="all"
TARGET="backend"
SKIP_BUILD=0
for arg in "$@"; do
    case "$arg" in
        --no-build) SKIP_BUILD=1      ;;
        all)        SERVICE="all"     ;;
        backend)    TARGET="backend"  ;;
        frontend)   TARGET="frontend" ;;
        *)          SERVICE="$arg"    ;;
    esac
done

REMOTE="$SERVER_BASE/$PROJECT/$TARGET"

# ── Helpers ──
ok()   { printf "    \033[32m✓\033[0m %s\n" "$1"; }
warn() { printf "    \033[33m⚠\033[0m %s\n" "$1"; WARNINGS=$((WARNINGS+1)); }
fail() { printf "    \033[31m✗\033[0m %s\n" "$1"; ERRORS=$((ERRORS+1)); }
section() {
    echo ""
    printf "  \033[1m%s\033[0m\n" "$1"
    printf "  %s\n" "$(printf '─%.0s' $(seq 1 45))"
}

echo ""
echo "  ══════════════════════════════════════════════"
printf "   \033[1mPre-flight Checks\033[0m  [%s → %s/%s]\n" "$SERVICE" "$PROJECT" "$TARGET"
echo "  ══════════════════════════════════════════════"

# ════════════════════════════════════════════════
#  LOCAL CHECKS
# ════════════════════════════════════════════════
section "Local — Project Files"

# docker-compose.prod.yml
if [ -f "$PROJECT_DIR/docker-compose.prod.yml" ]; then
    ok "docker-compose.prod.yml found"
else
    fail "docker-compose.prod.yml missing"
fi

# .env.prod
if [ -f "$PROJECT_DIR/.env.prod" ]; then
    ok ".env.prod found"
else
    fail ".env.prod missing  (cp .env.example .env.prod)"
fi

# gradlew
if [ "$SKIP_BUILD" = "0" ]; then
    if [ -f "$PROJECT_DIR/gradlew" ] || [ -f "$PROJECT_DIR/gradlew.bat" ]; then
        ok "gradlew found"
    else
        fail "gradlew missing — cannot build"
    fi
fi

# ── Discover services ──
mapfile -t SERVICES < <(
    awk '/^services:/{f=1;next} f && /^[^ ]/{f=0} f && /^  [a-zA-Z0-9_-]+:/{gsub(/[ :]/,""); print}' \
    "$PROJECT_DIR/docker-compose.prod.yml" 2>/dev/null
)
if [ ${#SERVICES[@]} -eq 0 ]; then
    fail "No services found in docker-compose.prod.yml"
fi

# ── Per-service local checks ──
section "Local — Per-Service"

for s in "${SERVICES[@]}"; do
    [ "$SERVICE" != "all" ] && [ "$SERVICE" != "$s" ] && continue

    # Service directory
    if [ -d "$PROJECT_DIR/$s" ]; then
        ok "$s/ directory exists"
    else
        fail "$s/ directory missing"
        continue
    fi

    # Dockerfile
    if [ -f "$PROJECT_DIR/$s/Dockerfile" ]; then
        ok "$s/Dockerfile found"
    else
        fail "$s/Dockerfile missing"
    fi

    # build.gradle
    if [ -f "$PROJECT_DIR/$s/build.gradle" ]; then
        ok "$s/build.gradle found"
    else
        warn "$s/build.gradle missing — not a Gradle module?"
    fi

    # app.jar (only relevant if --no-build)
    if [ "$SKIP_BUILD" = "1" ]; then
        jar=$(ls "$PROJECT_DIR/$s/build/libs/"*-boot.jar 2>/dev/null | head -1)
        [ -z "$jar" ] && jar=$(ls "$PROJECT_DIR/$s/build/libs/"*.jar 2>/dev/null | grep -iv plain | head -1)
        if [ -n "$jar" ]; then
            ok "$s/app.jar ready  ($(du -h "$jar" | cut -f1))"
        else
            fail "$s — no built jar found  (run without --no-build)"
        fi
    fi
done

# ── .env.prod content validation ──
section "Local — Environment Variables (.env.prod)"

if [ -f "$PROJECT_DIR/.env.prod" ]; then
    check_var() {
        local var="$1" required="$2"
        local val
        val=$(grep -E "^${var}=" "$PROJECT_DIR/.env.prod" | cut -d= -f2- | tr -d '"' | tr -d "'")
        if [ -z "$val" ]; then
            [ "$required" = "required" ] && fail "$var is not set" || warn "$var is empty (optional)"
        elif echo "$val" | grep -qiE "^(CHANGE_ME|YOUR_HOST|example)"; then
            fail "$var still has placeholder value: $val"
        else
            ok "$var  ✓"
        fi
    }

    # Required
    check_var "DB_PRIMARY_URL"  "required"
    check_var "DB_PRIMARY_USER" "required"
    check_var "DB_PRIMARY_PASS" "required"
    check_var "DB_SECONDARY_URL"  "required"
    check_var "DB_SECONDARY_USER" "required"
    check_var "DB_SECONDARY_PASS" "required"
    check_var "JWT_SECRET" "required"

    # JWT secret length check
    jwt_secret=$(grep -E "^JWT_SECRET=" "$PROJECT_DIR/.env.prod" | cut -d= -f2- | tr -d '"' | tr -d "'")
    if [ -n "$jwt_secret" ] && ! echo "$jwt_secret" | grep -qiE "CHANGE_ME"; then
        jwt_len=${#jwt_secret}
        if [ "$jwt_len" -lt 32 ]; then
            fail "JWT_SECRET too short (${jwt_len} chars, minimum 32 recommended)"
        else
            ok "JWT_SECRET length OK (${jwt_len} chars)"
        fi
    fi

    # Optional with defaults
    check_var "JWT_EXPIRATION"         "optional"
    check_var "JWT_REFRESH_EXPIRATION" "optional"
    check_var "API_PORT"               "optional"
    check_var "MOBILE_PORT"            "optional"
fi

# ════════════════════════════════════════════════
#  SERVER CHECKS
# ════════════════════════════════════════════════
section "Server — $SERVER"

# SSH connectivity
if ssh -n -o ConnectTimeout=5 -o BatchMode=yes "$SERVER" "echo ok" >/dev/null 2>&1; then
    ok "SSH connection OK"
else
    fail "SSH connection failed — check server/key"
    echo ""
    echo "  ══════════════════════════════════════════════"
    printf "   \033[31mERRORS: %d\033[0m  \033[33mWARNINGS: %d\033[0m\n" "$ERRORS" "$WARNINGS"
    echo "  ══════════════════════════════════════════════"
    echo ""
    exit 1
fi

# Run all server checks in one SSH call
server_results=$(ssh -n "$SERVER" bash << 'SSHEOF'
results=""

# python3
if command -v python3 >/dev/null 2>&1; then
    results+="ok:python3 $(python3 --version 2>&1 | awk '{print $2}')\n"
else
    results+="fail:python3 not installed  (sudo apt install -y python3)\n"
fi

# python3-yaml (PyYAML)
if python3 -c "import yaml" 2>/dev/null; then
    results+="ok:python3-yaml (PyYAML) available\n"
else
    results+="fail:python3-yaml missing  (sudo apt install -y python3-yaml)\n"
fi

# docker
if command -v docker >/dev/null 2>&1; then
    results+="ok:docker $(docker --version | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')\n"
else
    results+="fail:docker not installed\n"
fi

# docker-compose
if command -v docker-compose >/dev/null 2>&1; then
    results+="ok:docker-compose $(docker-compose --version | grep -oE '[0-9]+\.[0-9]+\.[0-9]+')\n"
elif docker compose version >/dev/null 2>&1; then
    results+="ok:docker compose (plugin) available\n"
else
    results+="fail:docker-compose not installed\n"
fi

# disk space (warn if < 2GB free)
free_kb=$(df / | awk 'NR==2{print $4}')
free_gb=$(echo "$free_kb" | awk '{printf "%.1f", $1/1024/1024}')
if [ "$free_kb" -lt 2097152 ]; then
    results+="warn:disk space low (${free_gb}GB free)\n"
else
    results+="ok:disk space OK (${free_gb}GB free)\n"
fi

printf "%b" "$results"
SSHEOF
)

while IFS= read -r line; do
    level="${line%%:*}"
    msg="${line#*:}"
    case "$level" in
        ok)   ok "$msg" ;;
        warn) warn "$msg" ;;
        fail) fail "$msg" ;;
    esac
done <<< "$server_results"

# Server remote directory
if ssh -n "$SERVER" "[ -d $REMOTE ] && echo ok || echo missing" 2>/dev/null | grep -q "ok"; then
    ok "Remote dir $REMOTE exists"
else
    warn "Remote dir $REMOTE not found — will be created on deploy"
fi

# Docker network
if ssh -n "$SERVER" "docker network inspect ${PROJECT}-net >/dev/null 2>&1 && echo ok || echo missing" 2>/dev/null | grep -q "ok"; then
    ok "Docker network ${PROJECT}-net exists"
else
    warn "Docker network ${PROJECT}-net missing — will be created on deploy"
fi

# ── Per-service server checks ──
for s in "${SERVICES[@]}"; do
    [ "$SERVICE" != "all" ] && [ "$SERVICE" != "$s" ] && continue
    section "Server — $s"

    # Service dir
    if ssh -n "$SERVER" "[ -d $REMOTE/$s ] && echo ok || echo missing" 2>/dev/null | grep -q "ok"; then
        ok "$s/ exists on server"

        # docker-compose.prod.yml on server
        if ssh -n "$SERVER" "[ -f $REMOTE/$s/docker-compose.prod.yml ] && echo ok || echo missing" 2>/dev/null | grep -q "ok"; then
            ok "$s/docker-compose.prod.yml present"
        else
            warn "$s/docker-compose.prod.yml missing — will be generated on deploy"
        fi

        # Container running?
        container_status=$(ssh -n "$SERVER" "docker ps --filter name=${PROJECT}-${s} --format '{{.Status}}' 2>/dev/null")
        if [ -n "$container_status" ]; then
            ok "Container ${PROJECT}-${s}: $container_status"
        else
            warn "Container ${PROJECT}-${s} not running"
        fi
    else
        warn "$s/ not on server — will be created on deploy"
    fi
done

# ════════════════════════════════════════════════
#  SUMMARY
# ════════════════════════════════════════════════
echo ""
echo "  ══════════════════════════════════════════════"
if [ "$ERRORS" -eq 0 ] && [ "$WARNINGS" -eq 0 ]; then
    printf "   \033[32m✓ All checks passed\033[0m\n"
elif [ "$ERRORS" -eq 0 ]; then
    printf "   \033[32m✓ Ready to deploy\033[0m  \033[33m(warnings: %d)\033[0m\n" "$WARNINGS"
else
    printf "   \033[31m✗ ERRORS: %d\033[0m  \033[33mWARNINGS: %d\033[0m  — fix errors before deploying\033[0m\n" "$ERRORS" "$WARNINGS"
fi
echo "  ══════════════════════════════════════════════"
echo ""

[ "$ERRORS" -gt 0 ] && exit 1 || exit 0