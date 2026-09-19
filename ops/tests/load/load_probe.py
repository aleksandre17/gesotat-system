#!/usr/bin/env python3
"""Site-agnostic HTTP load and watchdog probe for one phase of a load test.

The probe drives a fixed set of endpoints with a fixed number of concurrent
clients for a fixed duration, records per-request latency and status codes, and
at the same time samples one or more *watch* URLs (typically health endpoints of
systems that must not be harmed) plus optional `docker stats` counters.

It aborts the phase immediately when a watch URL fails, answers too slowly, or
when the share of 5xx responses of the load itself exceeds a threshold.

Nothing in this file is specific to a site, project or endpoint: every URL,
header source, threshold and container name is an argument.

Bearer tokens are never accepted on the command line and never written to the
output. `--token-command` is executed with `sh -c` and its stdout (stripped) is
used as the bearer token; it is re-executed every `--token-refresh-seconds`.

Usage (one phase):
  python3 load_probe.py \
      --base-url http://localhost:8081 \
      --endpoint meta=/api/v1/... --endpoint doc=/api/v1/... \
      --concurrency 15 --duration 180 --phase ramp-15 \
      --token-command 'my-token-fetcher' \
      --watch-url prod=http://localhost:8083/health \
      --watch-url dev=http://localhost:8081/health \
      --watch-expect '"status":"UP"' --watch-timeout 3 \
      --abort-watch-latency-ms 3000 --abort-5xx-share 0.05 \
      --docker-container some-container --sample-interval 10 \
      --out /tmp/phase.json
"""

from __future__ import annotations

import argparse
import json
import os
import ssl
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from collections import Counter
from urllib.parse import urlsplit

# ---------------------------------------------------------------- utilities


def percentile(values, q):
    if not values:
        return None
    ordered = sorted(values)
    if len(ordered) == 1:
        return round(ordered[0], 2)
    pos = (len(ordered) - 1) * (q / 100.0)
    low = int(pos)
    high = min(low + 1, len(ordered) - 1)
    frac = pos - low
    return round(ordered[low] * (1 - frac) + ordered[high] * frac, 2)


def summarise(latencies_ms):
    if not latencies_ms:
        return {"count": 0}
    return {
        "count": len(latencies_ms),
        "minMs": round(min(latencies_ms), 2),
        "p50Ms": percentile(latencies_ms, 50),
        "p95Ms": percentile(latencies_ms, 95),
        "p99Ms": percentile(latencies_ms, 99),
        "maxMs": round(max(latencies_ms), 2),
        "meanMs": round(sum(latencies_ms) / len(latencies_ms), 2),
    }


def parse_named(pairs, what):
    out = []
    for raw in pairs or []:
        if "=" not in raw:
            raise SystemExit(f"--{what} expects NAME=VALUE, got {raw!r}")
        name, value = raw.split("=", 1)
        out.append((name, value))
    return out


# ---------------------------------------------------------------- token


class TokenSource:
    """Re-runs a command to obtain a bearer token. Never logs the token."""

    def __init__(self, command, refresh_seconds, retries=6):
        self.command = command
        self.refresh_seconds = refresh_seconds
        self.retries = retries
        self._token = None
        self._fetched_at = 0.0
        self._lock = threading.Lock()
        self.fetch_count = 0
        self.fetch_failures = 0

    def _fetch(self):
        last = ""
        for attempt in range(self.retries):
            try:
                proc = subprocess.run(
                    ["sh", "-c", self.command],
                    capture_output=True, text=True, timeout=60,
                )
                token = proc.stdout.strip()
                if proc.returncode == 0 and token:
                    self.fetch_count += 1
                    return token
                last = f"exit={proc.returncode} empty={not token}"
            except Exception as exc:  # noqa: BLE001
                last = type(exc).__name__
            self.fetch_failures += 1
            time.sleep(min(2 ** attempt, 20))
        raise RuntimeError(f"token command failed after {self.retries} attempts ({last})")

    def token(self):
        if not self.command:
            return None
        now = time.time()
        with self._lock:
            if self._token is None or now - self._fetched_at >= self.refresh_seconds:
                self._token = self._fetch()
                self._fetched_at = now
            return self._token


# ---------------------------------------------------------------- requests


