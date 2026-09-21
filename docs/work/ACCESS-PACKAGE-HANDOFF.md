# Access package — handoff for the next session

Read this first, then `ACCESS-PACKAGE-MASTER-CHECKLIST.md`. It is written so that a new session can continue exactly
where this one stopped, without the conversation. Language: English on purpose (exact). Last updated: 2026-09-20.

## 1. What the owner wants (in their words, reduced)

One Access package — **the same package as the approved R8**, not a separate statistics file — in which the statistical
part is easy to fill, agnostic, and built on international grammar; everything categorised into "sheets" (what a person
fills, what is system, what is classifier, what is source trail); whatever can be filled automatically is; column names
are the standard ones (SDMX), not Georgian; table names keep their canonical prefixes; the link between raw data and
statistics is kept **inside the file, always, never breaking**; and the structure must be reusable on another site with
no code change. Standard of work: `AGENTS.md`, no less anywhere.

**The finish line, as the owner defined it on 2026-09-20:** first bring the Access file to the ideal, everything
considered; then go to the top layer - update the new contract - and run **the full path end to end: import, write
to the database, serve on the API.** The work is not done without that path.

Progress on that path (2026-09-20): the numeric envelope is done and live on dev - scale up to 16, columns
DECIMAL(38,16), proven lossless (evidence/statistical-numeric-envelope-2026-09-20.json). Dev chain now ends at 112.

Import and database are proven end to end with exact values: contract `6f6b3080-…` (`KIDS:KIDS_INDICATORS` 3.0.0,
`OBS_VALUE` (28,16)) -> authoring file -> filled from R8's lexical text -> load -> **snapshot 61: 880 of 880 exact,
including all 221 values with 11-16 decimals**; gates releasable
(evidence/kids-indicators-exact-end-to-end-2026-09-20.json). **Do not call the finish line reached.** On 2026-09-20 I told the owner "the full path is closed"; that was an overstatement and the owner called it out. What is true: **serving on the API is proven too**: the read endpoint returns the 880 rows of
snapshot 61, all exact, including the 221 long values. The path import -> database -> API is closed for the server's
statistics authoring file. What remains is to make the *unified package* (candidate 3) travel that same path - phases 4
(per-code attribute, classification API, site contract revision) and 5 (composer).

Known obstacle on that path: candidate 3 is not loadable by today's server. `/loads` expects the stamp table
`__stat_contract` and a table named `stat_<DSD>` holding plain codes; the candidate has `__stat_observation` holding
pinned item references and no stamp, and needs scale 16 where the server stores DECIMAL(28,10). Closing that gap *is*
phases 4 and 5 (contract revision, numeric envelope, composer reading the site contract).

## 2. Where things stand

Status: **OPEN / NOT READY.** A contract-less *candidate* exists and is proven as far as a file can be proven.

| Item | Value |
|---|---|
| Candidate | `C:\Users\Test-User\Desktop\KIDS_PACKAGE_candidate_3.accdb`, sha256 `751AA16B…5BF3` |
| Source it is built from | `platform/apps/geostat/backend/api/kids-portal-v1-canonical-r8-final.accdb` (approved R8) |
| Tools | `ops/scripts/java/prototype/` (README there) |
| Governance card | `docs/work/cards/access-package-composer/governance.json` — validator PASS |
| Checklist | `docs/work/ACCESS-PACKAGE-MASTER-CHECKLIST.md` — phases 0 and 2 closed, 1 decided (ADR-012) |
| Design / standard | `ACCESS-PACKAGE-COMPOSER-DESIGN.md`, `ACCESS-PACKAGE-CANONICAL-STANDARD.md` |
| Independent audit (Codex) | `docs/work/evidence/kids-package-full-audit-2026-09-20/` — 11 findings -> 7; response next to it |
| Evidence | `docs/work/evidence/access-package-candidate-3-2026-09-20.json` |

The 7 remaining audit findings cannot be fixed inside a file: 4 are required-table names (need a versioned contract
revision), 3 are R8's own DRAFT projections (need a steward).

