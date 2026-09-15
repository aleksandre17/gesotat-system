#!/usr/bin/env bash
set -euo pipefail
parent=/home/administrator/geostat/backend/infra
platform="$parent/geostat-platform"
mkdir -p "$platform"
install -m 0644 /tmp/geostat-platform-compose.yml "$platform/docker-compose.prod.yml"
install -m 0644 /tmp/geostat-platform-otel.yml "$platform/otel-collector-config.yml"
if [[ -f "$parent/.env.prod" ]]; then cp -p "$parent/.env.prod" "$platform/.env.prod"; chmod 600 "$platform/.env.prod"; fi
# Only the four GEOSOTAT platform-infra containers are restarted; MinIO and all
# containers owned by other projects remain untouched.
docker rm -f geostat-redis geostat-otel-collector geostat-keycloak geostat-keycloak-db 2>/dev/null || true
cd "$platform"
docker-compose --env-file .env.prod -f docker-compose.prod.yml config --quiet
docker-compose --env-file .env.prod -f docker-compose.prod.yml up -d
# Restore the shared parent compose to the MinIO-only contract used by the
# existing infrastructure owner; platform files stay below geostat-platform.
install -m 0644 /tmp/geostat-minio-compose.yml "$parent/docker-compose.prod.yml"
