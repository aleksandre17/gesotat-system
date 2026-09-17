# GEOSTAT პლატფორმის ერთიანი მდგომარეობა — 2026-09-17

**სტატუსი:** შემაჯამებელი snapshot (არა normative authority). შექმნილია `docs/`-ის სრული გადახედვის, remote სერვერის (192.168.1.199) runtime შემოწმებისა და git მდგომარეობის საფუძველზე.
**Authority-ის იერარქია უცვლელია:** runtime — `KIDS-R8-current-status-and-acceptance.md`; architecture — `final-unified-physical-virtual-contract.md`, `api-complete-input-output-contract.md`, `access-and-control-plane-responsibility-model.md`; engineering policy — `AGENTS.md`.

---

## 1. ერთ წინადადებაში

კოდი, ტესტები და ლოკალური/staging acceptance თითქმის დასრულებულია (33/33 PASS, 278 verified / 10 open), მაგრამ production release gate **FAIL**-ია — `productionDecision: NOT_READY_FOR_PRODUCTION`. მიზეზები: dirty tree, გარე authority-ის არარსებობა, deploy-ს მოლოდინში მყოფი fix-ები და დოკუმენტაციის წინააღმდეგობები.

---

## 2. სამიზნე არქიტექტურა

| Plane | ბაზა/საცავი | დანიშნულება |
|---|---|---|
| Control | `geostat-system` (`platform.*`) | კონტრაქტი, registry, policy, approval, migration ledger |
| Data | `geostat-data` | `ingest / raw / entity / statistics / classification-snapshot / geo / publication / serving` |
| Archive | `geostat-archive` | retention record + immutable pointer |
| Object storage | MinIO/S3 | ingest, quarantine, archive, export bytes |

ძირითადი წესები:
- პლატფორმა **contract/metadata-driven, schema/provider/site-agnostic**; KIDS R8 — პირველი reference profile, არა მიზანი.
- ახალი site = `data_product` metadata; **არ** იქმნება DB/table per site/field; generic EAV აკრძალულია.
- Access — მხოლოდ transport (`__gs_/__ent_/__rel_/__stat_/__raw_`); authority არ არის. `__raw_document` — მხოლოდ source-row locator; raw bytes-ის authority — immutable object-storage artifact.
- Pipeline: approved contract revision → ingest/stage/quarantine → canonical materialization → immutable snapshot → contract-scoped query → API/export.
- Migration-ები append-only და checksum-recorded; applied ფაილი არ ეცვლება.

### სავალდებულო თანმიმდევრობა
1. **Top-down:** Doctrine → Meta-schema/Control Plane → Physical DB planes → Object Storage → Site contract → Access schema → Ingestion/materialization → Snapshot/publication → API contract → Consumer/frontend.
2. Completion plan: `C-01 → C-02 → C-03/04/05 → C-06 → C-07/08/09 → C-10 → C-11/12 → C-13 → C-14`.
3. Workstreams: `W-01 → W-02/W-04/W-05 → W-03 → W-06 → W-07` (W-02/04/05 runtime activation მხოლოდ `VERIFIED` authority record-ით).
4. ნებისმიერი P0 blocker აჩერებს დანარჩენ მუშაოს.

---

## 3. დასრულებული და დამტკიცებული

### Platform (implementation/test)
Revision-bound query/validation/introspection; KIDS hardcode-ის მოსილვა (188 Java file, 0 violation); lifecycle/checksum/approval persistence (082–084); provider registry (081); SDMX/Parquet/ZIP codec; SDK generation; query-cost admission; Redis quota adapter; OTLP wiring; evidence bundle generator; secret-boundary preflight; duplicate migration sequence repaired (storage binding → `085`).

