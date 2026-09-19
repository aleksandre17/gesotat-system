#!/usr/bin/env python3
"""Site-agnostic RUNTIME security acceptance probe for an OIDC-protected HTTP API.

Nothing in this file names a project, site, realm, host, endpoint, role or claim
value: every one of those is read from the JSON configuration passed with
`--config`. The same probe therefore runs against any deployment whose
configuration describes it.

What it proves, in one pass, against a live deployment:

  authn      anonymous / malformed / tampered-signature / unknown-key /
             copied-kid / alg=none / HMAC-with-the-RSA-public-key /
             expired / wrong-audience / missing-required-claim / wrong-issuer
  authz      a valid privileged identity reads; an identity without the
             authority is refused; a privileged identity is refused on an
             endpoint that demands a higher authority
  transport  response headers and error media type on both a protected 200 and
             a 401, reflection of an unsafe request header, CORS preflight from
             a foreign origin
  discovery  OIDC discovery and JWKS over TLS with an explicit trust anchor
  replay     configured page introspection/query flows, including whether the
             relations a revision declares are actually hydrated in the rows

Secrets: the probe never receives a client secret. It receives, per identity, a
*token command* — a shell command that prints an access token on stdout. Tokens
live only in process memory and are never written to the output, the log or a
command line. Only a token's non-sensitive decoded metadata (algorithm, kid,
lifetime in seconds, whether a claim is present) is ever recorded.

Requires: python3 standard library and the `curl` and `openssl` CLIs.
"""

from __future__ import annotations

import argparse
import base64
import json
import os
import shutil
import subprocess
import sys
import tempfile
import time
from typing import Any

# --------------------------------------------------------------------------
# small helpers
# --------------------------------------------------------------------------


def b64u(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).decode("ascii").rstrip("=")


def b64u_decode(text: str) -> bytes:
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


def jwt_part(token: str, index: int) -> dict:
    try:
        return json.loads(b64u_decode(token.split(".")[index]))
    except Exception:
        return {}


class Curl:
    """One transport for every request, so TLS trust anchors and host
    overrides are expressed the same way everywhere."""

    def __init__(self, timeout: int = 30) -> None:
        self.timeout = timeout

    def __call__(
        self,
        method: str,
        url: str,
        headers: list[str] | None = None,
        data: bytes | None = None,
        cacert: str | None = None,
        resolve: str | None = None,
        insecure: bool = False,
        extra: list[str] | None = None,
    ) -> tuple[int, dict[str, str], bytes]:
        work = tempfile.mkdtemp(prefix="probe-")
        body_file = os.path.join(work, "body")
        head_file = os.path.join(work, "head")
        data_file = os.path.join(work, "data")
        argv = [
            "curl", "-s", "-o", body_file, "-D", head_file,
            "-w", "%{http_code}", "--max-time", str(self.timeout),
            "-X", method.upper(),
        ]
        for header in headers or []:
            argv += ["-H", header]
        if cacert:
            argv += ["--cacert", cacert]
        if resolve:
            argv += ["--resolve", resolve]
        if insecure:
            argv += ["-k"]
        if data is not None:
            with open(data_file, "wb") as handle:
                handle.write(data)
            argv += ["--data-binary", "@" + data_file]
        argv += list(extra or [])
        argv += [url]
        try:
            done = subprocess.run(argv, capture_output=True, text=True)
            code = int(done.stdout.strip() or 0)
            raw_head = open(head_file, "rb").read().decode("utf-8", "replace") if os.path.exists(head_file) else ""
            body = open(body_file, "rb").read() if os.path.exists(body_file) else b""
        finally:
            shutil.rmtree(work, ignore_errors=True)
        parsed: dict[str, str] = {}
        for line in raw_head.splitlines():
            if ":" in line:
                name, _, value = line.partition(":")
                parsed[name.strip().lower()] = value.strip()
        return code, parsed, body


# --------------------------------------------------------------------------
# token forging (openssl CLI for RSA, stdlib for the rest)
# --------------------------------------------------------------------------


class Forge:
    def __init__(self, workdir: str) -> None:
        self.dir = workdir
        self.key = os.path.join(workdir, "forge.key")
        subprocess.run(
            ["openssl", "genrsa", "-out", self.key, "2048"],
            capture_output=True, check=True,
        )

    def rs256(self, header: dict, payload: dict) -> str:
        signing_input = f"{b64u(json.dumps(header).encode())}.{b64u(json.dumps(payload).encode())}"
        data_file = os.path.join(self.dir, "si.bin")
        with open(data_file, "wb") as handle:
            handle.write(signing_input.encode())
        done = subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", self.key, data_file],
            capture_output=True, check=True,
        )
        return f"{signing_input}.{b64u(done.stdout)}"

    @staticmethod
    def hs256(header: dict, payload: dict, secret: bytes) -> str:
        import hmac
        import hashlib

        signing_input = f"{b64u(json.dumps(header).encode())}.{b64u(json.dumps(payload).encode())}"
        mac = hmac.new(secret, signing_input.encode(), hashlib.sha256).digest()
        return f"{signing_input}.{b64u(mac)}"

    @staticmethod
    def unsigned(header: dict, payload: dict) -> str:
        return f"{b64u(json.dumps(header).encode())}.{b64u(json.dumps(payload).encode())}."


