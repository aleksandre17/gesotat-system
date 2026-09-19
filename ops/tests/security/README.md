# `ops/tests/security` — runtime security acceptance suite

A site-agnostic suite that proves, against a **running** deployment, what an
OIDC-protected API actually enforces: who is refused, who is allowed, what the
error surface reveals, whether the authority is reachable over TLS, and whether
the contract pages a release gate names still answer.

Nothing in the three executables names a project, host, realm, endpoint, role,
claim, contract or page. Every one of those is configuration.

| File | Role |
|---|---|
| `security_probe.py` | the suite. Runs **on** the target's own host, forges tokens with the standard library plus the `openssl` CLI, drives one HTTP transport (`curl`) so TLS trust anchors and host overrides are expressed identically everywhere, and writes a redacted result document |
| `keycloak_test_clients.py` | creates and removes the throwaway identities the negative cases need, inside the identity provider's container. Bounded by a name prefix; `delete` verifies afterwards that nothing with the prefix is left |
| `remote-security-suite.sh` | the wrapper: copies the two scripts and the rendered configuration to the server, turns the reference client's secret into a server-side `0600` token request file, runs a phase, copies the result back, and tears everything down |
| `config/dev-api.json` | what the deployment is: base URL, OIDC policy, identities, endpoints and their expected authority outcomes, CORS origins, header checks, page replays |
| `config/throwaway-clients.json` | the identities to create in the realm, each one differing from a valid identity in exactly one property |
| `out/` | results (gitignored) |

## What it checks

**Authentication** — anonymous · malformed bearer · empty bearer · a non-Bearer
scheme · a genuine token with one signature byte flipped · RS256 signed with an
unknown key · the same carrying the authority's real `kid` · `alg=none` ·
HS256 signed with the authority's own RSA public key, in PEM and base64-DER
form (algorithm confusion) · a forged issuer · `exp` in the past · `nbf` in the
future · a real authority-signed token without the required audience · one
without the required claim · a real token used after its lifetime elapsed.

**Authorization** — a valid privileged identity reads; an identity with no role
is refused on the same endpoint, on a mutating endpoint and on id-addressed
routes; a privileged identity is refused on an endpoint demanding a higher
authority.

**Tenancy** — the part that needs the most care, because a blanket deny would
make every negative case pass. Every tenant denial is therefore paired with a
**positive control**: an identity holding the ordinary role and the *owning*
tenant, and explicitly **not** the cross-tenant authority, must still get its
data. Foreign-tenant identities are then checked on each boundary for the shape
that boundary declares — a masked 404 where the unknown-object answer is a 404,
a 403 problem document with a named code where it is not.

**Existence oracle** (`bodyEquivalence`) — a 404 that masks another tenant's
object is only a mask if it is *indistinguishable* from the 404 for an object
that never existed. The suite fetches both, removes the volatile members a
configuration names (correlation id, timestamp, echoed instance/path) and
replaces the identifier the caller itself supplied with a constant, then
compares status, media type and the whole remaining document. Each check runs
twice: once with the identity held constant — the attacker's real position,
varying only existence — and once across identities. Normalising the caller's
own input is sound: echoing back a key the caller just sent tells it nothing it
did not already know. Everything else, including the problem `type`, `title`,
`code` and `detail` template, is compared verbatim.

**Transport** — hardening headers on both a protected 200 and a 401 · the error
media type · whether any body leaks a stack trace, exception text or SQL · what
happens to an unsafe `X-Correlation-Id` (it must not come back verbatim) · a
CORS preflight from a foreign origin, with a configured origin as the positive
control · an unmapped route · the public endpoint.

**Response contracts** (`headerContracts` + `headerProfiles`) — a case may
declare the exact error surface it must produce: media type, the RFC 6750
challenge, `Cache-Control`, `nosniff`, and the RFC 9457 `code`. An expectation
is `null` (the header must be absent), a string (equal, or equal ignoring a
`;charset=` parameter) or `{"startsWith"|"contains"|"equals": "…"}`. A case
whose **status** is right but whose declared surface is not comes back as
`FAIL_RESPONSE_CONTRACT`, so a correct decision delivered in the wrong shape is
never silently counted as a pass — and the full actual header is recorded
either way.