_CTX = ssl.create_default_context()
_CTX.check_hostname = False
_CTX.verify_mode = ssl.CERT_NONE


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


_OPENER = urllib.request.build_opener(
    _NoRedirect, urllib.request.HTTPSHandler(context=_CTX)
)


# Admission-control headers worth counting. Deliberately excludes per-request
# counters such as RateLimit-Remaining, which would explode the summary.
CAPTURED_HEADERS = ("RateLimit-Policy", "RateLimit-Limit", "Retry-After")


def http_call(url, method="GET", token=None, timeout=30, body=None, content_type=None):
    """Returns (status, elapsed_ms, bytes_read, body_text_or_None, error_or_None).

    The sixth element is a dict of the admission-control headers in
    CAPTURED_HEADERS that the response carried.
    """
    req = urllib.request.Request(url, method=method, data=body)
    if token:
        req.add_header("Authorization", "Bearer " + token)
    if content_type:
        req.add_header("Content-Type", content_type)
    req.add_header("Accept", "*/*")
    started = time.perf_counter()

    def picked(headers):
        return {h: headers.get(h) for h in CAPTURED_HEADERS if headers.get(h)}

    try:
        with _OPENER.open(req, timeout=timeout) as resp:
            payload = resp.read()
            elapsed = (time.perf_counter() - started) * 1000.0
            return resp.status, elapsed, len(payload), payload, None, picked(resp.headers)
    except urllib.error.HTTPError as exc:
        payload = b""
        try:
            payload = exc.read()
        except Exception:  # noqa: BLE001
            pass
        elapsed = (time.perf_counter() - started) * 1000.0
        return exc.code, elapsed, len(payload), payload, None, picked(exc.headers)
    except Exception as exc:  # noqa: BLE001
        elapsed = (time.perf_counter() - started) * 1000.0
        return None, elapsed, 0, None, type(exc).__name__, {}


# ---------------------------------------------------------------- state


class PhaseState:
    def __init__(self, endpoints):
        self.lock = threading.Lock()
        self.latencies = {name: [] for name, _ in endpoints}
        self.latencies_2xx = {name: [] for name, _ in endpoints}
        self.latencies_429 = {name: [] for name, _ in endpoints}
        self.statuses = {name: Counter() for name, _ in endpoints}
        self.errors = {name: Counter() for name, _ in endpoints}
        self.admission_headers = Counter()
        self.stop = threading.Event()
        self.aborted = None

    def record(self, name, status, elapsed_ms, error, headers=None):
        with self.lock:
            self.latencies[name].append(elapsed_ms)
            if status is not None and 200 <= status < 300:
                self.latencies_2xx[name].append(elapsed_ms)
            elif status == 429:
                self.latencies_429[name].append(elapsed_ms)
            for key, value in (headers or {}).items():
                self.admission_headers[f"{key}: {value}"] += 1
            if error:
                self.errors[name][error] += 1
                self.statuses[name]["transport-error"] += 1
            else:
                self.statuses[name][str(status)] += 1

    def counts(self):
        with self.lock:
            total = Counter()
            for counter in self.statuses.values():
                total.update(counter)
            return total

    def abort(self, reason):
        with self.lock:
            if self.aborted is None:
                self.aborted = reason
        self.stop.set()


# ---------------------------------------------------------------- workers


def worker(args, endpoints, tokens, state, deadline):
    idx = threading.get_ident() % max(len(endpoints), 1)
    while not state.stop.is_set() and time.time() < deadline:
        name, path = endpoints[idx % len(endpoints)]
        idx += 1
        try:
            token = tokens.token()
        except Exception as exc:  # noqa: BLE001
            state.abort(f"token acquisition failed: {type(exc).__name__}")
            return
        status, elapsed, _, _, error, headers = http_call(
            args.base_url.rstrip("/") + path,
            token=token,
            timeout=args.request_timeout,
        )
        state.record(name, status, elapsed, error, headers)
        if args.pace_ms:
            time.sleep(args.pace_ms / 1000.0)