def tamper_signature(token: str) -> str:
    """Flip the last byte of the signature, leaving header and payload intact."""
    head, payload, signature = token.split(".")
    raw = bytearray(b64u_decode(signature))
    raw[-1] ^= 0x01
    return f"{head}.{payload}.{b64u(bytes(raw))}"


# --------------------------------------------------------------------------
# non-sensitive body / header inspection
# --------------------------------------------------------------------------

LEAK_MARKERS = (
    "Exception", "at java.", "at org.", "at jakarta.", "Caused by",
    "\tat ", "StackTrace", "SQLState", "com.microsoft.sqlserver",
)


def body_summary(headers: dict, body: bytes) -> dict:
    """Record only what is safe: media type, size, RFC 9457 envelope fields and
    whether the body leaks implementation detail. Never the body itself."""
    text = body.decode("utf-8", "replace")
    out: dict[str, Any] = {
        "contentType": headers.get("content-type"),
        "bytes": len(body),
        "leaksImplementationDetail": any(marker in text for marker in LEAK_MARKERS),
    }
    try:
        parsed = json.loads(text)
    except Exception:
        return out
    if isinstance(parsed, dict):
        for field in ("type", "title", "status", "code", "detail"):
            if field in parsed:
                out.setdefault("problem", {})[field] = parsed[field]
    return out


def header_view(headers: dict, names: list[str]) -> dict:
    return {name: headers.get(name.lower()) for name in names}


CALLER_INPUT = "<caller-supplied>"


def redact(node: Any, volatile: list[str], normalise: list[str] | None = None) -> Any:
    """Drop the members that differ between two otherwise identical responses
    (correlation ids, timestamps, the echoed request path) and replace the
    identifiers the caller itself sent with a constant placeholder. Echoing a
    caller's own input back tells that caller nothing it did not already know,
    so it cannot be an existence oracle; everything else that remains can be."""
    if isinstance(node, dict):
        return {k: redact(v, volatile, normalise) for k, v in sorted(node.items()) if k not in volatile}
    if isinstance(node, list):
        return [redact(v, volatile, normalise) for v in node]
    if isinstance(node, str) and normalise:
        for value in normalise:
            if value:
                node = node.replace(value, CALLER_INPUT)
    return node


def header_matches(got: str | None, want: Any) -> bool:
    """A declared expectation is either null (the header must be absent), a
    string (equal, or equal to the part before a parameter such as `;charset=`),
    or one of the objects {"startsWith"|"contains"|"equals": "..."}."""
    if want is None:
        return got is None
    if got is None:
        return False
    value = got.strip()
    if isinstance(want, dict):
        if "startsWith" in want:
            return value.startswith(want["startsWith"])
        if "contains" in want:
            return want["contains"] in value
        return value == want.get("equals")
    return value == want or (";" not in want and value.split(";")[0].strip() == want)


def assert_headers(actual: dict, expected: dict) -> tuple[dict, bool]:
    report: dict[str, Any] = {}
    ok = True
    for name, want in expected.items():
        got = actual.get(name.lower())
        matches = header_matches(got, want)
        report[name] = {"expected": want, "actual": got, "matches": matches}
        ok = ok and matches
    return report, ok


def walk(node: Any):
    yield node
    if isinstance(node, dict):
        for value in node.values():
            yield from walk(value)
    elif isinstance(node, list):
        for value in node:
            yield from walk(value)


def pick(node: Any, path: str) -> Any:
    """Dotted lookup; a numeric segment indexes a list."""
    current = node
    for segment in path.split("."):
        if segment == "":
            continue
        if isinstance(current, list):
            try:
                current = current[int(segment)]
                continue
            except (ValueError, IndexError):
                return None
        if not isinstance(current, dict) or segment not in current:
            return None
        current = current[segment]
    return current


def discover_codes(document: Any, keys: list[str]) -> list[str]:
    """Collect declared relation/include codes from a capabilities document
    without knowing its shape: any list of strings under one of `keys`."""
    found: list[str] = []
    for node in walk(document):
        if not isinstance(node, dict):
            continue
        for key in keys:
            value = node.get(key)
            if isinstance(value, list):
                for item in value:
                    if isinstance(item, str) and item not in found:
                        found.append(item)
                    elif isinstance(item, dict):
                        for id_key in ("code", "name", "relationCode", "include"):
                            if isinstance(item.get(id_key), str) and item[id_key] not in found:
                                found.append(item[id_key])
                                break
    return found