**Discovery** — OIDC discovery and JWKS over TLS with an explicit trust anchor,
and the same endpoint *without* it, which must fail the handshake.

**Pages** — the introspection, capabilities and query steps a release gate
names. The probe reads the relation codes a revision declares from the live
capabilities document rather than from a literal, then asserts that each one is
hydrated in the returned rows.

## Secrets

The probe never receives a client secret. Each identity is configured with a
*token command*: a shell command that prints an access token on stdout. Tokens
live in process memory only. The result document records a token's
non-sensitive metadata — algorithm, whether a `kid` is present, whether the
audience and issuer match policy, whether the required claim is present, the
role count, the lifetime in seconds — and never a token, a signature or a claim
value.

The reference client's secret is read from the gitignored env file, travels to
the server on **stdin only**, and lands in a `0600` file. A throwaway client's
generated secret is read *inside* the provider container and written straight
into its own `0600` token request file; it is never printed and never returned.
`teardown` removes the server-side directory.

## Throwaway identities

The realm may be shared with other environments, so `keycloak_test_clients.py`:

- refuses any client id that does not carry the configured prefix;
- refuses to touch a client that already exists — it only ever creates its own;
- never modifies an existing client, role, mapper, client scope, user or realm
  setting (it assigns existing realm roles to the service accounts it created,
  and nothing else);
- reports, per client, whether it existed before the delete and whether it
  exists after it, plus the full list of clients still carrying the prefix.

## Invocations actually used on 2026-09-19

Evidence: `docs/evidence/security-acceptance-runtime-2026-09-19.json`, which
holds both runs — `run-1` before tenant enforcement was deployed, `run-2` after.

```bash
# run-1 (41 cases) — baseline, before tenant ABAC
ops/tests/security/remote-security-suite.sh setup
ops/tests/security/remote-security-suite.sh run acceptance-2026-09-19
ops/tests/security/remote-security-suite.sh clients-verify
ops/tests/security/remote-security-suite.sh teardown

# run-2 (60 cases) — tenant enforcement deployed, positive controls added
ops/tests/security/remote-security-suite.sh setup
ops/tests/security/remote-security-suite.sh run security-acceptance-r2-2026-09-19
ops/tests/security/remote-security-suite.sh clients-verify
ops/tests/security/remote-security-suite.sh teardown
```

350 ms pacing; about three minutes of wall clock including the two expiry waits.
Defaults come from the environment variables documented at the top of
`remote-security-suite.sh`; `SUITE_CONFIG` and `SUITE_CLIENT_SPEC` point the
same suite at a different deployment.

Result vocabulary:

| Verdict | Meaning |
|---|---|
| `PASS` | the status and, where declared, the header contract and problem code were met |
| `FAIL` | the status was wrong, or an assertion about the body failed |
| `FAIL_RESPONSE_CONTRACT` | the decision was right, the declared response surface was not |
| `WITHIN_DECLARED_SKEW` | accepted inside a tolerance the deployment declares in configuration |
| `FINDING_NOT_ENFORCED` | the deployment answered as if the control does not exist |
| `AMBIGUOUS` | the observation supports neither verdict |
| `INCONCLUSIVE_RATE_LIMITED` | the admission guard answered instead of the control under test |
| `NOT_RUN` | the case could not be executed |

Nothing in this suite is ever tuned until it passes. A control that is absent, a
tolerance that is configured and a shape that is wrong each get their own
verdict so they cannot be confused with one another.

## Budget

Run-2 is 60 requests across nine identity buckets. The deployment admits 120 per
60 s per client key, so the suite stays far inside it; a 429 is surfaced as
`INCONCLUSIVE_RATE_LIMITED` and never confused with an authorization outcome.
Load behaviour belongs to `ops/tests/load`, not here.