### KIDS R8 (production 192.168.1.199:8083)
- Contract `KIDS_PORTAL_V1` rev 8, site/ingestion `APPROVED`; migrations 071–075.
- Artifact `kids-portal-v1-canonical-r8-final.accdb` — 2,834,432 bytes, SHA-256 `D62C58C9…` (⚠ 2026-09-10-ის accepted fingerprint განსხვავდება: 1,916,928 / `A2760BE2…` — replay-ზე ერთ-ერთი explicit-ად ასარჩიოს).
- Batch 3 → snapshots 22–36 → publication snapshot 13 `PUBLISHED`: 36 goal, 225 resource, 230 assignment, 178 glossary, 880 observation; cache 297; archive 2,224 record/pointer.
- Rollback 13→3→13 PASS; idempotent re-ingest (იგივე batch/load ID); quarantine fixture 879/1; newer-format artifact rejected; `BACKUP … WITH CHECKSUM` + `RESTORE VERIFYONLY` + DR restore PASS.
- 532 resource file MinIO-ში; Parquet Linux/Docker conformance PASS.

### Page-ების სტატუსი
| Page | Dataset | სტატუსი | Evidence |
|---|---|---|---|
| 7 root | — | data query სწორად rejected | `kids-r8-page-inventory-2026-09-15.md` |
| 8 goals | KIDS_GOAL | `RUNTIME_REPLAY_PASS`; live shadow diff/lineage OPEN | `kids-r8-page8-runtime-replay-2026-09-15.json` |
| 9 resources | KIDS_RESOURCE | `RUNTIME_REPLAY_PASS`, scope reconciliation PASS (159 legacy + 66 cat 5–10); `stableKeyProof: PENDING` | page9 + resource-scope json |
| 10 glossary | KIDS_GLOSSARY_ENTRY | parity 178↔178; publication BLOCKED (steward/quality/privacy) | page10 json |
| 11 statistics | KIDS_STATISTICAL_INPUT | parity 880↔880; **include regression** — source fix PASS, live deploy PENDING | page11 + `kids-r8-live-include-regression-2026-09-16.json` |
| 12 classifiers | KIDS_CLASSIFIER_ITEM | **BLOCKED_BY_REFERENCE_ADAPTER** | page12 json |

Overall gate: `FAIL_CLOSED_PENDING_EXTERNAL_AUTHORITY` (`kids-r8-live-acceptance-gate-2026-09-15.json`).

### Security / edge / observability
- HTTPS edge (nginx, 443 only, fail-closed): `PASS_FOR_INTERNAL_HTTPS_BOUNDARY`, issuer `https://auth.geostat.internal/realms/geostat`, JWKS PASS, anonymous 401.
- Protected replay: `BLOCKED_STALE_TOKEN_ISSUER` (token ძველი `keycloak:8443` issuer-იდან).
- API OIDC/JWKS implementation READY (AIR-2026-008: HMAC vs RS256 მიზეზი ცნობილია); production issuer/CA/DNS OPEN.
- Tempo — `PASS_STAGING_ONLY`; Prometheus/Grafana PARTIAL; OTel collector FAIL (9464 არ უსმენს, debug exporter).

---

## 4. ღია პრობლემები (P0 → P2)

### P0 — ახლავე ჩვენ შეგვიძლია
1. **Migration 085 შეცვლილი** (uncommitted). თუ რომელიმე გარემოში applied-ია → checksum drift. სავარაუდო კავშირი dev-ის `Invalid column name 'canonical_dataset_version_id'`-თან. გადაწყვეტა: შემოწმება ledger-ში; applied-ის შემთხვევაში ცვლილება → ახალი `086`, 085 აღდგეს.
2. **Truststore credential exposed** — `ROTATION_REQUIRED`; ამჟამად plain-text-ია `geostat-api` container-ის `JAVA_TOOL_OPTIONS`-ში. Rotate + secret-ში გადატანა.
3. **Release gate FAIL** — dirty entries 14–20; root-ის `txt.txt` (1898 ხაზი, სავარაუდოდ შემთხვევით commit-ილი 1c2e6bc-ში); uncommitted remote-dev wiring.
4. **stack-kit დუბლი:** `platform/kits/stack-kit` (submodule → `github.com/aleksandre17/stack-kit`) და untracked `kits/stack-kit`.

