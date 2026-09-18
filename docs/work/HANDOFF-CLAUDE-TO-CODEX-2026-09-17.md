# Handoff: Claude → Codex · 2026-09-17

**სტატუსი:** სესიის დახურვის ანგარიში. ფაქტები ადასტურებულია runtime-ით ან reproducible test-ით; სადაც არ არის — ასეთ მონიშნულია.
**Authority-ის იერარქია უცვლელია:** `AGENTS.md` → `docs/reference/CANONICAL-FULL-TREE.md` → `KIDS-R8-current-status-and-acceptance.md`. ეს დოკუმენტი მიმოხილვაა, არა normative source.

---

## 1. Executive summary

| | |
|---|---|
| **მიზანი** | Repository hygiene (P0), remote-dev გარემოს ჯანსაღება და prod-თან ფუნქციური parity |
| **შედეგი** | Remote-dev `geostat-api-dev` — prod profile, OIDC enforced, health ALL UP; working tree clean |
| **Commits** | superproject 16 (`a9d68f2 … this report`), stack-kit 1 (`1b2c77b`) |
| **Push** | ❌ **არცერთი** — master ahead 16; kit branch ahead 1 |
| **Legacy prod** | უცვლელი (მხოლოდ read-only inspection) |
| **Tests** | `:api:test` BUILD SUCCESSFUL · stack-kit `287 passed, 23 skipped` |
| **Production readiness** | უცვლელი: `NOT_READY_FOR_PRODUCTION` (B-01…B-07 ღია) |

---

## 2. მომხმარებლის დადგენილი წესები (სავალდებულო)

1. **Legacy prod-ს არ ეხება.** `geostat-api` (:8083), `geostat-mobile`, `geostat-system-app` (:5175), `~/geostat/backend/api` — ძველი ვერსიაა (jar-ში migration-ები მხოლოდ 075-მდე). მხოლოდ read-only.
2. **Remote-dev = prod-ის პროტოტიპი.** prod-ზე არსებული ყველა ფუნქციონალი dev-ზე სრულად უნდა იყოს. ყველა ცვლილება/restart — dev stack-ზე.
3. **stack-kit და project — ორი ცალკე ხაზი.** kit-ში geostat-specific ცოდნა დაუშვებელია; project მხოლოდ დეკლარირებს (`geostat.ops.json`, env values).
4. **Shared base → overlay.** კანონიკურად საერთო — ერთხელ დეკლარირებული; ინდივიდუალური — overlay/env-ში. host path-ები და secret-ები — env file-ებში, არა manifest-ში.
5. **ინფრასტრუქტურა საერთოა** (MinIO, Redis, Keycloak/edge, OTel, `geostat-net`). Per-environment infra split მომხმარებლის მიერ **უარყოფილია**.

---

## 3. ჩაბარებული სამუშაო

### 3.1 Migration integrity — `a9d68f2`
- **`085_canonical_storage_bindings.sql`:** column-dependent UPDATE → `sp_executesql` (deferred compile). ძველი batch fresh control plane-ზე failed. **უსაფრთხოება ადასტურებული:** prod jar-ში migration-ები 075-მდე; 085 applied მხოლოდ dev-ზე, fixed checksum-ით (`7a76f113…`).
- **Bug fix — 082 არასოდეს სრულდებოდა:** `082_contract_approval_receipt.sql` runner-ში რეგისტრირებული არ იყო → `platform.contract_approval_receipt` არსად არსებობდა, contract approval write failed-ის რისკი. დაემატა; dev-ზე table EXISTS, ledger-ში recorded.
- **Guard test:** `PlatformSchemaMigrationRegistrationTest` — ყველა `db/platform/*.sql` რეგისტრირებული ზუსტად ერთხელ, numeric order-ით.

