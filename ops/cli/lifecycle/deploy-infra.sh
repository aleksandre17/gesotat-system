#!/usr/bin/env bash
set -euo pipefail

# Deploy GEOSOTAT-owned shared infrastructure onto the existing external geostat-net.
# Secrets are never generated or committed. Provide GEOSTAT_ENV_FILE pointing to an
# external, operator-managed env file (the repository only contains a template).
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
SERVER="administrator@192.168.1.199"
REMOTE="/home/administrator/geostat/backend/infra/geostat-platform"
ENV="${GEOSTAT_ENV_FILE:-$ROOT/ops/config/projects/geostat/shared/runtime/production.env}"

[[ -f "$ENV" ]] || { echo "ERROR: $ENV is required" >&2; exit 1; }
for required in "$ROOT/ops/infra/geostat-platform/prometheus.yml" "$ROOT/ops/infra/geostat-platform/keycloak/geostat-realm.json" "$ROOT/ops/infra/geostat-platform/grafana/provisioning"; do
  [[ -e "$required" ]] || { echo "ERROR: required infrastructure asset is missing: $required" >&2; exit 1; }
done
for key in GEOSTAT_REDIS_PASSWORD GRAFANA_ADMIN_PASSWORD KEYCLOAK_DB_NAME KEYCLOAK_DB_USER KEYCLOAK_DB_PASSWORD KEYCLOAK_HOSTNAME KEYCLOAK_ADMIN_USERNAME KEYCLOAK_ADMIN_PASSWORD; do
  value="$(grep -E "^${key}=" "$ENV" | tail -n1 | cut -d= -f2- || true)"
  [[ -n "$value" && "$value" != *generate-a-strong* && "$value" != *CHANGE_ME* ]] || { echo "ERROR: $key is missing or placeholder" >&2; exit 1; }
done

ssh -o BatchMode=yes "$SERVER" "mkdir -p '$REMOTE'"
scp "$ROOT/ops/infra/geostat-platform/docker-compose.prod.yml" "$ROOT/ops/infra/geostat-platform/otel-collector-config.yml" "$SERVER:$REMOTE/"
scp "$ROOT/ops/infra/geostat-platform/prometheus.yml" "$SERVER:$REMOTE/"
scp -r "$ROOT/ops/infra/geostat-platform/keycloak" "$ROOT/ops/infra/geostat-platform/grafana" "$SERVER:$REMOTE/"
scp "$ENV" "$SERVER:$REMOTE/.env.prod"
ssh "$SERVER" "cd '$REMOTE' && docker compose --env-file .env.prod -f docker-compose.prod.yml config --quiet && docker compose --env-file .env.prod -f docker-compose.prod.yml up -d && docker compose --env-file .env.prod -f docker-compose.prod.yml ps"
