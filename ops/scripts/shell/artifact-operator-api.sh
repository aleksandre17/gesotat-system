#!/usr/bin/env bash
# Calls the remote dev API as the Keycloak service client `geostat-artifact-operator`.
# A fresh short-lived token is obtained on the server for every call and never printed.
#
# Usage: ops/scripts/shell/artifact-operator-api.sh METHOD PATH [extra curl args...]
#   ops/scripts/shell/artifact-operator-api.sh GET /api/v1/platform/artifacts/entities/KIDS_RESOURCE/128
#   ops/scripts/shell/artifact-operator-api.sh POST "/api/v1/platform/artifacts/snapshots/52/attachments?manifestId=4&dryRun=true"
#
# Inputs (gitignored): ops/config/projects/geostat/services/api/.env.dev
#   ARTIFACT_OPERATOR_CLIENT_ID, ARTIFACT_OPERATOR_CLIENT_SECRET
# Overrides: GEOSTAT_SERVER (default administrator@192.168.1.199), GEOSTAT_API (default localhost:8081)
set -euo pipefail

[ $# -ge 2 ] || { echo "usage: $0 METHOD PATH [curl args...]" >&2; exit 2; }
METHOD=$1; PATH_PART=$2; shift 2
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
ENV_FILE="$ROOT/ops/config/projects/geostat/services/api/.env.dev"
SERVER="${GEOSTAT_SERVER:-administrator@192.168.1.199}"
API="${GEOSTAT_API:-localhost:8081}"

value() { grep -E "^$1=" "$ENV_FILE" | head -1 | cut -d= -f2- | tr -d '\r\n'; }
CLIENT_ID="$(value ARTIFACT_OPERATOR_CLIENT_ID)"
[ -n "$CLIENT_ID" ] && [ -n "$(value ARTIFACT_OPERATOR_CLIENT_SECRET)" ] || { echo "operator client is not configured in $ENV_FILE" >&2; exit 3; }

quote() { printf "'%s'" "${1//\'/\'\\\'\'}"; }
ARGS=""; for a in "$@"; do ARGS="$ARGS $(quote "$a")"; done

# The secret travels only on stdin; the token lives only in a server-side shell variable.
{ printf 'grant_type=client_credentials&client_id=%s&client_secret=' "$CLIENT_ID"; value ARTIFACT_OPERATOR_CLIENT_SECRET; } |
ssh -o BatchMode=yes "$SERVER" "
  T=\$(curl -sk --resolve auth.geostat.internal:443:127.0.0.1 https://auth.geostat.internal/realms/geostat/protocol/openid-connect/token --data @- \
       | python3 -c 'import sys,json; print(json.load(sys.stdin).get(\"access_token\",\"\"))')
  [ -n \"\$T\" ] || { echo 'token request failed' >&2; exit 4; }
  curl -s -w '\nHTTP=%{http_code}\n' -X $(quote "$METHOD") -H \"Authorization: Bearer \$T\" $ARGS $(quote "http://$API$PATH_PART")"
