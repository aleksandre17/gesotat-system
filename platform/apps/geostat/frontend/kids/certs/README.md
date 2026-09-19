# KIDS frontend — trust material (public certificates only)

Everything the KIDS site needs in order to trust the platform's internal endpoints lives in this directory.
**Only public certificates belong here. Never put a private key in this directory or anywhere in the repository.**

| File | What it is | Used for |
|---|---|---|
| `geostat-internal-ca.crt` | Internal CA (EC P-256, `CA:TRUE, pathlen:0`) | Verifying `https://files.geostat.internal` (governed downloads) |
| `geostat-edge-selfsigned.crt` | Public certificate of the shared edge | Verifying `https://auth.geostat.internal` and `https://api.geostat.internal` — **TO ADD**: fetch from the server `…/geostat-platform/edge/tls/tls.crt` (public file) |
| `geostat-trust-bundle.pem` | Both of the above concatenated | One file for tools that take a single CA bundle — **TO BUILD** once the edge certificate is here |

## How the site uses it

- **Site web server (nginx) → governed downloads:** mount `geostat-internal-ca.crt` and include
  `ops/compose/projects/geostat/services/web/governed-files-proxy.conf`
  (`GOVERNED_FILES_HOST=files.geostat.internal`, `GOVERNED_FILES_CA=<mounted path>`). The visitor's browser talks only
  to the site's own origin and needs none of these files.
- **Node tooling (Vite dev server, scripts):** `NODE_EXTRA_CA_CERTS=certs/geostat-trust-bundle.pem`.
- **A developer's browser or OS**, only when calling the internal names directly: import the bundle as a trusted
  authority and add the hosts line
  `192.168.1.199 geostat.internal api.geostat.internal auth.geostat.internal files.geostat.internal`.

## Verify before use

```bash
grep -c "PRIVATE KEY" certs/*            # must print 0 for every file
openssl x509 -in certs/geostat-internal-ca.crt -noout -subject -enddate -fingerprint -sha256
```

Status 2026-09-19: the internal CA certificate is in place; the edge certificate and the bundle could not be added
because the server was unreachable at the end of the session (see `docs/work/HANDOFF-2026-09-19.md`, "Session end").
