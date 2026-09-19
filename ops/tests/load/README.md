# `ops/tests/load` — load, watchdog and recovery probes

Two small, site-agnostic probes plus a driver that runs them on a remote server
as an OIDC `client_credentials` client. Nothing here contains a project, site or
endpoint value: every URL, threshold and container name is an argument.

| File | Role |
|---|---|
| `load_probe.py` | drives N concurrent clients over a list of endpoints for a fixed duration; records per-request latency and status codes, splits latency into *admitted* (2xx) and *rejected* (429); samples watch URLs and `docker stats` every interval; aborts the phase on an unhealthy or slow watch URL or on a 5xx share above a threshold |
| `recovery_probe.py` | restarts one container and measures time to `healthy`, re-verifies read paths afterwards, and fires N simultaneous identical POSTs to prove a command is idempotent under contention |
| `remote-load-phase.sh` | copies the probes to the server, puts the client secret in a 0600 server-side file, runs one phase, copies the result JSON back, tears everything down |

## Why these and not `ab` / `hey` / `wrk` / k6

None of `ab`, `hey` or `wrk` exist on the server and installing system packages
is not allowed. Pulling a container image such as `grafana/k6` was rejected
because the host has roughly 1 GiB of free memory and is shared with production.
`python3` (3.12) is present and gives per-request latency, status codes,
response headers, health watchdog sampling and abort logic in one process with
no installation. The probes are also the only way to get the *watchdog* part:
aborting the load the moment a **different** system's health endpoint degrades.

## Secrets

The client secret is read from the gitignored
`ops/config/projects/geostat/services/api/.env.dev`, travels to the server on
**stdin only**, and lands in `/tmp/geostat-load/.tokenreq` with mode 0600. The
probe re-runs a token command (with exponential backoff, so a restart of the
edge or Keycloak is survivable) and keeps the token in process memory.
No secret and no token is ever passed on a command line, printed, or written to
the probe output. `teardown` removes the server-side directory.

## Invocation actually used on 2026-09-19

Evidence:
`docs/evidence/storage-line-load-and-recovery-runtime-2026-09-19.json`.
Raw per-phase output lands in `out/` (gitignored).

```bash
ops/tests/load/remote-load-phase.sh setup

# Phases: baseline (c=1, 60 s, paced under the admission budget), then
# c=5 / c=15 / c=30 for 180 s each, then a 60 s burst at c=60.
# Only --concurrency, --duration and (baseline only) --pace-ms differ.
ops/tests/load/remote-load-phase.sh run baseline-c1 \
  --base-url http://localhost:8081 \
  --endpoint meta=/api/v1/platform/artifacts/entities/KIDS_RESOURCE/128 \
  --endpoint download=/api/v1/platform/artifacts/entities/KIDS_RESOURCE/128/PRIMARY_FILE/ka/1/download \
  --endpoint packageRun=/api/v1/platform/artifacts/package-runs/2 \
  --endpoint manifestDocument=/api/v1/platform/artifacts/manifests/7/document \
  --concurrency 1 --duration 60 --pace-ms 600 \
  --watch-url prod=http://localhost:8083/health \
  --watch-url dev=http://localhost:8081/health \
  --watch-expect '"status":"UP"' --watch-timeout 3 \
  --abort-watch-latency-ms 3000 --abort-5xx-share 0.05 \
  --sample-interval 10 --keep-samples \
  --docker-container geostat-api-dev --docker-container geostat-api \
  --docker-container geostat-minio --docker-container geostat-system-mssql

ops/tests/load/remote-load-phase.sh run --probe recovery_probe.py recovery-and-idempotency \
  --base-url http://localhost:8081 \
  --restart geostat-api-dev --health-timeout 300 \
  --verify packageRun=/api/v1/platform/artifacts/package-runs/2 \
  --verify manifestDocument=/api/v1/platform/artifacts/manifests/7/document \
  --verify download=/api/v1/platform/artifacts/entities/KIDS_RESOURCE/128/PRIMARY_FILE/ka/1/download \
  --verify meta=/api/v1/platform/artifacts/entities/KIDS_RESOURCE/128 \
  --expect packageRun=COMPLETED \
  --idempotent-post /api/v1/platform/artifacts/package-runs \
  --body '{"manifestId":7}' --parallel 10 --identity-key runId

ops/tests/load/remote-load-phase.sh teardown
```

## Reading the numbers

The platform rate limit (`PlatformRateLimitFilter`, 120 requests per 60 s per
client key) applies per *client identity*. A load test run as one OIDC client
therefore has a ceiling of 120 admitted requests per minute no matter how much
load is offered, and the phases above that ceiling measure **bounded
admission**, not backend capacity. Read `admitted2xxLatency` — the overall
`latency` field mixes cheap 429 rejections with real work. Use `--pace-ms` to
stay under the budget when you want true service latency.

## Safety rules honoured by the driver

* Only the container named in `--restart` is restarted; nothing else is mutated.
* Any watch URL that stops matching `--watch-expect`, answers non-200, or
  exceeds `--abort-watch-latency-ms` aborts the phase at once — this is how a
  production health endpoint on the same host is protected during a dev load
  test.
* `--abort-5xx-share` stops a phase that is actually damaging the target.
