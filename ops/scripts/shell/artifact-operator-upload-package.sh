#!/usr/bin/env bash
# Uploads a local contract-bound ZIP package to the remote dev artifact API.
# MODE: admit (default, stores objects and registers the manifest) or preview (validate-only, writes nothing).
# The operator secret and token are never printed; the staged server copy is removed on completion.
set -euo pipefail

[ $# -eq 5 ] || [ $# -eq 6 ] || { echo "usage: $0 PACKAGE_CODE CONTRACT_CODE REVISION DATASET_CODE ARCHIVE.zip [admit|preview]" >&2; exit 2; }
PACKAGE_CODE=$1
CONTRACT_CODE=$2
REVISION=$3
DATASET_CODE=$4
ARCHIVE=$5
case "${6:-admit}" in
  admit) ENDPOINT=/api/v1/platform/artifacts/manifests/package ;;
  preview) ENDPOINT=/api/v1/platform/artifacts/manifests/package/preview ;;
  *) echo "MODE must be admit or preview" >&2; exit 2 ;;
esac
[[ $PACKAGE_CODE =~ ^[A-Za-z0-9._-]{1,120}$ && $CONTRACT_CODE =~ ^[A-Za-z0-9._-]{1,120}$ && $DATASET_CODE =~ ^[A-Za-z0-9._-]{1,120}$ && $REVISION =~ ^[1-9][0-9]{0,8}$ ]] || {
  echo "invalid contract package identity" >&2; exit 2;
}
[ -f "$ARCHIVE" ] || { echo "archive not found: $ARCHIVE" >&2; exit 2; }

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
ENV_FILE="$ROOT/ops/config/projects/geostat/services/api/.env.dev"
SERVER="${GEOSTAT_SERVER:-administrator@192.168.1.199}"
API="${GEOSTAT_API:-localhost:8081}"

value() { grep -E "^$1=" "$ENV_FILE" | head -1 | cut -d= -f2- | tr -d '\r\n'; }
CLIENT_ID="$(value ARTIFACT_OPERATOR_CLIENT_ID)"
CLIENT_SECRET="$(value ARTIFACT_OPERATOR_CLIENT_SECRET)"
[ -n "$CLIENT_ID" ] && [ -n "$CLIENT_SECRET" ] || { echo "operator client is not configured in $ENV_FILE" >&2; exit 3; }

REMOTE_PACKAGE="/tmp/geostat-artifact-package-$$.zip"
scp -q -o BatchMode=yes "$ARCHIVE" "$SERVER:$REMOTE_PACKAGE"
REMOTE_COMMAND="set -euo pipefail
  trap 'rm -f \"$REMOTE_PACKAGE\"' EXIT
  T=\$(curl -sk --resolve auth.geostat.internal:443:127.0.0.1 https://auth.geostat.internal/realms/geostat/protocol/openid-connect/token --data @- | python3 -c 'import sys,json; print(json.load(sys.stdin).get(\"access_token\",\"\"))')
  [ -n \"\$T\" ] || { echo 'token request failed' >&2; exit 4; }
  curl -sS -w '\\nHTTP=%{http_code}\\n' -X POST -H \"Authorization: Bearer \$T\" \\
    -F 'packageCode=$PACKAGE_CODE' -F 'contractCode=$CONTRACT_CODE' -F 'revision=$REVISION' -F 'datasetCode=$DATASET_CODE' \\
    -F \"package=@$REMOTE_PACKAGE;type=application/zip\" \"http://$API$ENDPOINT\""

{ printf 'grant_type=client_credentials&client_id=%s&client_secret=' "$CLIENT_ID"; printf '%s' "$CLIENT_SECRET"; } |
  ssh -o BatchMode=yes "$SERVER" "$REMOTE_COMMAND"
