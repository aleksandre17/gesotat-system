# GeoStat internal production HTTPS edge

This is the project-scoped TLS termination point. It publishes only `443` for
GeoStat and proxies `/api/` to `geostat-api` on `geostat-net` and the UI to the
existing GeoStat UI host port. It does not modify or attach to other projects.

Required runtime inputs:

- `GEOSTAT_TLS_DIR` — directory containing `tls.crt` and `tls.key`;
- certificate SAN matching the internal hostname and/or client IP;
- a client trust anchor (for self-signed/internal CA certificates).

The overlay is fail-closed when the certificate directory is absent. Self-signed
certificates are suitable only for the controlled internal production boundary;
replace them with an approved internal CA chain when available.

## Governed artifact distribution (`files.geostat.internal`)

`nginx.conf` carries a second server block that publishes API-issued presigned
downloads: `server_name files.geostat.internal`, `GET`/`HEAD` only, a request
without an `X-Amz-Signature` query parameter is refused with `403`, and the
request is proxied unchanged to `http://minio:9000` because SigV4 signs the
`Host` header. `docker-compose.yml` adds the matching `geostat-net` alias, so
in-cluster clients resolve the name without DNS.

### Why this hostname has its own certificate

The other server blocks keep `tls.crt`/`tls.key` untouched. Re-issuing that
self-signed certificate to add a SAN is **not** transparent: an OpenSSL-based
client (curl, browsers, Go, Python) accepts a self-issued certificate only when
that exact certificate is in its truststore, so a re-issue breaks every such
client that already trusts the deployed one (`error 18 at 0 depth lookup:
self-signed certificate`); only JVM clients keep validating. The download
hostname therefore gets a leaf certificate of its own, signed by a small
internal CA, and the existing trust anchor is never invalidated. Adding or
rotating a hostname from now on only means issuing another leaf.

Key type is EC P-256: TLS 1.2 is the floor here, every client in scope
(OpenSSL 3, curl, JVM 17, current browsers) supports it, and it gives smaller
handshakes and cheaper signatures than RSA 3072 at equivalent strength. Pass
`--key-type rsa --rsa-bits 3072` if a client ever needs RSA.

### Issuing or rotating the certificate

```
ops/scripts/shell/tls/issue-internal-ca-leaf.sh   --dir "$GEOSTAT_TLS_DIR" --ca-cn "GeoStat Internal CA"   --san "DNS:files.geostat.internal" --leaf-cert files.crt --leaf-key files.key
```

The script creates the CA on first use (5 years, `CA:TRUE, pathlen:0`,
`keyCertSign, cRLSign`) and reuses it afterwards; the leaf is `CA:FALSE`,
`serverAuth`, at most 397 days, written as `leaf + CA` so the server presents a
complete chain. Keys are mode 0600 and never leave the host. It is idempotent —
an already-correct leaf is left alone — backs up whatever it overwrites with a
UTC timestamp suffix, and installs nothing unless the leaf key matches and
`openssl verify -CAfile <ca> <leaf>` passes.

### Deploying a change to this edge

The edge is shared with production, so:

1. Back up `nginx.conf` and `docker-compose.yml` on the server with a timestamp
   suffix, then copy both files from this directory.
2. Validate before restarting, on the network that resolves `minio` and with
   the same host alias the real container has:
   `docker run --rm --network geostat-net --add-host host.docker.internal:host-gateway
   -v $PWD/nginx.conf:/etc/nginx/nginx.conf:ro -v $GEOSTAT_TLS_DIR:/etc/nginx/tls:ro
   nginx:1.29.1-alpine nginx -t`.
3. `docker-compose up -d` in the edge directory — required, not `docker restart`:
   a new network alias is only applied when the container is recreated.
4. Immediately re-check `geostat-edge` health, the OIDC discovery document at
   `https://auth.geostat.internal/realms/geostat/.well-known/openid-configuration`
   **verified with the unchanged `tls.crt`** (this is what proves existing
   clients are unaffected), and the production API health on `:8083`. Roll back
   from the backups and bring the edge up again if any of them fails.
5. Point the signer at the hostname:
   `STORAGE_PUBLIC_ENDPOINT=https://files.geostat.internal` in the gitignored
   `ops/config/projects/geostat/services/api/.env.dev`, then redeploy the API.
   Signing is offline, so the API never has to reach or trust the endpoint.

### What a client needs (one time)

The CA's public certificate is in this directory at
`ca/geostat-internal-ca.crt` — public material, no private key. Installing it
once makes every current and future `*.geostat.internal` leaf trusted:

- Linux: copy to `/usr/local/share/ca-certificates/geostat-internal-ca.crt` and
  run `update-ca-certificates`;
- Windows: import into `Trusted Root Certification Authorities` (current user
  is enough);
- one-off tools: `curl --cacert ops/compose/projects/geostat/services/edge/ca/geostat-internal-ca.crt`.

There is no DNS server for `*.geostat.internal`. Clients outside the docker
network need this hosts entry:

```
192.168.1.199 geostat.internal api.geostat.internal auth.geostat.internal files.geostat.internal
```

No hosts file is edited by any script in this repository, and none was edited
on any client machine. For a one-off check without touching a hosts file:

```
curl --resolve files.geostat.internal:443:192.168.1.199   --cacert ops/compose/projects/geostat/services/edge/ca/geostat-internal-ca.crt "<signed url>"
```