### 3.2 Repository hygiene
- `f4b5a60` — root `txt.txt` (Codex სესიის object-storage შენიშვნები) → `docs/archive/source-notes/object-storage-codex-session-2026-09-16.txt` (კონტენტი შენახული).
- Orphan `kits/stack-kit` (broken `.git`, ძველი snapshot, მხოლოდ test-run output) → repo-ს გარეთ: `Desktop/stack-kit-orphan-backup-2026-09-17` (**არ წაიკიდა**).
- `7989943` — stack-kit submodule + `geostat.ops.json` + CLI entrypoint + infra mssql + dev Dockerfile + UI `.npmrc`.
- `67e2072` — **Bug fix:** UI app კითხულობს `VITE_BASE_URL`, build-ი გადასცემდა `VITE_API_URL` → bundle fallback `localhost:8081`.
- `ffabbf2` — `*.sql eol=lf` (87 file უკვე LF — checksum-ი არ შეიცვალა).
- `180ce6c` — `docs/work/PLATFORM-STATE-2026-09-17.md` (docs-ის ერთიანი snapshot).

### 3.3 Dev/prod parity — shared base architecture
| Commit | ცვლილება | მტკიცებულება |
|---|---|---|
| `8e100ab` | `ops/compose/projects/geostat/services/api/docker-compose.base.yml` — API service ერთხელ (env keys, volumes, health, logging); `docker-compose.prod.yml` → `extends` | rendered `docker-compose config` before/after **identical** |
| `125759a` | `platform.oidc.*` → base `application.yml` (profile-agnostic) | `:api:test` PASS |
| `347e69f` | `api/docker/legacy-sqlserver-tls.security` — ერთ policy file ორივე image-ში (append to `java.security`); `bootRun` ← `BOOTRUN_JVM_ARGS` | dev: `secondary` UP; Gradle daemon-ზე 0 truststore flag |
| `0bfbdb0`, `25e6e54` | `ops/scripts/shell/tls/build-public-truststore.sh` — passwordless PKCS12 (public cert only), private key refusal | store opens without password (`1` entry) |
| `e26730e` | manifest: `composeBase`, `profile: prod`, truststore volume `${PUBLIC_TRUSTSTORE_FILE}` | runtime (ქვემოთ) |

### 3.4 stack-kit — `1b2c77b` (branch `feat/manifest-declared-remote-dev`)
- Generic declaration points: `dev.services.<svc>.composeBase | profile | volumes` (+ schema).
- tar+ssh transport + server-side `rsync --delete`, როცა local rsync არ არის.
- **Bug fix:** remote script-ები `source .env` → value-ები `;`-ზე truncated, export-ები override-ავდნენ `--env-file`-ს (JDBC URL-იდან `trustServerCertificate=true` იკარგოდა). 4 file-ში გასწორდა.
- Tests: behavioural + regression (`test_remote_scripts_never_source_dotenv_files` — negative proof: ძველი კოდზე fail).

### 3.5 Remote-dev runtime (verified 2026-09-17)
| Check | შედეგი |
|---|---|
| `/health` | `primary, dataPlane, archivePlane, objectStorage, secondary` = **UP** |
| Profile / OIDC | `prod` / `true`, issuer `https://auth.geostat.internal/realms/geostat` |
| Anonymous request | 401 |
| Forged RS256 | 401 `invalid_token … no matching key(s)` → JWKS TLS-ით მოიპოვა |
| TLS/PKIX errors | 0 |
| State | `/app/storage`, `/app/uploads` — named volumes (rsync-safe) |
| `.env.dev` source | `ops/config/projects/geostat/services/api/.env.dev` (gitignored), 51 key, backups `*.bak-20260917*` |

### 3.6 Documentation
- `f6bcbea`, `e393d3c` — `docs/reference/SERVER-TOPOLOGY.html` (prod/dev/shared infra: IP, domain, port, image, risks). Online: <https://claude.ai/artifact/6frDAfNHEvgT2KQyNomuwT>. Linked from `docs/README.md` და PLATFORM-STATE §6.

