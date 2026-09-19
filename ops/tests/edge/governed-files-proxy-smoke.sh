#!/usr/bin/env bash
# Proves the site-side download proxy (ops/compose/.../web/governed-files-proxy.conf) on the platform host:
# a throwaway nginx on the platform network plays the site's web server, and a signed URL issued by the API
# is fetched THROUGH it, the way a visitor's browser would reach it through the site origin.
# Nothing is published on the host and both containers are removed afterwards.
#
# Inputs (environment): SIGNED_URL (API-issued, never printed), EXPECTED_SHA256, GOVERNED_FILES_HOST,
#   CA_FILE (internal CA certificate on this host), SNIPPET (path of governed-files-proxy.conf on this host),
#   PLATFORM_NETWORK (docker network), NGINX_IMAGE, CURL_IMAGE.
set -euo pipefail
: "${SIGNED_URL:?}"; : "${EXPECTED_SHA256:?}"; : "${GOVERNED_FILES_HOST:?}"; : "${CA_FILE:?}"; : "${SNIPPET:?}"
NETWORK="${PLATFORM_NETWORK:?}"; NGINX_IMAGE="${NGINX_IMAGE:?}"; CURL_IMAGE="${CURL_IMAGE:?}"
NAME="site-proxy-smoke-$$"
WORK="$(mktemp -d)"
cleanup() { docker rm -f "$NAME" >/dev/null 2>&1 || true; rm -rf "$WORK"; }
trap cleanup EXIT

mkdir -p "$WORK/templates/snippets"
cp "$SNIPPET" "$WORK/templates/snippets/governed-files-proxy.conf.template"
printf 'server {\n  listen 8080;\n  include /etc/nginx/conf.d/snippets/governed-files-proxy.conf;\n}\n' > "$WORK/templates/default.conf.template"

docker run -d --name "$NAME" --network "$NETWORK" \
  -e GOVERNED_FILES_HOST="$GOVERNED_FILES_HOST" -e GOVERNED_FILES_CA=/etc/nginx/internal-ca.crt \
  -v "$WORK/templates:/etc/nginx/templates:ro" -v "$CA_FILE:/etc/nginx/internal-ca.crt:ro" "$NGINX_IMAGE" >/dev/null
for _ in $(seq 1 20); do docker exec "$NAME" nginx -t >/dev/null 2>&1 && break; sleep 0.5; done
docker exec "$NAME" nginx -t 2>&1 | tail -n 1

# What the frontend does: keep path and query, replace the origin with the site's own /files-download/ prefix.
PATH_AND_QUERY="${SIGNED_URL#*://*/}"
fetch() { docker run --rm --network "$NETWORK" "$CURL_IMAGE" -s "$@"; }

SHA="$(fetch "http://$NAME:8080/files-download/$PATH_AND_QUERY" | sha256sum | cut -d' ' -f1)"
CODE="$(fetch -o /dev/null -w '%{http_code}' "http://$NAME:8080/files-download/$PATH_AND_QUERY")"
UNSIGNED="$(fetch -o /dev/null -w '%{http_code}' "http://$NAME:8080/files-download/${PATH_AND_QUERY%%\?*}")"
POSTED="$(fetch -o /dev/null -w '%{http_code}' -X POST "http://$NAME:8080/files-download/$PATH_AND_QUERY")"
echo "through_site_proxy=$CODE sha256_match=$([ "$SHA" = "$EXPECTED_SHA256" ] && echo yes || echo no) unsigned=$UNSIGNED post=$POSTED"
[ "$CODE" = "200" ] && [ "$SHA" = "$EXPECTED_SHA256" ] && [ "$UNSIGNED" = "403" ] && [ "$POSTED" = "403" ]
