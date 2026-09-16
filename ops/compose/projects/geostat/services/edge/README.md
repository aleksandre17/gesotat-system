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