---

## 4. ჩაბარებამდე ადმითებული შეცდომები (ობიექტურად)

| # | შეცდომა | გავლენა | სტატუსი |
|---|---|---|---|
| 1 | `git commit` pathspec-ის გარეშე — pre-staged `.gitmodules`/submodule migration commit-ში | local only | `reset --soft` + სწორი split; push-ამდე |
| 2 | prod-ის `STORAGE_*` dev-ში → dev ↔ prod bucket-ები | dev writes prod bucket-ებში (ფაქტიური write არ დაფიქსირდა) | მომხმარებლის წესით infra საერთო — დარჩა შენაწილებული MinIO |
| 3 | მომხმარებლის "ორი ხაზი" არასწორად წაკითხვა → infra split (dev MinIO, `geostat-dev-net`, bucket copy) | dev MSSQL ~5 წთ network-ზე ცალკე; seed container დროებით prod network-ზე | **სრულად revert-ილი**; prod read-only; volume/network/container მოსილული |
| 4 | `JAVA_TOOL_OPTIONS` workspace-ში | Gradle daemon crash (GC conflict) | `BOOTRUN_JVM_ARGS` |

---

## 5. ღია საქმეები (prioritized)

### P0 — decision / action required
1. **Push.** superproject `master` ahead 16; stack-kit `1b2c77b` unpushed. ⚠ Submodule pointer unpushed commit-ზე — **clone broken, სანამ kit push არ მოხდება.** თანმიმდევრობა: kit branch push → superproject push. (მომხმარებლის ნებართვა საჭიროა.)
2. **Duplicate DNS alias `api`** — `geostat-api` და `geostat-api-dev` ორივე `api` geostat-net-ზე → round-robin prod/dev. Fix: kit workspace compose-ში service key `api` იძლევა default alias-ს; dev-ზე alias-ი მოისილოს (მაგ. kit-ში declared `networkAliases` ან service key ≠ `api`). Edge სრული სახელს იყენებს — ამჟამად უსაფრთხოა.

### P1 — parity / drift
3. **Legacy prod server compose ≠ repo.** სერვერზე: `SPRING_DATA_REDIS_*` და password truststore (`oidc-truststore.jks`, password `JAVA_TOOL_OPTIONS`-ში plain text — `ROTATION_REQUIRED`). Repo-ში არ არის; app კითხულობს `PLATFORM_RATE_LIMIT_REDIS_URL`/`REDIS_PASSWORD`. prod-ის შემდეგ deploy-ამდე reconciliation.
4. **UI parity** — `geostat-system-app-dev` prod-თან შედარება/shared base არ გაკეთდა; dev UI საკუთარ network-ზე, prod UI default bridge-ზე.
5. **Dev API edge-ის უკან არ არის** — HTTPS acceptance-ისთვის ცალკე pseudo-domain (მაგ. `api-dev.geostat.internal`).
6. Manifest `stack.networkName: geostat-net` — env-specific value shared manifest-ში (fallback only; `deploy.env` override-ავს).
7. `mobile` service — shared base-ზე refactor არ გაკეთდა (prod compose-ში inline).

### P2 — carry-over (PLATFORM-STATE-2026-09-17 §4–§7-იდან, უცვლელი)
8. Protected HTTPS replay (fresh token `auth.geostat.internal`) pages 8–11; include-fix live replay.
9. Page 12 reference adapter; `entity.resource_locator` binding.
10. Docs contradictions (C-14): schema naming, lifecycle, counts, host-layout status.
11. Unregistered migrations `037, 039, 045, 047, 054, 055, 056` — test-ში `KNOWN_UNREGISTERED`; intent unverified.
12. Authority template (B-01…B-07) — external owners.

---

## 6. გაგრძელების ინსტრუქცია