def watcher(args, watch_urls, state, deadline, samples, docker_samples):
    while not state.stop.is_set() and time.time() < deadline:
        sample = {"t": round(time.time(), 3), "watch": {}}
        for name, url in watch_urls:
            status, elapsed, _, payload, error, _ = http_call(
                url, timeout=args.watch_timeout
            )
            text = payload.decode("utf-8", "replace") if payload else ""
            ok = (
                error is None
                and status == 200
                and (args.watch_expect in text if args.watch_expect else True)
            )
            sample["watch"][name] = {
                "status": status, "ms": round(elapsed, 2), "ok": ok,
                "error": error,
            }
            if not ok:
                state.abort(
                    f"watch '{name}' unhealthy: status={status} error={error} "
                    f"expect={'present' if args.watch_expect in text else 'absent'}"
                )
            elif elapsed > args.abort_watch_latency_ms:
                state.abort(
                    f"watch '{name}' slow: {elapsed:.0f} ms > "
                    f"{args.abort_watch_latency_ms} ms"
                )
        if args.docker_container:
            sample["docker"] = docker_stats(args.docker_container)
            docker_samples.append(sample["docker"])
        samples.append(sample)

        counts = state.counts()
        total = sum(counts.values())
        if total >= args.min_requests_for_5xx_check:
            bad = sum(v for k, v in counts.items() if k.startswith("5"))
            if bad / total > args.abort_5xx_share:
                state.abort(
                    f"5xx share {bad}/{total} exceeds {args.abort_5xx_share:.0%}"
                )
        state.stop.wait(args.sample_interval)


def docker_stats(containers):
    try:
        proc = subprocess.run(
            ["docker", "stats", "--no-stream", "--format",
             "{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}", *containers],
            capture_output=True, text=True, timeout=30,
        )
    except Exception as exc:  # noqa: BLE001
        return {"error": type(exc).__name__}
    out = {}
    for line in proc.stdout.strip().splitlines():
        parts = line.split("\t")
        if len(parts) == 4:
            out[parts[0]] = {
                "cpuPerc": parts[1], "mem": parts[2], "memPerc": parts[3]
            }
    return out


# ---------------------------------------------------------------- main


def run_phase(args):
    endpoints = parse_named(args.endpoint, "endpoint")
    watch_urls = parse_named(args.watch_url, "watch-url")
    if not endpoints:
        raise SystemExit("at least one --endpoint is required")

    tokens = TokenSource(args.token_command, args.token_refresh_seconds)
    if args.token_command:
        tokens.token()  # fail fast, with backoff, before load starts

    state = PhaseState(endpoints)
    samples, docker_samples = [], []
    started = time.time()
    deadline = started + args.duration

    threads = [
        threading.Thread(
            target=worker, args=(args, endpoints, tokens, state, deadline), daemon=True
        )
        for _ in range(args.concurrency)
    ]
    watch_thread = None
    if watch_urls or args.docker_container:
        watch_thread = threading.Thread(
            target=watcher,
            args=(args, watch_urls, state, deadline, samples, docker_samples),
            daemon=True,
        )
        watch_thread.start()
    for t in threads:
        t.start()
    for t in threads:
        t.join(timeout=args.duration + args.request_timeout + 30)
    state.stop.set()
    if watch_thread:
        watch_thread.join(timeout=args.watch_timeout + 35)
    finished = time.time()

    counts = state.counts()
    total = sum(counts.values()) or 1
    per_endpoint = {}
    for name, path in endpoints:
        per_endpoint[name] = {
            "path": path,
            "latency": summarise(state.latencies[name]),
            "latencyAdmitted2xx": summarise(state.latencies_2xx[name]),
            "latencyRejected429": summarise(state.latencies_429[name]),
            "status": dict(state.statuses[name]),
            "transportErrors": dict(state.errors[name]),
        }

    watch_summary = {}
    for name, url in watch_urls:
        lat = [s["watch"][name]["ms"] for s in samples if name in s["watch"]]
        bad = [s["watch"][name] for s in samples
               if name in s["watch"] and not s["watch"][name]["ok"]]
        watch_summary[name] = {
            "url": redact(url),
            "samples": len(lat),
            "minMs": round(min(lat), 2) if lat else None,
            "maxMs": round(max(lat), 2) if lat else None,
            "meanMs": round(sum(lat) / len(lat), 2) if lat else None,
            "notOkSamples": len(bad),
        }

    return {
        "phase": args.phase,
        "config": {
            "baseUrl": redact(args.base_url),
            "concurrency": args.concurrency,
            "durationSeconds": args.duration,
            "endpoints": {n: p for n, p in endpoints},
            "requestTimeoutSeconds": args.request_timeout,
            "sampleIntervalSeconds": args.sample_interval,
            "abortWatchLatencyMs": args.abort_watch_latency_ms,
            "abort5xxShare": args.abort_5xx_share,
            "tokenRefreshSeconds": args.token_refresh_seconds,
        },
        "startedAtEpoch": round(started, 3),
        "elapsedSeconds": round(finished - started, 2),
        "requests": sum(counts.values()),
        "requestsPerSecond": round(sum(counts.values()) / max(finished - started, 0.001), 2),
        "statusCounts": dict(counts),
        "shares": {
            "429": round(counts.get("429", 0) / total, 4),
            "5xx": round(sum(v for k, v in counts.items() if k.startswith("5")) / total, 4),
            "2xx": round(sum(v for k, v in counts.items() if k.startswith("2")) / total, 4),
            "transportError": round(counts.get("transport-error", 0) / total, 4),
        },
        "latencyAdmitted2xxAllEndpoints": summarise(
            [v for name, _ in endpoints for v in state.latencies_2xx[name]]
        ),
        "admissionHeaders": dict(state.admission_headers),
        "perEndpoint": per_endpoint,
        "watch": watch_summary,
        "watchSamples": samples if args.keep_samples else len(samples),
        "dockerSummary": docker_summary(docker_samples),
        "tokenFetches": tokens.fetch_count,
        "tokenFetchRetries": tokens.fetch_failures,
        "aborted": state.aborted,
    }