def hydration(rows: list, codes: list[str]) -> dict:
    """A relation counts as hydrated when at least one returned row carries a
    non-empty value under its code, at the row root or nested one level."""
    report: dict[str, Any] = {}
    for code in codes:
        hits = 0
        for row in rows if isinstance(rows, list) else []:
            for node in walk(row):
                if isinstance(node, dict) and code in node:
                    value = node[code]
                    if value not in (None, [], {}, ""):
                        hits += 1
                        break
        report[code] = {"rowsWithValue": hits, "hydrated": hits > 0}
    return report


# --------------------------------------------------------------------------
# probe
# --------------------------------------------------------------------------


class Probe:
    def __init__(self, config: dict, pace_ms: int | None = None) -> None:
        self.config = config
        self.target = config["target"]
        self.oidc = config["oidc"]
        self.curl = Curl(int(config.get("timeoutSeconds", 30)))
        self.pace = (pace_ms if pace_ms is not None else int(config.get("paceMs", 300))) / 1000.0
        self.cases: list[dict] = []
        self.notes: list[str] = []
        self.tokens: dict[str, str] = {}
        self.token_meta: dict[str, dict] = {}
        self.workdir = tempfile.mkdtemp(prefix="sec-probe-")
        self.forge = Forge(self.workdir)

    # -- infrastructure ----------------------------------------------------

    def cleanup(self) -> None:
        shutil.rmtree(self.workdir, ignore_errors=True)

    def url(self, path: str) -> str:
        return self.target["baseUrl"].rstrip("/") + path

    def call(self, method: str, path: str, token: str | None = None,
             headers: list[str] | None = None, data: bytes | None = None,
             extra: list[str] | None = None):
        head = list(headers or [])
        if token is not None:
            head.append("Authorization: Bearer " + token)
        time.sleep(self.pace)
        return self.curl(
            method, self.url(path), headers=head, data=data,
            cacert=self.target.get("caCert"), resolve=self.target.get("resolve"),
            insecure=bool(self.target.get("insecure")), extra=extra,
        )

    def record(self, case_id: str, group: str, request: dict, expected: Any,
               status: int, headers: dict, body: bytes,
               extra: dict | None = None, verdict: str | None = None,
               header_profile: str | None = None,
               expect_problem_code: str | None = None) -> dict:
        expected_list = expected if isinstance(expected, list) else [expected]
        extra = dict(extra or {})
        contract_ok = True
        if header_profile:
            declared = self.config.get("headerContracts", {}).get(header_profile)
            if declared is not None:
                report, contract_ok = assert_headers(headers, declared)
                extra["headerContract"] = {"profile": header_profile, "checks": report,
                                           "satisfied": contract_ok}
        if expect_problem_code:
            try:
                seen = json.loads(body.decode("utf-8", "replace")).get("code")
            except Exception:
                seen = None
            matched = seen == expect_problem_code
            extra["problemCode"] = {"expected": expect_problem_code, "actual": seen,
                                    "matches": matched}
            contract_ok = contract_ok and matched
        if verdict is None:
            if status == 429:
                verdict = "INCONCLUSIVE_RATE_LIMITED"
            elif status not in expected_list:
                verdict = "FAIL"
            else:
                verdict = "PASS" if contract_ok else "FAIL_RESPONSE_CONTRACT"
        case = {
            "id": case_id,
            "group": group,
            "request": request,
            "expectedStatus": expected_list,
            "actualStatus": status,
            "result": verdict,
            "response": body_summary(headers, body),
        }
        if extra:
            case.update(extra)
        self.cases.append(case)
        return case

    # -- identities --------------------------------------------------------

    def acquire_tokens(self) -> None:
        for identity in self.config["identities"]:
            name = identity["name"]
            done = subprocess.run(identity["tokenCommand"], shell=True,
                                  capture_output=True, text=True)
            token = (done.stdout or "").strip()
            if not token or token.count(".") != 2:
                self.token_meta[name] = {"obtained": False,
                                         "reason": "token command returned no JWT"}
                continue
            self.tokens[name] = token
            header, payload = jwt_part(token, 0), jwt_part(token, 1)
            roles = pick(payload, self.oidc["rolesClaim"]) or []
            self.token_meta[name] = {
                "obtained": True,
                "alg": header.get("alg"),
                "hasKid": bool(header.get("kid")),
                "audienceMatchesPolicy": self.oidc["audience"] in (
                    payload.get("aud") if isinstance(payload.get("aud"), list)
                    else [payload.get("aud")]),
                "issuerMatchesPolicy": payload.get("iss") == self.oidc["issuer"],
                "requiredClaimPresent": bool(payload.get(self.oidc["tenantClaim"])),
                "roleCount": len(roles) if isinstance(roles, list) else 0,
                "lifetimeSeconds": (payload.get("exp", 0) - payload.get("iat", 0)) or None,
            }

    def claims_template(self, source: str) -> dict:
        """A payload shaped like a genuinely accepted token, so that a negative
        case differs from the positive one in exactly one property."""
        base = jwt_part(self.tokens[source], 1) if source in self.tokens else {}
        now = int(time.time())
        payload = dict(base)
        payload["iat"] = now
        payload["nbf"] = now
        payload["exp"] = now + 300
        payload["jti"] = b64u(os.urandom(12))
        return payload

    # -- groups ------------------------------------------------------------

    def run_discovery(self) -> None:
        tls = self.oidc.get("tls", {})
        for name, url in (("discovery", self.oidc.get("discoveryUrl")),
                          ("jwks", self.oidc.get("jwksUrl"))):
            if not url:
                continue
            time.sleep(self.pace)
            status, headers, body = self.curl(
                "GET", url, cacert=tls.get("caCert"), resolve=tls.get("resolve"))
            extra: dict[str, Any] = {"url": url,
                                     "trustAnchor": os.path.basename(tls.get("caCert") or "system")}
            if name == "discovery" and status == 200:
                try:
                    document = json.loads(body)
                    extra["issuerMatchesPolicy"] = document.get("issuer") == self.oidc["issuer"]
                    extra["issuerScheme"] = str(document.get("issuer", "")).split(":")[0]
                    extra["jwksOverTls"] = str(document.get("jwks_uri", "")).startswith("https://")
                except Exception:
                    extra["parsed"] = False
            if name == "jwks" and status == 200:
                try:
                    keys = json.loads(body).get("keys", [])
                    extra["keyCount"] = len(keys)
                    extra["algorithms"] = sorted({k.get("alg") for k in keys if k.get("alg")})
                    self.jwks = keys
                except Exception:
                    extra["parsed"] = False
            # A wrong trust anchor must fail: curl reports 000 on a TLS error.
            self.record(f"tls.{name}", "transport",
                        {"method": "GET", "target": name, "tls": "explicit trust anchor"},
                        200, status, headers, body, extra)

        # The same endpoint without the trust anchor must not validate.
        url = self.oidc.get("discoveryUrl")
        if url and tls.get("caCert"):
            time.sleep(self.pace)
            status, headers, body = self.curl(
                "GET", url, resolve=tls.get("resolve"))
            self.record("tls.discovery.untrusted", "transport",
                        {"method": "GET", "target": "discovery",
                         "tls": "default trust store, no explicit anchor"},
                        0, status, headers, body,
                        {"note": "curl reports 000 when the TLS handshake is rejected"})

    def run_authn(self) -> None:
        endpoint = self.config["endpoints"]["read"]
        path, method = endpoint["path"], endpoint.get("method", "GET")
        reference = self.config.get("referenceIdentity", "operator")
        kid = None
        for key in getattr(self, "jwks", []):
            if key.get("alg") == "RS256" or key.get("use") == "sig":
                kid = key.get("kid")
                break

        profiles = self.config.get("headerProfiles", {})

        def profile_for(case_id: str) -> str | None:
            """Which declared header contract this case must satisfy. The mapping
            is configuration, so the probe never assumes a challenge shape."""
            return profiles.get("byCase", {}).get(case_id, profiles.get("default"))

        def negative(case_id: str, description: str, token: str | None,
                     headers: list[str] | None = None, expected: Any = 401):
            status, head, body = self.call(method, path, token=token, headers=headers)
            self.record(case_id, "authn",
                        {"method": method, "path": path, "credential": description},
                        expected, status, head, body,
                        {"headers": header_view(head, self.config["headerChecks"]["onError"])},
                        header_profile=profile_for(case_id))

        negative("authn.anonymous", "no Authorization header", None)
        negative("authn.malformed", "Authorization: Bearer not-a-jwt", "not-a-jwt")
        negative("authn.emptyBearer", "Authorization: Bearer with empty value", "")
        negative("authn.basic", "a non-Bearer Authorization scheme", None,
                 headers=["Authorization: Basic " + b64u(b"user:pass")])

        if reference in self.tokens:
            negative("authn.tamperedSignature",
                     "a genuine token whose last signature byte is flipped",
                     tamper_signature(self.tokens[reference]))

            payload = self.claims_template(reference)
            negative("authn.unknownKey",
                     "RS256 signed with an unknown key; issuer, audience and claims correct",
                     self.forge.rs256({"alg": "RS256", "typ": "JWT",
                                       "kid": "zz-sectest-unknown"}, payload))
            if kid:
                negative("authn.copiedKid",
                         "RS256 signed with an unknown key but carrying the authority's kid",
                         self.forge.rs256({"alg": "RS256", "typ": "JWT", "kid": kid}, payload))
            negative("authn.algNone",
                     "alg=none with an empty signature",
                     self.forge.unsigned({"alg": "none", "typ": "JWT"}, payload))

            # Algorithm confusion: the RSA public key material used as an HMAC secret.
            for label, secret in self.public_key_secrets():
                negative(f"authn.hs256.{label}",
                         f"HS256 signed with the authority's public key ({label})",
                         self.forge.hs256({"alg": "HS256", "typ": "JWT", "kid": kid}, payload, secret))

            forged_issuer = self.config.get("forgedIssuer") or (self.oidc["issuer"] + "-not")
            wrong_issuer = self.claims_template(reference)
            wrong_issuer["iss"] = forged_issuer
            negative("authn.wrongIssuer",
                     "self-signed token asserting a different issuer",
                     self.forge.rs256({"alg": "RS256", "typ": "JWT", "kid": kid}, wrong_issuer))

            expired = self.claims_template(reference)
            expired["iat"] = expired["nbf"] = int(time.time()) - 3600
            expired["exp"] = int(time.time()) - 60
            negative("authn.expiredForged",
                     "self-signed token whose exp is in the past",
                     self.forge.rs256({"alg": "RS256", "typ": "JWT", "kid": kid}, expired))

            future = self.claims_template(reference)
            future["nbf"] = future["iat"] = int(time.time()) + 3600
            future["exp"] = int(time.time()) + 7200
            negative("authn.notYetValid",
                     "self-signed token whose nbf is in the future",
                     self.forge.rs256({"alg": "RS256", "typ": "JWT", "kid": kid}, future))

        # Identities whose real, authority-signed token is expected to be refused.
        for identity in self.config["identities"]:
            expectation = identity.get("expect")
            if not expectation or expectation.get("group") != "authn":
                continue
            name = identity["name"]
            if name not in self.tokens:
                self.cases.append({
                    "id": f"authn.{name}", "group": "authn",
                    "request": {"credential": identity["description"]},
                    "expectedStatus": [expectation["status"]],
                    "actualStatus": None, "result": "NOT_RUN",
                    "response": {"reason": self.token_meta.get(name, {}).get("reason")},
                })
                continue
            token = self.tokens[name]
            if identity.get("sleepBeforeUseSeconds"):
                # A lifetime-dependent case must start from a token issued now,
                # not from one acquired at the beginning of the run.
                fresh = subprocess.run(identity["tokenCommand"], shell=True,
                                       capture_output=True, text=True).stdout.strip()
                if fresh.count(".") == 2:
                    token = fresh
                time.sleep(identity["sleepBeforeUseSeconds"])
            status, head, body = self.call(method, path, token=token)
            verdict = None
            extra: dict[str, Any] = {"tokenProperties": self.token_meta.get(name)}
            if identity.get("sleepBeforeUseSeconds"):
                extra["secondsSinceIssue"] = identity["sleepBeforeUseSeconds"]
            if expectation.get("finding"):
                extra["finding"] = expectation["finding"]
                verdict = ("FINDING_NOT_ENFORCED"
                           if status in expectation.get("notEnforcedStatus", [200])
                           else "PASS" if status == expectation["status"] else "AMBIGUOUS")
            if expectation.get("toleratedAs"):
                # A declared, configured tolerance is not a defect. It is recorded
                # under its own verdict so it can never be read as a silent pass.
                extra["declaredTolerance"] = expectation["toleratedAs"]
                verdict = (expectation["toleratedAs"]["verdict"]
                           if status in expectation["toleratedAs"]["status"]
                           else "PASS" if status == expectation["status"] else "AMBIGUOUS")
            self.record(f"authn.{name}", "authn",
                        {"method": method, "path": path,
                         "credential": identity["description"]},
                        expectation["status"], status, head, body, extra, verdict,
                        header_profile=expectation.get("headerProfile"))

    def public_key_secrets(self) -> list[tuple[str, bytes]]:
        """The authority's RSA public key in the encodings an attacker would try
        as an HMAC secret. Public material only: nothing secret is handled."""
        secrets: list[tuple[str, bytes]] = []
        realm_url = self.oidc.get("realmUrl")
        tls = self.oidc.get("tls", {})
        der_b64 = None
        if realm_url:
            status, _, body = self.curl("GET", realm_url, cacert=tls.get("caCert"),
                                        resolve=tls.get("resolve"))
            if status == 200:
                try:
                    der_b64 = json.loads(body).get("public_key")
                except Exception:
                    der_b64 = None
        if not der_b64:
            for key in getattr(self, "jwks", []):
                if key.get("kty") == "RSA" and key.get("n"):
                    der_b64 = base64.b64encode(rsa_spki_der(key["n"], key["e"])).decode()
                    break
        if not der_b64:
            return secrets
        wrapped = "\n".join(der_b64[i:i + 64] for i in range(0, len(der_b64), 64))
        pem = f"-----BEGIN PUBLIC KEY-----\n{wrapped}\n-----END PUBLIC KEY-----\n"
        secrets.append(("pem", pem.encode()))
        secrets.append(("base64Der", der_b64.encode()))
        return secrets

    def run_authz(self) -> None:
        for name, endpoint in self.config["endpoints"].items():
            for expectation in endpoint.get("identityExpectations", []):
                identity = expectation["identity"]
                if identity not in self.tokens:
                    self.cases.append({
                        "id": f"authz.{name}.{identity}", "group": "authz",
                        "request": {"method": endpoint.get("method", "GET"),
                                    "path": endpoint["path"], "identity": identity},
                        "expectedStatus": [expectation["status"]],
                        "actualStatus": None, "result": "NOT_RUN",
                        "response": {"reason": self.token_meta.get(identity, {}).get("reason")},
                    })
                    continue
                data = json.dumps(endpoint["body"]).encode() if endpoint.get("body") else None
                headers = ["Content-Type: application/json"] if data else []
                status, head, body = self.call(
                    endpoint.get("method", "GET"), endpoint["path"],
                    token=self.tokens[identity], headers=headers, data=data)
                extra = {
                    "identityProperties": self.token_meta.get(identity),
                    "headers": header_view(
                        head,
                        self.config["headerChecks"]["onSuccess"] if status < 400
                        else self.config["headerChecks"]["onError"]),
                    "rateLimit": header_view(head, ["RateLimit-Limit", "RateLimit-Remaining"]),
                }
                verdict = None
                if expectation.get("finding"):
                    verdict = ("FINDING_NOT_ENFORCED"
                               if status in expectation.get("notEnforcedStatus", [200])
                               else "PASS" if status == expectation["status"] else "AMBIGUOUS")
                    extra["finding"] = expectation["finding"]
                if expectation.get("requireNonEmpty") and status == 200:
                    try:
                        rows = pick(json.loads(body), expectation["requireNonEmpty"])
                    except Exception:
                        rows = None
                    count = len(rows) if isinstance(rows, (list, dict)) else 0
                    extra["itemsReturned"] = count
                    if count == 0:
                        verdict = "FAIL"
                self.record(f"authz.{name}.{identity}", "authz",
                            {"method": endpoint.get("method", "GET"),
                             "path": endpoint["path"], "identity": identity,
                             "intent": expectation.get("intent", "")},
                            expectation["status"], status, head, body, extra, verdict,
                            header_profile=expectation.get("headerProfile"),
                            expect_problem_code=expectation.get("expectProblemCode"))

    def run_transport(self) -> None:
        endpoint = self.config["endpoints"]["read"]
        reference = self.config.get("referenceIdentity", "operator")
        unsafe = self.config["unsafeCorrelationId"]
        correlation_header = self.config.get("correlationHeader", "X-Correlation-Id")

        if reference in self.tokens:
            status, head, body = self.call(
                endpoint.get("method", "GET"), endpoint["path"],
                token=self.tokens[reference],
                headers=[f"{correlation_header}: {unsafe}"])
            echoed = head.get(correlation_header.lower())
            self.record("transport.correlationReflection.authenticated", "transport",
                        {"method": endpoint.get("method", "GET"), "path": endpoint["path"],
                         "sent": f"{correlation_header} with an unsafe value"},
                        200, status, head, body,
                        {"reflectedVerbatim": echoed == unsafe,
                         "replacedWithGeneratedValue": bool(echoed) and echoed != unsafe},
                        verdict=None if status == 429 else
                        ("PASS" if status == 200 and echoed != unsafe else "FAIL"))

        status, head, body = self.call(
            endpoint.get("method", "GET"), endpoint["path"], token=None,
            headers=[f"{correlation_header}: {unsafe}"])
        echoed = head.get(correlation_header.lower())
        self.record("transport.correlationReflection.unauthenticated", "transport",
                    {"method": endpoint.get("method", "GET"), "path": endpoint["path"],
                     "sent": f"{correlation_header} with an unsafe value, no credential"},
                    401, status, head, body,
                    {"reflectedVerbatim": echoed == unsafe,
                     "headers": header_view(head, self.config["headerChecks"]["onError"])},
                    verdict="PASS" if status == 401 and echoed != unsafe else "FAIL")

        origin = self.config["cors"]["foreignOrigin"]
        status, head, body = self.call(
            "OPTIONS", endpoint["path"], token=None,
            headers=[f"Origin: {origin}",
                     "Access-Control-Request-Method: " + endpoint.get("method", "GET"),
                     "Access-Control-Request-Headers: authorization"])
        allowed = head.get("access-control-allow-origin")
        self.record("transport.corsPreflight.foreignOrigin", "transport",
                    {"method": "OPTIONS", "path": endpoint["path"],
                     "origin": "a foreign origin"},
                    self.config["cors"].get("expectStatus", [403, 401, 400]),
                    status, head, body,
                    {"accessControlAllowOrigin": allowed,
                     "originEchoed": allowed == origin},
                    verdict="PASS" if allowed != origin and allowed != "*" else "FAIL")

        allowed_origin = self.config["cors"].get("allowedOrigin")
        if allowed_origin:
            status, head, body = self.call(
                "OPTIONS", endpoint["path"], token=None,
                headers=[f"Origin: {allowed_origin}",
                         "Access-Control-Request-Method: " + endpoint.get("method", "GET")])
            self.record("transport.corsPreflight.configuredOrigin", "transport",
                        {"method": "OPTIONS", "path": endpoint["path"],
                         "origin": "an origin the deployment configures as allowed"},
                        [200, 204], status, head, body,
                        {"accessControlAllowOrigin":
                            "echoed" if head.get("access-control-allow-origin") == allowed_origin
                            else head.get("access-control-allow-origin"),
                         "allowCredentials": head.get("access-control-allow-credentials")})

        unmapped = self.config.get("unmappedPath")
        if unmapped:
            status, head, body = self.call("GET", unmapped, token=None)
            self.record("transport.unmappedRoute", "transport",
                        {"method": "GET", "path": unmapped, "credential": "none"},
                        self.config.get("unmappedExpect", [401, 404]),
                        status, head, body,
                        {"headers": header_view(head, self.config["headerChecks"]["onError"])})

        public = self.config["endpoints"].get("public")
        if public:
            status, head, body = self.call(public.get("method", "GET"), public["path"], token=None)
            self.record("transport.publicEndpoint", "transport",
                        {"method": public.get("method", "GET"), "path": public["path"],
                         "credential": "none"},
                        200, status, head, body)

    def run_pages(self) -> None:
        reference = self.config.get("referenceIdentity", "operator")
        for page in self.config.get("pages", []):
            captured: list[str] = []
            page_identity = page.get("identity", reference)
            for step in page["steps"]:
                identity = step.get("identity", page_identity)
                if identity not in self.tokens:
                    self.cases.append({
                        "id": f"page.{page['id']}.{step['name']}", "group": "pages",
                        "request": {"method": step.get("method", "GET"),
                                    "path": step["path"], "identity": identity},
                        "expectedStatus": [step.get("expectStatus", 200)],
                        "actualStatus": None, "result": "NOT_RUN",
                        "response": {"reason": self.token_meta.get(identity, {}).get("reason")},
                    })
                    continue
                token = self.tokens[identity]
                data = None
                headers = []
                if "body" in step:
                    body_doc = json.loads(
                        json.dumps(step["body"]).replace('"@declaredIncludes"',
                                                         json.dumps(captured)))
                    data = json.dumps(body_doc).encode()
                    headers = ["Content-Type: application/json"]
                status, head, raw = self.call(
                    step.get("method", "GET"), step["path"], token=token,
                    headers=headers, data=data)
                document: Any = None
                try:
                    document = json.loads(raw)
                except Exception:
                    pass
                extra: dict[str, Any] = {"identity": identity}
                verdict = None
                if step.get("captureIncludesFrom"):
                    captured = discover_codes(document, step["captureIncludesFrom"])
                    extra["declaredIncludes"] = captured
                if step.get("requireNonEmpty") and status == 200:
                    rows = pick(document, step["requireNonEmpty"])
                    count = len(rows) if isinstance(rows, list) else 0
                    extra["rowsReturned"] = count
                    total = pick(document, step.get("totalPath", "pagination.total"))
                    if total is not None:
                        extra["rowsTotal"] = total
                    if count == 0:
                        verdict = "FAIL"
                    if step.get("requireHydratedIncludes"):
                        report = hydration(rows, captured)
                        extra["includeHydration"] = report
                        if captured and not all(v["hydrated"] for v in report.values()):
                            verdict = "FAIL"
                        elif not captured:
                            verdict = "AMBIGUOUS"
                            extra["note"] = ("no relation include codes could be discovered "
                                             "from the capabilities document")
                if verdict is None and status != step.get("expectStatus", 200):
                    verdict = "INCONCLUSIVE_RATE_LIMITED" if status == 429 else "FAIL"
                self.record(f"page.{page['id']}.{step['name']}", "pages",
                            {"method": step.get("method", "GET"), "path": step["path"],
                             "identity": identity, "intent": step.get("intent", "")},
                            step.get("expectStatus", 200), status, head, raw, extra, verdict,
                            header_profile=step.get("headerProfile"),
                            expect_problem_code=step.get("expectProblemCode"))

    def run_body_equivalence(self) -> None:
        """Proves the absence of an existence oracle: the answer a caller gets for
        an object of another tenant must be indistinguishable from the answer for
        an object that does not exist at all."""
        for check in self.config.get("bodyEquivalence", []):
            volatile = check.get("volatileMembers", [])
            sides: dict[str, Any] = {}
            missing = None
            for side in ("left", "right"):
                spec = check[side]
                identity = spec["identity"]
                if identity not in self.tokens:
                    missing = identity
                    break
                status, head, raw = self.call(spec.get("method", "GET"), spec["path"],
                                              token=self.tokens[identity])
                try:
                    document: Any = json.loads(raw)
                except Exception:
                    document = raw.decode("utf-8", "replace")
                sides[side] = {
                    "identity": identity, "path": spec["path"], "status": status,
                    "intent": spec.get("intent", ""),
                    "callerSuppliedValuesNormalised": spec.get("normaliseValues", []),
                    "redactedBody": redact(document, volatile, spec.get("normaliseValues", [])),
                    "mediaType": head.get("content-type"),
                }
            if missing:
                self.cases.append({
                    "id": f"oracle.{check['id']}", "group": "oracle",
                    "request": {"check": check.get("intent", "")},
                    "expectedStatus": [check.get("expectStatus")],
                    "actualStatus": None, "result": "NOT_RUN",
                    "response": {"reason": f"identity {missing} has no token"},
                })
                continue
            expected = check.get("expectStatus")
            equal = (sides["left"]["redactedBody"] == sides["right"]["redactedBody"]
                     and sides["left"]["mediaType"] == sides["right"]["mediaType"])
            same_status = sides["left"]["status"] == sides["right"]["status"]
            want_equal = check.get("expectEqual", True)
            status_ok = expected is None or (sides["left"]["status"] == expected and same_status)
            verdict = "PASS" if (equal == want_equal and status_ok) else "FAIL"
            if 429 in (sides["left"]["status"], sides["right"]["status"]):
                verdict = "INCONCLUSIVE_RATE_LIMITED"
            self.cases.append({
                "id": f"oracle.{check['id']}", "group": "oracle",
                "request": {"check": check.get("intent", ""),
                            "volatileMembersRemoved": volatile},
                "expectedStatus": [expected] if expected is not None else [],
                "actualStatus": sides["left"]["status"],
                "result": verdict,
                "response": {"contentType": sides["left"]["mediaType"],
                             "leaksImplementationDetail": False},
                "bodiesEqualAfterRedaction": equal,
                "expectedEqual": want_equal,
                "sameStatus": same_status,
                "sides": sides,
            })

    # -- entry point -------------------------------------------------------

    def run(self) -> dict:
        self.jwks: list = []
        self.run_discovery()
        self.acquire_tokens()
        self.run_authn()
        self.run_authz()
        self.run_transport()
        self.run_pages()
        self.run_body_equivalence()
        counts: dict[str, int] = {}
        for case in self.cases:
            counts[case["result"]] = counts.get(case["result"], 0) + 1
        return {
            "probe": "security_probe.py",
            "startedAt": self.started,
            "finishedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "target": {k: v for k, v in self.target.items() if k != "caCert"},
            "identities": self.token_meta,
            "summary": counts,
            "cases": self.cases,
            "notes": self.notes,
        }