```powershell
# 1. baseline
git status -sb; git -C platform/kits/stack-kit status -sb
ssh administrator@192.168.1.199 "curl -s localhost:8081/health"

# 2. dev redeploy (kit, service explicitly)
pwsh -NoProfile -File ops/cli/lifecycle/geostat.ps1 api dev bootstrap api

# 3. tests
cd platform/apps/geostat/backend; .\gradlew.bat :api:test --console=plain
cd platform/kits/stack-kit; python -m pytest -q tests
```

**Operational notes**
- სერვერზე `docker-compose` (standalone), არა `docker compose`.
- Local machine-ზე rsync/Docker არ არის; kit tar fallback-ს იყენებს.
- `.env.dev` dotenv-ია, **არა shell** — არასოდეს `source`.
- JVM options: image run → `JAVA_TOOL_OPTIONS`; source run → `BOOTRUN_JVM_ARGS`.
- Legacy SQL `192.168.0.230` საჭიროებს `legacy-sqlserver-tls.security` policy-ს (JDK ≥ 17.0.16).
- Rollback artefacts: server `~/geostat/backend/workspace/.rollback-20260917/`; local `*.bak-20260917*` (gitignored).
- Commit-ები — ყოველთვის explicit pathspec-ით; Windows-ზე executable bit — `update-index --chmod=+x` + commit pathspec-ის გარეშე.

---

**ჩაბარა:** Claude (Opus 5) · **მიიღოს:** Codex · 2026-09-17

---

## 7. დამატება 2026-09-18 — Storage / Artifact Attachment ხაზი (ცალკე სამუშაო ნაკადი)

**სტატუსი:** source + tests DONE (`:api:test` 171 PASS, 0 failure; 54 artifact test) · **uncommitted** (commit არ გაკეთებულა — მომხმარებლის ნებართვა საჭიროა) · dev runtime-ზე დგას **refactor-მდე** ვერსია → საჭიროა redeploy.
**Checklist (layer-by-layer სტატუსები):** `docs/work/STORAGE-ARTIFACT-CLOSURE-CHECKLIST.md` · **ADR-008** (`docs/platform-decisions.md`) · **AIR-2026-010…013** · **Evidence:** `docs/evidence/kids-r8-resource-artifact-binding-2026-09-18.json`.

### 7.1 რა გაკეთდა (ზემოდან ქვემოთ)