### P1 — ტექნიკური
5. Include fix-ის image deploy + live replay (release gate-ის შემდეგ).
6. Fresh token `auth.geostat.internal`-იდან → protected HTTPS replay (`ops/scripts/kids-page11-runtime-replay.ps1`).
7. Page 12 reference adapter (version-aware `item_ref`, publication-state filter, relation replay).
8. `inventory.json` → `entity.resource_locator` → checksum → signed download; შემდეგ frontend `public/files` retirement.
9. OTel collector/Tempo live config; container-ში internal CA trust.
10. Contract approval ↔ steward policy (C-01/C-08); keyset runtime index enforcement + concurrent-write test (C-07).
11. Tenant data boundary — `NOT_IMPLEMENTED_OR_NOT_EVIDENCED`.

### P2 — დოკუმენტაციის წინააღმდეგობები (C-14)
- `final-unified-physical-virtual-contract.md` §2 authority #2-ად ასახელებს superseded `unified-canonical-platform-final.md`-ს.
- `access-driven-data-platform-master-plan.md` ერთდროულად "superseded" და "baseline"; per-product schema (`cids`, DDL provisioning) ADR-001-ს ეწინააღმდეგება.
- Schema naming: `catalog.* / reference.classification_* / semantic.*` vs `platform.contract_* / classification.*_snapshot` (migration 034–036 = `platform.*`).
- Lifecycle: docs `DRAFT→REVIEW→APPROVED→PUBLISHED→DEPRECATED→RETIRED` vs code `DRAFT→REVIEW_REQUIRED→APPROVED→SUPERSEDED/ROLLED_BACK`.
- Retention: 1 year (ADR-003) / 90 days / `7Y`.
- Archive table-ები: 2 vs 4 (`archive.artifact`, `integrity_event`).
- `api-modernization-capability-gap.md` stale (rate limit "TODO", SDMX/Parquet "not declared").
- Completion plan §3 vs §6 (provider discovery, alternate stat storage, SDK).
- Count drift: 147/183/188/219/271/278; acceptance 20/29/32/33; W01–W10 vs W-01–W-07.
- Blueprint: root `db/` §2-ში აკრძალული, §8-ში ჩამოთვლილი; `control-plane-ui`/`geostat-system-app` blueprint-ში არ არის.
- Host layout: `host-layout-migration.md` = "completed", `CONTINUE-HERE.md` = "scaffolded"; ძველი `scripts/host-layout-check.ps1` path (სწორი: `ops/cli/validation/host-layout-check.ps1`); `project.json` = `PHYSICAL_RELOCATED`.
- Release identity: `6fe8701d…` / `c782623…` / tag `geostat-v1.0.0` on `f9906872…` (tag note misplaced `access-and-control-plane-responsibility-model.md`-ის ბოლოს) vs probe `releaseAndDeployAuthority: NOT_ASSERTED`.

---

## 5. გარე authority (ჩვენ არ დავხურავთ)

Template: `production-authority-input-contract.md` (ამჟამად `DRAFT`, `authorityRevision: null`).

| ID | Blocker | Owner | საჭირო input |
|---|---|---|---|
| B-01 | Release provenance | Release owner | signed tag, deploy authority, clean replay, OCI digest |
| B-02 | OIDC/RBAC/ABAC/tenancy | Security + data protection | HTTPS issuer/JWKS, audience, claim→role/tenant, negative suite |
| B-03 | Independent provider | Platform/data owner | მეორე რეალური DB endpoint + secretRef |
| B-04 | Redis quota/HA | Platform + security | TLS Redis, budgets, HA topology |
| B-05 | Durable observability/SLO | SRE + business | backend, retention, SLO, alert routes |
| B-06 | Backup/DR | Operations + business | target, retention, RPO/RTO, timed restore |
| B-07 | Load/chaos/security test | Operations | UTC window, abort threshold, approver |

Steward decisions (`kids-steward-decision-register.md`): `KIDS-STAT-001` (43 carrier metric/unit/aggregation), `KIDS-STAT-002` (8 age band), `KIDS-REF-001` (`sub_category`, token `4`), `KIDS-LANG-001` (`ena`).
Frontend decisions: anonymous vs OIDC pages 8–12, public API origin, section→statistic filter, static link → governed resource.