**Phase 5 has started (2026-09-20).** The bridge between the owner's file and the server is one plan that both
directions read. Done: `org.base.api.service.platform.packaging` - `PackagePlan` (immutable, digested),
`PackageContractSource` + `JdbcPackageContractSource` (reads an approved site contract revision; refuses an unapproved
revision, an unknown family, a dangling relation, a dataset without a key), `AuthoringPolicy` (who fills a field, which
group shows a table - from metadata alone). 6 of 6 tests on a non-KIDS site. No bean is wired yet: the running product
is unchanged. The real KIDS r8 contract on dev satisfies every invariant (15 datasets, 21 relations). Open finding: the
Control Plane declares 104 fields for r8, the R8 file 122. Next: 5.2 family sections + provider writer (port the
prototype's BuildPackage logic, not its KIDS names), 5.3 the loader reading the same plan.

## 3. How to rebuild the candidate (exact order)

Windows, Git Bash for Java, PowerShell 7 for Access. Jackcess classpath: build it as in the README; Java:
`C:\Users\Test-User\.jdks\corretto-17.0.20.1\bin\java.exe`.

1. `java -cp <cp> BuildPackage.java <R8.accdb> <out.accdb>` — prints `tables=32 … relationships=39 observations=880`.
2. `java -cp <cp> VerifyPackage.java <R8.accdb> <out.accdb>` — must end `VERDICT: PASS`.
3. `java -cp <cp> Audit.java <R8.accdb> <out.accdb>` — must end `unaccounted=0 broken targets=0`.
4. `pwsh codelist_views.ps1 -File <out>` then `java … LinkViews.java <out>`.
5. `pwsh provenance_macros.ps1 -File <out>` — must end `WARMED UP … traces left behind = 0`.
6. `pwsh see-access.ps1 -File <out> -Png <png> -Category GEOSTAT` — sets the startup view; **read the PNG**.
7. Copy `<out>` and run `pwsh provenance_test.ps1 -File <copy>` — must end `VERDICT: PASS`; repeat on 3 fresh copies.
8. `gradlew :api:auditAccessArtifact -PaccessArtifact=<out>` — expected 7 findings (see §2).
9. `java … ContentDigest.java <genA> <genB>` — two generations must print the same digest.

Never deliver a file without steps 2, 6 (looked at) and 7.

## 4. Traps already paid for (do not pay twice)

- Real Microsoft Access 16 is installed here. Open every generated file through COM and capture its window before
  handing it over; Jackcess reading a file proves nothing about what a person sees.
- Georgian text: **never type it free-hand** — it drifts into Cyrillic within a paragraph. Write a Latin
  transliteration and convert with `ops/scripts/java/prototype/ka.py` (it asserts zero Cyrillic). In chat, keep Georgian
  replies short, generate them with `ka.py`, `Read` the result, then copy it. Java sources stay pure ASCII (`\uXXXX`).
- Bash heredocs eat backslashes and choke on some quotes: write scripts with the Write tool, then run them.
- A piped command hides a failed build: use `set -o pipefail` and check the verdict before copying a file anywhere.
- Provider facts F-1…F-7 in the standard (navigation groups, `Like "####"`, pipes in expressions, qualified field
  names in data macros, the first-run `USysApplicationLog` failure, DAO SQL literals with pipes).
- **Never call `POST /platform/publication/publish` on the shared dev product to demonstrate one dataset.** It is a
  product-wide release: it publishes every `REVIEW_REQUIRED` snapshot of the product (other people's too) and replaces
  the current publication (AIR-2026-054). On 2026-09-20 this displaced publication 14 for 90 seconds; it was restored
  with `POST /platform/publication/rollback {productId:1,targetPublicationSnapshotId:14}`. Residue: dataset snapshots
  53, 57-61 keep status PUBLISHED; **snapshot 53 belonged to someone else**.
- To ship to dev safely: back up the server copies, ship, **compile inside the running container**
  (`docker exec geostat-api-dev sh -c 'cd /app && ./gradlew :api:compileJava -x test --no-daemon -q'`, exit 0), then restart.
- **Before shipping any file to dev:** diff it against `HEAD`, back up the server's copy, and ship only files whose
  every dependency is already deployed. On 2026-09-20 a working-tree configuration that wired undeployed chart classes
  took dev down for 13 minutes. The server checkout is `~/geostat/backend/workspace/geostat-api-dev`; the container
  compiles at start (`docker restart geostat-api-dev`), so a compile error is an outage. Its migration runner is
  patched in place (it must not carry the other session's `105` line).
- The Desktop file may be open in Access (a `.laccdb` next to it): deliver under a new name rather than overwrite.
- Before modelling anything Access-related, open the R8 package. It holds truths the Control Plane lacks
  (`__stat_metric`: real codes, ka/en titles, units, aggregation NONE, one `OBS_VALUE`).

## 5. Mistakes of this session on dev (phase 3 — cleaned as far as the API allows; see evidence/access-package-dev-hygiene-2026-09-20.json)

Dev server `administrator@192.168.1.199`, API `localhost:8081`, helper `ops/scripts/shell/artifact-operator-api.sh`
(identities `ARTIFACT_OPERATOR`, `CONTRACT_APPROVER`). **Do not source `.env.dev` into a shell** (AIR-2026-049).

- Contracts on product `KIDS_PORTAL`: `bfe98140-…` and `6d3e6ba2-…` (dataset key `KIDS:KIDS_STATISTICAL_SERIES`,
  43 measures — **wrong model**), `d07a0d45-…` (`KIDS:KIDS_INDICATORS`, placeholder `KIDS_FILE_n` codes),
  `f224667a-…` (`KIDS:KIDS_INDICATORS` 2.0.0, canonical codes — the right shape, but scale 10).
- Snapshots 58, 59, 60 are `REVIEW_REQUIRED`, unpublished. 58 and 59 must never be published. 42 published
  snapshots and legacy snapshot 36 are untouched.
- Registry: 43 concepts + 43 measures `KIDS:KIDS_FILE_n` (wrong model); classification scheme `KIDS_INDICATOR`
  versions 10 (placeholder codes) and 11 (canonical) were written **by hand-written SQL** — a violation of AGENTS.md,
  to be redone through a governed API (AIR-2026-053).
- Code change deployed to dev and uncommitted locally: aggregation as part of a measure definition
  (`ReferenceRegistryService`, `StatisticalReferenceController`, test added; 100/100 in the statistical package).

## 6. What comes next, in order (master checklist phases 3–7)

3. Dev hygiene: supersede the wrong references through the API (`decision: SUPERSEDE`); record, with evidence, that
   snapshots 58/59 are not to be published; update AIR-2026-050…053.
4. Contract legitimacy: classification propose/approve API with four eyes (AIR-053); a dimension-group attribute that
   varies by code (AIR-052); numeric envelope per ADR-012 D-3; then the **KIDS site contract revision N+1** with the
   184-column mapping, a compatibility report and a deprecation rule.
5. The composer in the product (design doc): `PackageContractSource` -> `PackagePlan` -> `FamilySectionRegistry` ->
   `GroupPolicy` -> `PackageWriter`, reading the Control Plane site contract (15 datasets / 287 fields / 58 relations
   already there) instead of the R8 script and `AccessAuthoringAdapter`. Risk G-1: a Windows finishing-and-acceptance
   worker, because saved queries and data macros need the real provider.
6. Acceptance B7: all eight, including the server round-trip with negatives and a second, differently shaped site
   with zero code change.
7. Release: acceptance checklist of `ACCESS-PACKAGE-ORGANIZATION-PLAN.md`, new checksum and provenance, then commit
   and PR — **only with the owner's go-ahead** (nothing of this work is committed yet).

## 7. Working rules the owner has set

Reply in Georgian, short. Work autonomously and do not stop; do not touch web or production; no push without a
go-ahead. Follow the documents, no experiments. Tick a checklist item only with evidence. When something is not
ready, say NOT READY.