| Layer | Artifact |
|---|---|
| Meta-schema / Control | `086_artifact_attachment_meta_schema.sql` — `platform.artifact_policy`, `platform.artifact_relation_definition`; APPROVED immutable (THROW 51020/51021) |
| Physical Data plane | `087_artifact_registry_data_plane.sql` — `ingest.artifact_object` (SHA-256 UK), `ingest.artifact_manifest`, `ingest.artifact_version` (append-only, 51023), `entity.artifact_attachment` (published immutable, 51022), view `entity.v_artifact_attachment_reconciliation` |
| Contract binding (KIDS, seed only) | `088_kids_r8_resource_artifact_binding.sql` — policy `KIDS_PUBLIC_STATISTICAL_FILE` r1, relation `PRIMARY_FILE` (ka/en, 1..1), rule `SOURCE_PATH` (NFC, strip `files/`, root `mainstat/`) |
| Runner | 086 control, 087 data, 088 control, 089 control — `PlatformSchemaMigrationRunner`; 086 checksum preserved after its first application |
| Object Storage port | `service/artifact/ArtifactObjectStore` (port) ← `service/storage/ObjectStorageService` (MinIO adapter): stat, full SHA-256, bounded read, content-addressed put, presign (`STORAGE_PUBLIC_ENDPOINT`, `STORAGE_REGION`) |
| Domain (pure) | `ArtifactKeys`, `MediaTypes` (Spring `MediaTypeFactory`), `ArtifactManifest(+Generator)` v1, `ArtifactMatchRule` strategy + `SourcePathMatchRule(+Parser)` + `ArtifactMatchRules` registry, `ArtifactMatcher` (1:N ordinals, cardinality), `ArtifactBindingPlanner`, `ArtifactReconciler`, `ArtifactPolicy`, enums (`VerificationStatus`, `ArtifactRole`, `RetentionClass`, `BindingStatus`, `GateResult`) |
| Persistence | `ArtifactRegistry`, `ArtifactAttachmentRepository`, `ReleaseGateEvidenceRepository`, `ArtifactContractResolver` |
| Application | `ArtifactPackageService` (inventory import, ZIP upload → `artifacts/sha256/`, verify), `ArtifactAttachmentService`, `ArtifactReconciliationService` (gate `ARTIFACT_RECONCILIATION` + checksum), `ArtifactDistributionService`, `ArtifactMetrics`, `ArtifactProperties` (`platform.artifacts.*`) |
| Publication | `PlatformPublicationService` → `requirePassIfDeclared` (declared datasets only); `PlatformArchiveService` → attached objects into `archive.artifact_reference` |
| API | `PlatformArtifactController` `/api/v1/platform/artifacts/**` (7 routes) + `ArtifactApiExceptionHandler` (RFC 9457) |
| Edge | `ops/compose/.../edge/nginx.conf` server `files.geostat.internal` (signed GET/HEAD only) + alias — `nginx -t` PASS, **არ არის deployed** |
| Config | `application.yml`: `storage.s3.public-endpoint`, `storage.s3.region`, `platform.artifacts.*`; local `.env.dev` (gitignored): `STORAGE_PUBLIC_ENDPOINT=http://minio:9000` |

### 7.2 დადასტურებული ფაქტები
- MinIO: 532/532 object ფიზიკურ prefix-ზე `kids/r8/resources/kids-files-r8-sanitized/`, content SHA-256 = key; inventory `objectName` ამ segment-ს არ შეიცავს (AIR-2026-010 — manifest key-ს ახლიდან ითვლის).
- `source_resource_id` 225/225 unique non-null; 450/450 slot exact match; 450 distinct object; 115 orphan (warning).
- Dev (`geostat-api-dev:8081`, DB `geostat-system-mssql`, prod DB `192.168.0.230` ხელშეუხებელი): bootstrap OK, health ALL UP, 7 route, anonymous 401, startup-ზე migration შეცდომა არ ყოფილა.

### 7.3 დარჩენილი (რიგით)
1. **Commit** (explicit pathspec, ზემოთ ჩამოთვლილი ფაილები) → **dev redeploy**: `pwsh -NoProfile -File ops/cli/lifecycle/geostat.ps1 api dev bootstrap api --no-build`.
2. **Ledger read** 086–088 (`platform.schema_migration`) — DB read-ს auto-mode classifier-მა უარი უთხრა; ოპერატორის ნებართვა/`!` command.
3. **Token** (`contract.write`/`contract.read`) → runtime acceptance:
   - `POST /api/v1/platform/artifacts/manifests/inventory` `{"packageCode":"KIDS_R8_RESOURCES","inventoryKey":"kids/r8/resources/kids-files-r8-sanitized/inventory.json","objectPrefix":"kids/r8/resources/kids-files-r8-sanitized/"}` → ელოდება 532 VERIFIED;
   - `POST .../snapshots/{KIDS_RESOURCE snapshot}/attachments?manifestId=..&dryRun=true` → `false` → `POST .../reconciliation` → PASS;
   - download smoke (200 + checksum) და negatives 403/404/409 → evidence JSON განახლება.
4. **EXT:** `files.geostat.internal` (TLS SAN, DNS, edge redeploy, `STORAGE_PUBLIC_ENDPOINT`), anonymous policy (EXT-1), retention/DR/encryption (B-06).
5. Frontend `/files/...` → governed download (AIR-2026-009/013) — მხოლოდ live gate PASS-ის შემდეგ.
