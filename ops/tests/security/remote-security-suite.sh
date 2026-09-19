#!/usr/bin/env bash
# Runs the runtime security acceptance suite ON a remote server.
#
# The suite needs several identities. The long-lived one is a confidential
# client that already exists; its secret is read from a gitignored env file,
# travels to the server on stdin only, and lands in a 0600 token request file.
# The negative identities are throwaway clients created in the realm by
# `keycloak_test_clients.py`, whose generated secrets never leave the server.
# No secret and no token is ever passed on a command line, printed, or written
# to the suite output.
#
#   ops/tests/security/remote-security-suite.sh setup
#   ops/tests/security/remote-security-suite.sh run RUN_NAME [--pace-ms N]
#   ops/tests/security/remote-security-suite.sh clients-verify
#   ops/tests/security/remote-security-suite.sh exec 'server-side command'
#   ops/tests/security/remote-security-suite.sh teardown      # deletes the
#       throwaway clients, verifies they are gone, removes the server directory
#
# Environment (all overridable; no site value is baked into this file):
#   GEOSTAT_SERVER        default administrator@192.168.1.199
#   GEOSTAT_ENV_FILE      default ops/config/projects/geostat/services/api/.env.dev
#   GEOSTAT_TOKEN_URL / GEOSTAT_TOKEN_RESOLVE   token endpoint used on the server
#   GEOSTAT_CLIENT_ID_KEY / GEOSTAT_CLIENT_SECRET_KEY   keys read from the env file
#   GEOSTAT_REMOTE_DIR    default /tmp/geostat-sectest
#   SUITE_CONFIG          default ops/tests/security/config/dev-api.json
#   SUITE_CLIENT_SPEC     default ops/tests/security/config/throwaway-clients.json
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
HERE="$ROOT/ops/tests/security"
SERVER="${GEOSTAT_SERVER:-administrator@192.168.1.199}"
ENV_FILE="${GEOSTAT_ENV_FILE:-$ROOT/ops/config/projects/geostat/services/api/.env.dev}"
TOKEN_URL="${GEOSTAT_TOKEN_URL:-https://auth.geostat.internal/realms/geostat/protocol/openid-connect/token}"
TOKEN_RESOLVE="${GEOSTAT_TOKEN_RESOLVE:-auth.geostat.internal:443:127.0.0.1}"
ID_KEY="${GEOSTAT_CLIENT_ID_KEY:-ARTIFACT_OPERATOR_CLIENT_ID}"
SECRET_KEY="${GEOSTAT_CLIENT_SECRET_KEY:-ARTIFACT_OPERATOR_CLIENT_SECRET}"
REMOTE_DIR="${GEOSTAT_REMOTE_DIR:-/tmp/geostat-sectest}"
CONFIG="${SUITE_CONFIG:-$HERE/config/dev-api.json}"
CLIENT_SPEC="${SUITE_CLIENT_SPEC:-$HERE/config/throwaway-clients.json}"
SSH=(ssh -o BatchMode=yes "$SERVER")

value() { grep -E "^$1=" "$ENV_FILE" | head -1 | cut -d= -f2- | tr -d '\r\n'; }
quote() { printf "'%s'" "${1//\'/\'\\\'\'}"; }

render() { # substitute the server-side directory and token endpoint into a config
  sed -e "s#{{REMOTE_DIR}}#$REMOTE_DIR#g" \
      -e "s#{{TOKEN_URL}}#$TOKEN_URL#g" \
      -e "s#{{TOKEN_RESOLVE}}#$TOKEN_RESOLVE#g" "$1"
}

cmd_setup() {
  local client_id
  client_id="$(value "$ID_KEY")"
  [ -n "$client_id" ] && [ -n "$(value "$SECRET_KEY")" ] || {
    echo "reference client credentials ($ID_KEY/$SECRET_KEY) are not configured in $ENV_FILE" >&2
    exit 3
  }
  mkdir -p "$HERE/out"
  "${SSH[@]}" "mkdir -p $REMOTE_DIR && chmod 700 $REMOTE_DIR"
  scp -q -o BatchMode=yes "$HERE/security_probe.py" "$HERE/keycloak_test_clients.py" "$SERVER:$REMOTE_DIR/"
  render "$CONFIG" | "${SSH[@]}" "umask 077 && cat > $REMOTE_DIR/config.json"
  render "$CLIENT_SPEC" | "${SSH[@]}" "umask 077 && cat > $REMOTE_DIR/clients.json"
  # The reference client's secret travels on stdin only and lands in a 0600 file.
  { printf 'grant_type=client_credentials&client_id=%s&client_secret=' "$client_id"
    value "$SECRET_KEY"; } |
    "${SSH[@]}" "umask 077 && cat > $REMOTE_DIR/reference.tokenreq && chmod 600 $REMOTE_DIR/reference.tokenreq && echo reference-credential-stored"
  "${SSH[@]}" "T=\$(curl -sk --max-time 20 --resolve $TOKEN_RESOLVE $TOKEN_URL --data @$REMOTE_DIR/reference.tokenreq | python3 -c 'import sys,json; print(json.load(sys.stdin).get(\"access_token\",\"\"))'); [ -n \"\$T\" ] && echo reference-token-ok || { echo reference-token-failed >&2; exit 4; }"
  "${SSH[@]}" "cd $REMOTE_DIR && python3 keycloak_test_clients.py --spec clients.json --action create --tokenreq-dir $REMOTE_DIR --out $REMOTE_DIR/clients-created.json >/dev/null && echo throwaway-clients-created"
  scp -q -o BatchMode=yes "$SERVER:$REMOTE_DIR/clients-created.json" "$HERE/out/clients-created.json" 2>/dev/null || true
}

cmd_run() {
  local name="$1"; shift
  local out="$HERE/out/$name.json"
  mkdir -p "$HERE/out"
  local args=""
  for a in "$@"; do args="$args $(quote "$a")"; done
  set +e
  "${SSH[@]}" "cd $REMOTE_DIR && python3 security_probe.py --config config.json --out $REMOTE_DIR/$name.json$args"
  local rc=$?
  set -e
  scp -q -o BatchMode=yes "$SERVER:$REMOTE_DIR/$name.json" "$out" || true
  echo "run=$name rc=$rc out=$out"
  return 0
}

cmd_clients_verify() {
  "${SSH[@]}" "cd $REMOTE_DIR && python3 keycloak_test_clients.py --spec clients.json --action verify"
}

cmd_exec() { "${SSH[@]}" "$1"; }

cmd_teardown() {
  mkdir -p "$HERE/out"
  "${SSH[@]}" "cd $REMOTE_DIR && python3 keycloak_test_clients.py --spec clients.json --action delete --out $REMOTE_DIR/clients-deleted.json" || true
  scp -q -o BatchMode=yes "$SERVER:$REMOTE_DIR/clients-deleted.json" "$HERE/out/clients-deleted.json" 2>/dev/null || true
  "${SSH[@]}" "rm -rf $REMOTE_DIR && ls -d $REMOTE_DIR 2>/dev/null || echo remote-directory-removed"
}

case "${1:-}" in
  setup) shift; cmd_setup "$@" ;;
  run) shift; cmd_run "$@" ;;
  clients-verify) shift; cmd_clients_verify "$@" ;;
  exec) shift; cmd_exec "$@" ;;
  teardown) shift; cmd_teardown "$@" ;;
  *) echo "usage: $0 {setup|run NAME [--pace-ms N]|clients-verify|exec CMD|teardown}" >&2; exit 2 ;;
esac
