#!/usr/bin/env bash
# Runs `load_probe.py` ON a remote server as an OIDC client_credentials client.
#
# The probe needs one token reused for many requests, so the client secret is
# written once to a 0600 file on the server and the probe re-fetches a token
# from it as needed. The secret and the token never appear in a command line,
# in the probe output, or on the local disk outside the gitignored env file.
#
#   ops/tests/load/remote-load-phase.sh setup
#   ops/tests/load/remote-load-phase.sh run PHASE_NAME [load_probe.py args...]
#   ops/tests/load/remote-load-phase.sh exec 'arbitrary server-side command'
#   ops/tests/load/remote-load-phase.sh teardown
#
# `run` writes <PHASE_NAME>.json on the server and copies it to
# --local-out (default: ops/tests/load/out/PHASE_NAME.json, gitignored).
#
# Environment (all overridable, no site values are baked in):
#   GEOSTAT_SERVER      default administrator@192.168.1.199
#   GEOSTAT_ENV_FILE    default ops/config/projects/geostat/services/api/.env.dev
#   GEOSTAT_TOKEN_URL   default https://auth.geostat.internal/realms/geostat/protocol/openid-connect/token
#   GEOSTAT_TOKEN_RESOLVE default auth.geostat.internal:443:127.0.0.1
#   GEOSTAT_CLIENT_ID_KEY / GEOSTAT_CLIENT_SECRET_KEY  keys read from the env file
#   GEOSTAT_REMOTE_DIR  default /tmp/geostat-load
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
SERVER="${GEOSTAT_SERVER:-administrator@192.168.1.199}"
ENV_FILE="${GEOSTAT_ENV_FILE:-$ROOT/ops/config/projects/geostat/services/api/.env.dev}"
TOKEN_URL="${GEOSTAT_TOKEN_URL:-https://auth.geostat.internal/realms/geostat/protocol/openid-connect/token}"
TOKEN_RESOLVE="${GEOSTAT_TOKEN_RESOLVE:-auth.geostat.internal:443:127.0.0.1}"
ID_KEY="${GEOSTAT_CLIENT_ID_KEY:-ARTIFACT_OPERATOR_CLIENT_ID}"
SECRET_KEY="${GEOSTAT_CLIENT_SECRET_KEY:-ARTIFACT_OPERATOR_CLIENT_SECRET}"
REMOTE_DIR="${GEOSTAT_REMOTE_DIR:-/tmp/geostat-load}"
SSH=(ssh -o BatchMode=yes "$SERVER")

value() { grep -E "^$1=" "$ENV_FILE" | head -1 | cut -d= -f2- | tr -d '\r\n'; }

quote() { printf "'%s'" "${1//\'/\'\\\'\'}"; }

token_command() {
  # Executed on the server by the probe. Prints only the access token.
  printf "curl -sk --max-time 20 --resolve %s %s --data @%s/.tokenreq | python3 -c 'import sys,json; print(json.load(sys.stdin).get(\"access_token\",\"\"))'" \
    "$TOKEN_RESOLVE" "$TOKEN_URL" "$REMOTE_DIR"
}

cmd_setup() {
  local client_id
  client_id="$(value "$ID_KEY")"
  [ -n "$client_id" ] && [ -n "$(value "$SECRET_KEY")" ] || {
    echo "client credentials ($ID_KEY/$SECRET_KEY) are not configured in $ENV_FILE" >&2
    exit 3
  }
  "${SSH[@]}" "mkdir -p $REMOTE_DIR && chmod 700 $REMOTE_DIR"
  scp -q -o BatchMode=yes "$ROOT/ops/tests/load/load_probe.py" \
      "$ROOT/ops/tests/load/recovery_probe.py" "$SERVER:$REMOTE_DIR/"
  # Secret travels on stdin only and lands in a 0600 file.
  { printf 'grant_type=client_credentials&client_id=%s&client_secret=' "$client_id"
    value "$SECRET_KEY"; } |
    "${SSH[@]}" "umask 077 && cat > $REMOTE_DIR/.tokenreq && chmod 600 $REMOTE_DIR/.tokenreq && echo setup-ok"
  # Prove the credential works without revealing anything about the token.
  "${SSH[@]}" "T=\$($(token_command)); [ -n \"\$T\" ] && echo token-ok || { echo token-failed >&2; exit 4; }"
}

cmd_run() {
  local script="load_probe.py" phase_flag="--phase"
  if [ "${1:-}" = "--probe" ]; then script="$2"; phase_flag=""; shift 2; fi
  local phase="$1"; shift
  local local_out="$ROOT/ops/tests/load/out/$phase.json"
  if [ "${1:-}" = "--local-out" ]; then local_out="$2"; shift 2; fi
  local args=""
  for a in "$@"; do args="$args $(quote "$a")"; done
  mkdir -p "$(dirname "$local_out")"
  set +e
  local phase_arg=""
  [ -n "$phase_flag" ] && phase_arg=" $phase_flag $(quote "$phase")"
  "${SSH[@]}" "cd $REMOTE_DIR && python3 $script$phase_arg --out $REMOTE_DIR/$phase.json --token-command $(quote "$(token_command)")$args > /dev/null"
  local rc=$?
  set -e
  scp -q -o BatchMode=yes "$SERVER:$REMOTE_DIR/$phase.json" "$local_out" || true
  echo "phase=$phase rc=$rc out=$local_out"
  return 0
}

cmd_exec() { "${SSH[@]}" "$1"; }

cmd_teardown() {
  "${SSH[@]}" "rm -rf $REMOTE_DIR && echo teardown-ok"
}

case "${1:-}" in
  setup) shift; cmd_setup "$@" ;;
  run) shift; cmd_run "$@" ;;
  exec) shift; cmd_exec "$@" ;;
  teardown) shift; cmd_teardown "$@" ;;
  *) echo "usage: $0 {setup|run PHASE [args]|exec CMD|teardown}" >&2; exit 2 ;;
esac
