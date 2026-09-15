#!/usr/bin/env bash
set -euo pipefail
base=/home/administrator/geostat/backend/infra/geostat-platform
mkdir -p "$base"
install -m 0644 /tmp/geostat-infra-compose.yml "$base/docker-compose.prod.yml"
install -m 0644 /tmp/geostat-otel-config.yml "$base/otel-collector-config.yml"
if [[ ! -f "$base/.env.prod" ]]; then
  umask 077
  {
    printf 'GEOSTAT_REDIS_PASSWORD=%s\n' "$(openssl rand -hex 32)"
    printf '%s\n' 'KEYCLOAK_DB_NAME=keycloak' 'KEYCLOAK_DB_USER=keycloak'
    printf 'KEYCLOAK_DB_PASSWORD=%s\n' "$(openssl rand -hex 32)"
    printf '%s\n' 'KEYCLOAK_HOSTNAME=keycloak' 'KEYCLOAK_ADMIN_USERNAME=platform-admin'
    printf 'KEYCLOAK_ADMIN_PASSWORD=%s\n' "$(openssl rand -hex 32)"
    printf 'GRAFANA_ADMIN_PASSWORD=%s\n' "$(openssl rand -hex 32)"
  } > "$base/.env.prod"
fi
# Repair only the missing Grafana secret in legacy env files; existing values are never read or replaced.
if ! grep -Eq '^GRAFANA_ADMIN_PASSWORD=[^[:space:]]+$' "$base/.env.prod"; then
  sed -i '/^GRAFANA_ADMIN_PASSWORD=/d' "$base/.env.prod"
  printf 'GRAFANA_ADMIN_PASSWORD=%s\n' "$(openssl rand -hex 32)" >> "$base/.env.prod"
fi
chmod 600 "$base/.env.prod"
cd "$base"
docker-compose --env-file .env.prod -f docker-compose.prod.yml config --quiet
docker-compose --env-file .env.prod -f docker-compose.prod.yml up -d
docker-compose --env-file .env.prod -f docker-compose.prod.yml ps