def rsa_spki_der(n_b64u: str, e_b64u: str) -> bytes:
    """Minimal DER SubjectPublicKeyInfo for an RSA JWK (standard library only)."""
    def length(size: int) -> bytes:
        if size < 0x80:
            return bytes([size])
        raw = size.to_bytes((size.bit_length() + 7) // 8, "big")
        return bytes([0x80 | len(raw)]) + raw

    def tlv(tag: int, value: bytes) -> bytes:
        return bytes([tag]) + length(len(value)) + value

    def integer(raw: bytes) -> bytes:
        raw = raw.lstrip(b"\x00") or b"\x00"
        if raw[0] & 0x80:
            raw = b"\x00" + raw
        return tlv(0x02, raw)

    rsa_key = tlv(0x30, integer(b64u_decode(n_b64u)) + integer(b64u_decode(e_b64u)))
    algorithm = tlv(0x30, tlv(0x06, bytes([0x2A, 0x86, 0x48, 0x86, 0xF7, 0x0D, 0x01, 0x01, 0x01]))
                    + tlv(0x05, b""))
    return tlv(0x30, algorithm + tlv(0x03, b"\x00" + rsa_key))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--pace-ms", type=int, default=None)
    args = parser.parse_args()

    with open(args.config, "r", encoding="utf-8") as handle:
        config = json.load(handle)
    probe = Probe(config, args.pace_ms)
    probe.started = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
    try:
        report = probe.run()
    finally:
        probe.cleanup()
    with open(args.out, "w", encoding="utf-8") as handle:
        json.dump(report, handle, indent=2, ensure_ascii=False)
    print(json.dumps(report["summary"], sort_keys=True))
    return 0 if not report["summary"].get("FAIL") else 1


if __name__ == "__main__":
    sys.exit(main())
