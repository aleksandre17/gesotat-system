#!/usr/bin/env python3
"""Acceptance probe for a clamd endpoint before it may serve artifact admission (EXT-6).

Checks, fail-closed, and prints one machine-readable JSON document:
  1. PING -> PONG
  2. VERSION -> engine and signature database date; signatures older than --max-signature-age-hours fail
  3. INSTREAM clean payload -> "stream: OK"
  4. INSTREAM EICAR test string -> "stream: ... FOUND"
  5. INSTREAM of --expected-stream-max bytes is accepted (StreamMaxLength >= platform limit)
     and one byte more is refused with "INSTREAM size limit exceeded"

Usage:
  python clamd_acceptance_probe.py --host <private-host> [--port 3310]
      --expected-stream-max <PLATFORM_ARTIFACT_MALWARE_SCANNER_MAX_BYTES>

The EICAR string is the industry-standard harmless antivirus test file. No other payload leaves
this process; results never include host credentials (clamd TCP has none).
"""
import argparse
import datetime as dt
import json
import socket
import struct
import sys

EICAR = b"X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*"
CHUNK = 64 * 1024


def command(host, port, timeout, payload):
    with socket.create_connection((host, port), timeout=timeout) as s:
        s.sendall(payload)
        return _read_all(s)


def instream(host, port, timeout, blocks):
    with socket.create_connection((host, port), timeout=timeout) as s:
        s.sendall(b"zINSTREAM\0")
        try:
            for block in blocks:
                s.sendall(struct.pack(">I", len(block)) + block)
            s.sendall(struct.pack(">I", 0))
        except (BrokenPipeError, ConnectionResetError):
            pass  # clamd closes the stream after an oversize chunk; its reply is still readable
        return _read_all(s)


def _read_all(s):
    data = b""
    while True:
        try:
            chunk = s.recv(4096)
        except (ConnectionResetError, socket.timeout):
            break
        if not chunk:
            break
        data += chunk
    return data.rstrip(b"\0\n").decode("utf-8", "replace")


def sized(total):
    remaining = total
    while remaining > 0:
        n = min(CHUNK, remaining)
        yield b"\0" * n
        remaining -= n


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--host", required=True)
    p.add_argument("--port", type=int, default=3310)
    p.add_argument("--timeout", type=float, default=30.0)
    p.add_argument("--expected-stream-max", type=int, required=True)
    p.add_argument("--max-signature-age-hours", type=int, default=48)
    a = p.parse_args()

    checks = {}
    try:
        checks["ping"] = command(a.host, a.port, a.timeout, b"zPING\0") == "PONG"
        version = command(a.host, a.port, a.timeout, b"zVERSION\0")
        parts = version.split("/")
        age_ok, signature_date = False, None
        if len(parts) >= 3:
            signature_date = parts[2].strip()
            parsed = dt.datetime.strptime(signature_date, "%a %b %d %H:%M:%S %Y").replace(tzinfo=dt.timezone.utc)
            age_ok = dt.datetime.now(dt.timezone.utc) - parsed <= dt.timedelta(hours=a.max_signature_age_hours)
        checks["version"] = {"engine": parts[0] if parts else version, "signatureDate": signature_date, "fresh": age_ok}
        checks["clean"] = instream(a.host, a.port, a.timeout, [b"geostat clamd acceptance clean payload"]) == "stream: OK"
        eicar = instream(a.host, a.port, a.timeout, [EICAR])
        checks["eicar"] = eicar.endswith("FOUND")
        at_limit = instream(a.host, a.port, a.timeout, sized(a.expected_stream_max))
        over_limit = instream(a.host, a.port, a.timeout, sized(a.expected_stream_max + 1))
        checks["streamMax"] = {"atLimitAccepted": at_limit == "stream: OK",
                               "overLimitRefused": "size limit exceeded" in over_limit.lower()}
    except (OSError, ValueError) as error:
        checks["error"] = type(error).__name__
    passed = (checks.get("ping") is True and checks.get("version", {}).get("fresh") is True and checks.get("clean") is True
              and checks.get("eicar") is True and checks.get("streamMax", {}).get("atLimitAccepted") is True
              and checks.get("streamMax", {}).get("overLimitRefused") is True)
    print(json.dumps({"schema": "geostat.clamd-acceptance.v1", "port": a.port, "checks": checks,
                      "result": "PASS" if passed else "FAIL"}, indent=2))
    return 0 if passed else 1


if __name__ == "__main__":
    sys.exit(main())