---

## 6. Remote dev გარემო (192.168.1.199)

| Container | Port | სტატუსი |
|---|---|---|
| `geostat-system-app-dev` | 5176 | UP, HTTP 200 |
| `geostat-api-dev` | 8081 | **UP, health ALL UP** (2026-09-17 fix-ის შემდეგ) |
| `geostat-system-mssql` | 1433 (internal) | healthy; `geostat-system/data/archive` restored |
| `geostat-api` (prod) | 8083 | healthy |

Dev workspace: `~/geostat/backend/workspace/geostat-api-dev/` (`docker-compose.workspace.yml`, `.env.dev`). სერვერზე `docker-compose` (v1 syntax), არა `docker compose`.

**2026-09-17 fix:**
1. `secondary` DOWN — JDK 17.0.20 disables `TLS_RSA_*`/`rsa_pkcs1_sha1`; legacy SQL Server `192.168.0.230` handshake fail. Fix: `api/Dockerfile.dev.remote` — `JAVA_TOOL_OPTIONS=-Djava.security.properties=…` prod (17.0.15) policy-ით.
2. `objectStorage` DOWN — `.env.dev`-ში `STORAGE_*` არ იყო; დაემატა prod `.env.prod`-იდან (backup `.env.dev.bak-*`).
- ⚠ `STORAGE_*` მხოლოდ სერვერის `.env.dev`-შია; kit-ის re-bootstrap შეიძლება წაიკიდოს — local config source-ში ასახვა საჭიროია.
- ⚠ Dockerfile ცვლილება uncommitted.

Stack-kit remote modes: static/runtime deploy; remote source dev (`dev bootstrap` → `dev watch`); remote compose build (`deploy remote`, `stack-deploy --prod`). Prod API ჯერ legacy flat layout-ზეა (`~/geostat/backend/api`), structured `compose/` layout-ზე migration არ მომხდარა.

---

## 7. გაგრძელების გეგმა

**ეტაპი 1 — ჰიგიენა (P0):** migration 085 ledger check → საჭიროების შემთხვევაში `086`; `txt.txt` მოსილვა; stack-kit დუბლის გადაწყვეტა; remote-dev wiring + Dockerfile commit; truststore rotation → release gate PASS.
**ეტაპი 2 — live acceptance:** fresh token → HTTPS protected replay 8–11; include-fix deploy + replay; page 12 adapter; resource locator binding.
**ეტაპი 3 — დოკუმენტაცია (C-14):** ერთ schema naming, ერთ lifecycle, ერთ count; superseded დოკუმენტები authority list-იდან; host layout status-ის გაერთიანება.
**ეტაპი 4 — authority:** owner-ები ავსებენ authority template-ს → OIDC/Redis/OTel production activation (W-02/04/05) → W-03 → W-06 → W-07.
**ეტაპი 5 — frontend:** KIDS frontend Phase A→G (top-down-ის ბოლო რგოლი).

---

## 8. წყაროები

- Handoff/status: `README.md`, `KIDS-R8-current-status-and-acceptance.md`, `NEXT_SESSION_HANDOFF.md`, `CONTINUATION_HANDOFF.md` (ორივე historical), `work/*`.
- Architecture: `platform-capability-and-architecture-audit-2026-09-13.md`, `contract-driven-metadata-schema-agnostic-completion-plan.md`, `final-*.md`, `three-database-physical-dictionary.md`, `platform-decisions.md`, `reference/*`.
- Production: `production-blockers-and-required-authority.md`, `production-authority-input-contract.md`, `remote-production-evidence-2026-09-13.md`, `evidence/geostat-production-readiness-probe-2026-09-16.json`.
- KIDS: `kids-*.md`, `evidence/kids-r8-*.json`.
- Improvement register: `work/ARCHITECTURE-IMPROVEMENT-REGISTER.md` (AIR-2026-008 P0 TRIAGED; 001/002/003 DISCOVERED; 009 DEFERRED; 007 VERIFIED).