def redact(url):
    parts = urlsplit(url)
    return f"{parts.scheme}://{parts.netloc}{parts.path}"


def docker_summary(docker_samples):
    out = {}
    for sample in docker_samples:
        if not isinstance(sample, dict):
            continue
        for name, values in sample.items():
            if not isinstance(values, dict) or "cpuPerc" not in values:
                continue
            bucket = out.setdefault(name, {"samples": 0, "cpuPerc": [], "memPerc": [], "memLast": None})
            bucket["samples"] += 1
            try:
                bucket["cpuPerc"].append(float(values["cpuPerc"].rstrip("%")))
                bucket["memPerc"].append(float(values["memPerc"].rstrip("%")))
            except ValueError:
                pass
            bucket["memLast"] = values["mem"]
    for name, bucket in out.items():
        cpu, mem = bucket.pop("cpuPerc"), bucket.pop("memPerc")
        bucket["cpuPercMax"] = round(max(cpu), 2) if cpu else None
        bucket["cpuPercMean"] = round(sum(cpu) / len(cpu), 2) if cpu else None
        bucket["memPercMax"] = round(max(mem), 2) if mem else None
    return out


def build_parser():
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--base-url", required=True)
    p.add_argument("--endpoint", action="append", metavar="NAME=PATH")
    p.add_argument("--concurrency", type=int, default=1)
    p.add_argument("--duration", type=float, default=60)
    p.add_argument("--phase", default="phase")
    p.add_argument("--token-command", default="")
    p.add_argument("--token-refresh-seconds", type=float, default=120)
    p.add_argument("--request-timeout", type=float, default=30)
    p.add_argument("--pace-ms", type=float, default=0,
                   help="optional think time between requests per client")
    p.add_argument("--watch-url", action="append", metavar="NAME=URL")
    p.add_argument("--watch-expect", default="")
    p.add_argument("--watch-timeout", type=float, default=3)
    p.add_argument("--abort-watch-latency-ms", type=float, default=3000)
    p.add_argument("--abort-5xx-share", type=float, default=0.05)
    p.add_argument("--min-requests-for-5xx-check", type=int, default=50)
    p.add_argument("--sample-interval", type=float, default=10)
    p.add_argument("--docker-container", action="append")
    p.add_argument("--keep-samples", action="store_true")
    p.add_argument("--out", default="")
    return p


def main(argv=None):
    args = build_parser().parse_args(argv)
    result = run_phase(args)
    text = json.dumps(result, indent=2, sort_keys=False)
    if args.out:
        with open(args.out, "w", encoding="utf-8") as handle:
            handle.write(text + "\n")
        os.chmod(args.out, 0o600)
    print(text)
    return 1 if result["aborted"] else 0


if __name__ == "__main__":
    sys.exit(main())
