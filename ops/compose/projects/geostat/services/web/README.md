# Web composition boundary

Reserved for the web service composition when that runtime is enabled. The
project-level Compose files remain the orchestration authority and this folder
owns only web-specific overlays and health/resource settings.

## Governed file downloads for a site hosted on the platform server

A site frontend (for example the KIDS portal) that runs as a container on the platform host serves downloads
through its own origin. The visitor's browser never sees the internal download hostname and needs no internal CA
or hosts entry:

1. Attach the site's web container to the platform network and mount the internal CA public certificate
   (`../edge/ca/`).
2. Include `governed-files-proxy.conf` inside the site's `server { }` block (nginx image templates;
   environment: `GOVERNED_FILES_HOST`, `GOVERNED_FILES_CA`).
3. The frontend asks the API for the download (`…/download` → signed URL) and replaces only the origin with its
   own `/files-download/` prefix; path and query stay untouched because they are signed.

Proof on the platform host with a throwaway site server: `ops/tests/edge/governed-files-proxy-smoke.sh`
(200 with matching SHA-256 through the proxy; unsigned and `POST` → 403).
