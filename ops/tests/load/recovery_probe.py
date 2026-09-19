#!/usr/bin/env python3
"""Container restart / durable-state recovery and concurrent-idempotency probe.

Three independent, optional steps, all driven by arguments (no site values):

1. `--restart CONTAINER` restarts one container and measures the time until
   Docker reports it healthy again (`--health-timeout`).
2. `--verify NAME=PATH` (repeatable) issues authenticated GETs afterwards and
   records status, elapsed time and — with `--expect NAME=SUBSTRING` — whether
   the response body contains an expected substring. This is what proves that
   state persisted across the restart rather than living in process memory.
3. `--idempotent-post PATH --body JSON --parallel N` fires N identical POSTs at
   the same instant and reports every status and the distinct values of
   `--identity-key` found in the responses. A correctly idempotent command
   returns one and the same identity to all callers.

Tokens come from `--token-command` (see load_probe.py) and are never printed.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
import threading
import time

from load_probe import TokenSource, http_call  # same directory


def sh(*cmd, timeout=180):
    proc = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
    return proc.returncode, proc.stdout.strip(), proc.stderr.strip()


def container_health(name):
    rc, out, _ = sh(
        "docker", "inspect", "-f",
        "{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}",
        name,
    )
    return out if rc == 0 else None


def restart_and_wait(name, health_timeout, poll):
    started = time.time()
    rc, _, err = sh("docker", "restart", name)
    if rc != 0:
        return {"container": name, "restarted": False, "error": err[:400]}
    restart_returned = time.time()
    status, healthy_at = None, None
    while time.time() - started < health_timeout:
        status = container_health(name)
        if status == "healthy":
            healthy_at = time.time()
            break
        time.sleep(poll)
    return {
        "container": name,
        "restarted": True,
        "dockerRestartSeconds": round(restart_returned - started, 2),
        "secondsToHealthy": round(healthy_at - started, 2) if healthy_at else None,
        "finalHealth": status,
        "healthy": healthy_at is not None,
        "healthTimeoutSeconds": health_timeout,
    }


def parse_named(pairs, what):
    out = []
    for raw in pairs or []:
        if "=" not in raw:
            raise SystemExit(f"--{what} expects NAME=VALUE, got {raw!r}")
        name, value = raw.split("=", 1)
        out.append((name, value))
    return out


def dig(obj, key):
    """First value found for `key` anywhere in a nested JSON structure."""
    if isinstance(obj, dict):
        if key in obj:
            return obj[key]
        for value in obj.values():
            found = dig(value, key)
            if found is not None:
                return found
    elif isinstance(obj, list):
        for item in obj:
            found = dig(item, key)
            if found is not None:
                return found
    return None


def verify(base_url, checks, expects, tokens, timeout, retries, retry_wait):
    results = {}
    for name, path in checks:
        attempt, outcome = 0, None
        while attempt < retries:
            attempt += 1
            status, elapsed, size, payload, error, _ = http_call(
                base_url.rstrip("/") + path, token=tokens.token(), timeout=timeout
            )
            text = payload.decode("utf-8", "replace") if payload else ""
            outcome = {
                "path": path, "status": status, "ms": round(elapsed, 2),
                "bytes": size, "attempts": attempt, "transportError": error,
            }
            if name in expects:
                outcome["expected"] = expects[name]
                outcome["expectedPresent"] = expects[name] in text
            if status == 200 and outcome.get("expectedPresent", True):
                break
            time.sleep(retry_wait)
        results[name] = outcome
    return results


def idempotency(base_url, path, body, parallel, identity_key, tokens, timeout):
    token = tokens.token()
    url = base_url.rstrip("/") + path
    payload = body.encode("utf-8")
    barrier = threading.Barrier(parallel)
    results = [None] * parallel

    def fire(index):
        barrier.wait()
        status, elapsed, _, data, error, _ = http_call(
            url, method="POST", token=token, timeout=timeout,
            body=payload, content_type="application/json",
        )
        identity = None
        try:
            identity = dig(json.loads(data.decode("utf-8")), identity_key)
        except Exception:  # noqa: BLE001
            pass
        results[index] = {
            "status": status, "ms": round(elapsed, 2),
            identity_key: identity, "transportError": error,
        }

    threads = [threading.Thread(target=fire, args=(i,)) for i in range(parallel)]
    for t in threads:
        t.start()
    for t in threads:
        t.join(timeout=timeout + 30)

    statuses, identities = {}, {}
    for entry in results:
        if entry is None:
            statuses["no-result"] = statuses.get("no-result", 0) + 1
            continue
        key = str(entry["status"])
        statuses[key] = statuses.get(key, 0) + 1
        value = entry.get(identity_key)
        if value is not None:
            identities[str(value)] = identities.get(str(value), 0) + 1
    return {
        "path": path,
        "requestBody": body,
        "parallel": parallel,
        "identityKey": identity_key,
        "statusCounts": statuses,
        "distinctIdentities": identities,
        "singleIdentity": len(identities) == 1,
        "responses": results,
    }


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--base-url", required=True)
    p.add_argument("--token-command", default="")
    p.add_argument("--token-refresh-seconds", type=float, default=120)
    p.add_argument("--restart")
    p.add_argument("--health-timeout", type=float, default=300)
    p.add_argument("--health-poll", type=float, default=2)
    p.add_argument("--verify", action="append", metavar="NAME=PATH")
    p.add_argument("--expect", action="append", metavar="NAME=SUBSTRING")
    p.add_argument("--verify-retries", type=int, default=5)
    p.add_argument("--verify-retry-wait", type=float, default=5)
    p.add_argument("--idempotent-post")
    p.add_argument("--body", default="{}")
    p.add_argument("--parallel", type=int, default=10)
    p.add_argument("--identity-key", default="id")
    p.add_argument("--request-timeout", type=float, default=60)
    p.add_argument("--out", default="")
    args = p.parse_args(argv)

    tokens = TokenSource(args.token_command, args.token_refresh_seconds)
    result = {"baseUrl": args.base_url, "startedAtEpoch": round(time.time(), 3)}

    if args.restart:
        result["recovery"] = restart_and_wait(
            args.restart, args.health_timeout, args.health_poll
        )
    if args.verify:
        result["verifyAfterRestart"] = verify(
            args.base_url, parse_named(args.verify, "verify"),
            dict(parse_named(args.expect, "expect")), tokens,
            args.request_timeout, args.verify_retries, args.verify_retry_wait,
        )
    if args.idempotent_post:
        result["idempotency"] = idempotency(
            args.base_url, args.idempotent_post, args.body, args.parallel,
            args.identity_key, tokens, args.request_timeout,
        )

    text = json.dumps(result, indent=2)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as handle:
            handle.write(text + "\n")
    print(text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
