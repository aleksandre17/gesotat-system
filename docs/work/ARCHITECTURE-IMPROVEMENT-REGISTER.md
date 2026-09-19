# Architecture Improvement Register (AIR)

**სტატუსი:** CANONICAL / ACTIVE  
**მიზანი:** ყოველი კრიტიკული აღმოჩენის, გაუმჯობესების იდეისა და არქიტექტურული
რისკის სტრუქტურულად ჩაწერა ისე, რომ შემდგომში ის პირდაპირ შესრულებად,
დამოწმებად სამუშაოდ გადაიქცეს.

## 1. რატომ არსებობს AIR

ინტეგრაციის, audit-ის ან production incident-ის დროს აღმოჩენილი ღირებული აზრი
არ უნდა დარჩეს ჩატში, ზეპირ შეთანხმებაში ან დროებით TODO-ში. AIR არის მისი
ერთადერთი სამუშაო რეესტრი. თითო ჩანაწერს აქვს owner, საზღვარი, პრიორიტეტი,
დამოკიდებულება, acceptance evidence და საბოლოო გადაწყვეტილება.

AIR არ არის უბრალოდ backlog. ჩანაწერი აღწერს **რატომ** არის ცვლილება საჭირო,
რომელ invariant-ს აძლიერებს და როგორ გახდება გაუმჯობესება ყველა consumer-ისთვის
reusable.

## 2. ჩანაწერის კანონიკური ფორმა

## 2a. Proactive smell-detection checklist

ახალი task-ის დაწყებამდე და დასრულებისას reviewer ამოწმებს:

- [ ] ერთი source of truth და ერთი owner არსებობს;
- [ ] არ არის დუბლირებული field, mapping, table ან execution branch;
- [ ] არ არის consumer/provider-specific hardcode generic core-ში;
- [ ] ყველა identity, grain, time, unit, dimension და null semantics explicit-ია;
- [ ] relation, lineage, privacy, tenant და authorization საზღვრები ჩანს;
- [ ] query cost, pagination, concurrency და failure ქცევა bounded-ია;
- [ ] contract/runtime/docs drift-ის აღმოჩენის მექანიზმი არსებობს;
- [ ] ახალი reusable capability ან invariant ხომ არ უნდა შეიქმნას;
- [ ] test, telemetry, evidence და rollback გზები განსაზღვრულია.

რომელიმე პუნქტის გაურკვევლობა ავტომატურად ქმნის ახალ AIR ჩანაწერს `DISCOVERED`
სტატუსით; აღმოჩენის ზეპირად დატოვება არ შეიძლება.

## 2b. Blocker escalation

თუ checklist ან review გამოავლენს საკითხს, რომლის გარეშე შემდეგი ნაბიჯი
არასწორი, სახიფათო ან არქიტექტურულად არავალიდური გახდება:

1. მიმდინარე execution დაუყოვნებლივ ჩერდება;
2. ჩანაწერს ენიჭება `priority: P0`, `status: TRIAGED`, `decision: PROPOSED`;
3. იწერება გავლენა, blast radius, containment და stop condition;
4. blocker გადადის პირდაპირ `READY`-ში მხოლოდ მაშინ, როცა fix, regression,
   evidence და rollback მზად არის;
5. განვითარება გრძელდება მხოლოდ `VERIFIED`-ის შემდეგ.

ეს წესი ვრცელდება ყველა პროექტზე და consumer-ზე. დროებითი გაგრძელება მხოლოდ
versioned ADR-ით და compensating controls-ით შეიძლება; backlog-ში დამალული P0
blocker დაუშვებელია.

## 2c. Schema-evolution review

ყოველი site-ის contract/schema review-ში AIR-ში ცალკე უნდა დაფიქსირდეს
`SCHEMA_EVOLUTION` შემოწმება: table split/merge/move, column add/remove/rename,
grain/type/nullability ცვლილება, registry extraction, relation/cardinality,
duplicate normalization და index/lineage/privacy გაუმჯობესება. ჩანაწერს უნდა
ჰქონდეს current-vs-target diff, მიზეზი, consumer impact, compatibility mode,
migration და rollback. თუ ცვლილება არ არის საჭირო, ესეც evidence-ით იწერება.

ამ გზით schema-ს “უცვლელად დატოვება” აღარ ჩაითვლება ნაგულისხმევ სწორ პასუხად;
სწორი პასუხია დამტკიცებული evidence-ით საუკეთესო სტრუქტურის არჩევა.

## 2d. Meta-schema escalation

თუ აღმოჩენილი გაუმჯობესება ეხება იმ სქემას, რომელიც სხვა სქემებს ქმნის ან
ამოწმებს (Control Plane/meta-schema), ჩანაწერს ემატება `metaSchemaImpact: true`
და review მაღალ დონეზე escalates. უნდა ჩაიწეროს meta-model diff, downstream
contract/generator/API impact graph, migration/compatibility strategy და
validator/generator/docs-ის ერთიანი acceptance. Meta-schema-ის ცვლილება
ვერ დაიხურება მხოლოდ ქვედა Access/SQL artifact-ის წარმატებით.

```yaml
id: AIR-YYYY-NNN
title: short imperative title
status: DISCOVERED
source:
  project: geostat
  case: KIDS_PAGE_8_GOALS
  evidence: docs/kids-legacy-canonical-control-plane-comparison-2026-09-15.md#...
problem: precise observed gap or opportunity
impact:
  users: []
  systems: []
  risk: LOW|MEDIUM|HIGH|CRITICAL
invariant:
  violated_or_strengthened: []
  doctrine: docs/reference/ENGINEERING-QUALITY-DOCTRINE.md
scope:
  in: []
  out: []
proposed_capability: reusable platform-level solution
alternatives: []
dependencies: []
owner: team-or-role
priority: P0|P1|P2|P3
target_release: null
acceptance:
  tests: []
  telemetry: []
  evidence: []
rollback: explicit reversible procedure
decision: PROPOSED|ACCEPTED|REJECTED|DEFERRED
created_at: YYYY-MM-DD
updated_at: YYYY-MM-DD
```

სავალდებულო ველების გარეშე ჩანაწერი არ გადადის `TRIAGED` სტატუსში. არ შეიძლება
ერთი consumer-ის workaround generic capability-ად გამოცხადდეს, თუ მისი
reusability და blast radius აღწერილი არ არის.

## 3. Lifecycle და პასუხისმგებლობები

```text
DISCOVERED
  → TRIAGED (problem/evidence/impact verified)
  → AGREED (scope, owner, priority and doctrine fit accepted)
  → READY (design, dependencies, tests and rollback defined)
  → IN_PROGRESS
  → VERIFIED (implementation + evidence + independent review)
  → CLOSED
```

ალტერნატიული დასასრულია `DEFERRED` (მიზეზი/trigger სავალდებულოა) ან `REJECTED`
(decision record სავალდებულოა). `CLOSED` მხოლოდ კოდის არსებობას არ ნიშნავს:
საჭიროა automated tests, runtime/evidence artifact და documentation link.

| როლი | მოვალეობა |
|---|---|
| Discoverer | ფაქტის, source და evidence-ის ჩაწერა; ვარაუდი ფაქტად არ მონიშნოს |
| Architect/Reviewer | abstraction, boundary, alternatives, blast radius და doctrine fit |
| Owner | implementation plan, dependency, timeline, rollback |
| Steward/Authority | semantic/security/business გადაწყვეტილება, სადაც საჭიროა |
| Verifier | test, telemetry, reconciliation და independent evidence |

## 4. Review gates

ჩანაწერი შემდეგ ეტაპზე მხოლოდ მაშინ გადადის, თუ:

- პრობლემა reproducible evidence-ითაა დადასტურებული;
- გადაწყვეტა generic და provider/site-agnostic არის;
- contract/schema/policy ცვლილება versioned-ია;
- security, privacy, lineage, reliability და performance გავლენა შეფასებულია;
- negative, replay, concurrency და rollback ტესტები განსაზღვრულია;
- documentation/runtime ledger linkage მითითებულია.

## 5. საწყისი ჩანაწერები — Goals ქეისი

### AIR-2026-001 — Localized-field normalizer

- **სტატუსი:** `DISCOVERED`
- **პრობლემა:** legacy `title_geo/title_eng` და `path_geo/path_eng` წყვილები
  implicit locale convention-ს იყენებს.
- **გაუმჯობესება:** generic locale-map normalizer, missing-locale validation და
  contract-defined output.
- **მტკიცებულება:** Goals comparison, section 8.2.
- **Acceptance:** locale property tests, missing/duplicate locale negatives,
  shadow parity და redacted telemetry.
- **Current evidence:** Goals page-8 parity tool reports `fieldMismatch=0`;
  generic locale normalizer ჯერ არ არის `VERIFIED`.

### AIR-2026-002 — Source-key parity checker

- **სტატუსი:** `DISCOVERED`
- **პრობლემა:** legacy 36 და canonical 36 row count ემთხვევა, მაგრამ key-level
  parity ჯერ არ დადასტურებულა.
- **გაუმჯობესება:** reusable stable-key diff, duplicate/missing/extra კლასიფიკაცია
  ყველა family-ისთვის.
- **Acceptance:** deterministic diff report, checksum და replay test.
- **Current evidence:** page-8 audit reports `missing=0, extra=0` for the current
  R8 artifact; cross-family reusable checker ჯერ არ არის `VERIFIED`.

### AIR-2026-003 — Relation-backed category resolver

- **სტატუსი:** `DISCOVERED`
- **პრობლემა:** legacy numeric `category` business semantics-ს მალავს.
- **გაუმჯობესება:** classifier/relation registry-driven resolver; უცნობი code
  fail-closed.
- **Acceptance:** valid/unknown/deprecated code tests და cross-provider replay.

სხვა აღმოჩენები (`AIR-2026-004` capability validation, `005` typed response
mapper, `006` drift evidence) იმავე ფორმით ემატება; მათი ID არ ხელახლა არ უნდა
გამოიყენოს სხვა ქეისმა.

### AIR-2026-007 — KIDS_GOAL projection approval blocker

- **სტატუსი:** `VERIFIED` / **priority:** `P0`
- **აღმოჩენა:** R8 Access-ში page 8-ის `KIDS_GOAL` projection არსებობს, მაგრამ
  მისი `approval_state` არის `DRAFT`; capability audit ვერ ადასტურებს `READY`
  projection-ს.
- **გავლენა:** canonical capability და query response-ის production acceptance
  ვერ დაიხურება; დაუმტკიცებელი response shape-ის გამოქვეყნება არღვევს fail-closed
  release governance-ს.
- **containment:** არ ჩაირთოს page 8 canonical serving; არ შეიცვალოს state
  ხელით და არ დაემატოს legacy fallback როგორც ჩუმი ალტერნატივა.
- **გადაწყვეტილება:** `KIDS_GOAL_ENTITY` generator/contract source-ში დამტკიცდა
  `READY`-ად და ახალი R8 artifact ხელახლა გენერირდა.
- **Acceptance evidence:** `auditKidsGoalsCapability` PASS (8 fields,
  `readyProjection=true`), `auditKidsGoalsParity` PASS, artifact SHA-256
  `D62C58C9633766BC597C1EF67F5984FD7104F922C837224374E5D194F6F0BFF7`.

### AIR-2026-008 — Live page-8 runtime OIDC binding mismatch

- **სტატუსი:** `TRIAGED` / **priority:** `P0`
- **აღმოჩენა:** approved test token მიღებულია, მაგრამ live API აბრუნებს `401
  Unsupported JWT token`: API-ის validator იყენებს HMAC `SecretKeySpec`-ს,
  ხოლო Keycloak token არის asymmetric `RS256`.
- **ინტერპრეტაცია:** fail-closed authorization მუშაობს, მაგრამ production
  OIDC/JWKS binding არ არის აქტიური.
- **containment:** anonymous bypass, static token ან security filter-ის შესუსტება
  აკრძალულია.
- **დახურვა:** API-ში approved issuer/JWKS asymmetric decoder-ის ჩართვა,
  issuer/audience/tenant mapping, authenticated replay, response parity, audit
  evidence და token redaction proof.
- **Current evidence:** `kids-page8-runtime-replay.ps1` მიიღო 769-byte signed
  token; API პასუხი იყო `HTTP 401 Unsupported JWT token`.
- **Implementation:** `ops/scripts/kids-page8-runtime-replay.ps1` ახლა fail-closed
  harness-ია; token-ის გარეშე აბრუნებს `CREDENTIAL_REQUIRED`-ს და არ აკეთებს
  anonymous bypass-ს.

## 6. ოპერაციული წესი

### AIR-2026-009 — Legacy request path replacement (deferred)

- **სტატუსი:** `DEFERRED` / **priority:** `P1`
- **აღმოჩენა:** KIDS frontend-ის `Goals`, `SectionDataPage` და `GlossaryModal`
  ჯერ კიდევ პირდაპირ იყენებენ legacy `/api/goals`, `/api/files` და
  `/api/glossary` fetch-ებს; canonical `platformRequestClient` და contract
  request cases უკვე არსებობს, მაგრამ ამ სამ execution path-ში არ არის მიბმული.
- **გაუმჯობესება:** ერთი governed client, page capability preflight, OIDC,
  contract revision, typed response normalization და server-side pagination;
  legacy URL reconstruction და chart JSON parsing უნდა გაქრეს.
- **Acceptance:** სამივე request-ის canonical replay, normalized parity,
  unauthorized/timeout/abort negatives, ETag/correlation evidence და no-legacy
  production build check.
- **დამოკიდებულება:** page 8/9/10/11 approved projection და frontend runtime
  auth configuration.
- **მიმდინარე scope:** frontend implementation შეგნებულად არ შედის მიმდინარე
  page/API conformance სამუშაოში; იგი დაიწყება მხოლოდ ყველა page-ის semantic
  mapping, response parity და contract acceptance-ის დასრულების შემდეგ.

ახალი აღმოჩენა პირველად იწერება AIR-ში, შემდეგ იქმნება card/ADR/implementation
task. Chat message, issue ან commit message შეიძლება იყოს ბმული, მაგრამ AIR არის
canonical status. კვირეული review ამოწმებს stale `DISCOVERED/DEFERRED` ჩანაწერებს,
დამოკიდებულებებს და evidence-ის ვადაგასულობას.

## 7. Storage / artifact attachment ჩანაწერები (2026-09-18)

Checklist: `docs/work/STORAGE-ARTIFACT-CLOSURE-CHECKLIST.md` · ADR-008 · evidence
`docs/evidence/kids-r8-resource-artifact-binding-2026-09-18.json`.

### AIR-2026-010 — Inventory object key drift

- **სტატუსი:** `VERIFIED` / **priority:** `P1`
- **აღმოჩენა:** `inventory.json`-ის `objectName` = `kids/r8/resources/<sha>.<ext>`, ფიზიკური
  key კი `kids/r8/resources/kids-files-r8-sanitized/<sha>.<ext>`; დოკუმენტაციაც ძველ prefix-ს
  ასახელებდა. `objectName`-ზე დაყრდნობილი binding 532-ვე object-ს ვერ იპოვიდა.
- **გადაწყვეტა:** manifest v1 (`ArtifactManifestGenerator`) key-ს ყოველთვის ახლიდან ითვლის
  checksum-იდან და დეკლარირებული ფიზიკური prefix-იდან; inventory-ს `objectName` საერთოდ არ
  გამოიყენება. Reference docs გასწორდა.
- **Evidence:** 532/532 object — content SHA-256 = key; `KidsR8ArtifactBindingConformanceTest`.

### AIR-2026-011 — Meta-schema lacked artifact primitives

- **სტატუსი:** `READY` (runtime ledger read pending) / **priority:** `P0` (blocking gate §22)
- **აღმოჩენა:** meta-schema-ში არ იყო artifact policy / relation definition; `entity.resource_locator`
  ინახავდა მხოლოდ legacy path string-ს checksum-ის, object-ისა და policy-ის გარეშე.
- **გადაწყვეტა:** migrations 086 (Control), 087 (Data), 088 (KIDS seed); approved definition/policy
  immutable trigger-ებით; published attachment immutable.
- **დახურვა:** dev ledger rows 086–088 + API binding/reconciliation run (checklist 12.2–12.5).

### AIR-2026-012 — Browser-reachable signed distribution endpoint

- **სტატუსი:** `TRIAGED` / **priority:** `P1` / **owner:** ops
- **აღმოჩენა:** MinIO მხოლოდ `geostat-net`-შია; presigned URL-ის host უნდა იყოს consumer-ისთვის
  ხელმისაწვდომი და SigV4 host-ს აწერს ხელს.
- **გადაწყვეტა (repo-ში მზადაა):** edge server block `files.geostat.internal` (GET/HEAD, signature
  სავალდებულო, Host უცვლელი) — `nginx -t` PASS. **გააქტიურებას სჭირდება:** TLS SAN
  `files.geostat.internal`, DNS/hosts, edge redeploy, `STORAGE_PUBLIC_ENDPOINT=https://files.geostat.internal`.
- **Containment:** endpoint-ის გარეშე distribution აბრუნებს 503-ს (fail-closed), არა storage path-ს.

### AIR-2026-013 — Legacy locator and static files coexist

- **სტატუსი:** `DEFERRED` / **priority:** `P2`
- **აღმოჩენა:** `entity.resource_locator` (`PATH` kind) და frontend `public/files` კვლავ legacy
  consumer-ისთვის რჩება.
- **წესი:** ორივე რჩება backward compatibility-სთვის, სანამ AIR-2026-009 (frontend governed client)
  არ დასრულდება და live snapshot-ზე `ARTIFACT_RECONCILIATION` PASS არ იქნება; retirement — მხოლოდ
  შემდეგ, ცალკე migration-ით.

### AIR-2026-014 — Approved artifact contract identity could be mutated

- **სტატუსი:** `VERIFIED` / **priority:** `P0` / **owner:** Data Platform
- **აღმოჩენა:** migration 086-ის approved-row triggers არ იცავდა policy/relation version identity-ს,
  checksum algorithm-სა და retired-state immutability-ს. პირდაპირ SQL update-ს შეეძლო approved
  contract-ის semantic identity შეეცვალა ისე, რომ trigger არ ამოქმედებულიყო.
- **Fix in source:** applied migration `086` პირვანდელ, recorded checksum-ზე აღდგა (`f22af46d…`);
  ახალი migration `089_artifact_contract_lifecycle_immutability.sql` trigger-ებს versioned-ად
  ცვლის. Trigger-ები იცავს identity-სა და ყველა semantic field-ს; DRAFT-იდან დაშვებულია
  APPROVED/RETIRED, APPROVED-იდან მხოლოდ RETIRED, RETIRED terminal-ია. Delete და reactivation იბლოკება.
- **Evidence:** migration 089 ledger checksum `dacccda5c2d03d5f33d27bf11b0e9276ff23df19af5fb9b4e331b8aee8c68113`;
  `ops/tests/sql/artifact-contract-lifecycle.sql` SQL Server-ზე PASS — approved policy/relation mutation
  იბლოკება, transaction rollback დასტურდება; remote dev API health PASS.

### AIR-2026-015 — Artifact reconciliation checksum omitted served metadata

- **სტატუსი:** `READY` / **priority:** `P1` / **owner:** Data Platform
- **აღმოჩენა:** publication gate-ის checksum შეიცავდა slot-სა და content SHA-256-ს, მაგრამ არა
  filename/MIME/size/role/verification-ს. Reconcile-ის შემდეგ API-visible metadata შეიძლებოდა
  შეცვლილიყო checksum-ის ცვლილების გარეშე.
- **გადაწყვეტა:** checksum canonical-ად, length-prefixed UTF-8-ით, input order-ისგან დამოუკიდებლად
  ითვლის ყველა API-visible identity/metadata field-ს; digest streaming-ად გამოითვლება bounded memory-ით.
- **Evidence:** `ArtifactReconcilerTest.checksumCoversServedMetadataAndIsIndependentOfInputOrder`;
  full `:api:test` PASS (176 tests).

### AIR-2026-016 — Artifact metadata response omitted declared download policy

- **სტატუსი:** `READY` / **priority:** `P1` / **owner:** API Platform
- **აღმოჩენა:** contract §15 metadata shape-ს `download.mode` და `expiresInSeconds` სჭირდება; API
  response ამას ტოვებდა და undeclared relation-ის metadata-ს fail-closed არ ბლოკავდა.
- **გადაწყვეტა:** metadata approved relation/policy-იდან აბრუნებს mode-სა და TTL-ს; approved definition-ის
  გარეშე serving fail-closed-ია.
- **Evidence:** `ArtifactDistributionServiceTest` policy response და undeclared-relation negative;
  full `:api:test` PASS (176 tests).

### AIR-2026-017 — Multipart ingress limit contradicted artifact package budget

- **სტატუსი:** `READY` / **priority:** `P1` / **owner:** API Platform
- **აღმოჩენა:** package service allowed configurable large bounded ZIPs, but Spring multipart defaulted
  to the Access artifact cap. Valid package uploads could be rejected before application validation.
- **გადაწყვეტა:** separate `PLATFORM_ARTIFACT_MAX_UPLOAD_BYTES` now controls both servlet multipart
  limits and artifact configuration; compressed request size remains distinct from bounded expanded
  entry/package sizes. Compose and env template declare the same override.
- **Evidence:** artifact configuration test, full `:api:test` PASS (176 tests); remote config/deployment replay pending.

### AIR-2026-018 — Concurrent identical manifest registration could race

- **სტატუსი:** `VERIFIED` / **priority:** `P1` / **owner:** Data Platform
- **აღმოჩენა:** two retries with the same package checksum could both observe no manifest before the
  unique-key insert; one request could fail with a duplicate-key error instead of idempotently returning
  the existing manifest.
- **Fix in source:** manifest checksum lookup now takes an update/serializable key-range lock on its
  declared unique index inside the registration transaction.
- **Acceptance/evidence:** `ops/tests/sql/artifact-manifest-concurrent-replay.sh` ran on the remote
  dev SQL Server against the exact `UPDLOCK,HOLDLOCK,INDEX(uq_artifact_manifest_checksum)` lookup and
  transaction used by `ArtifactRegistry`. Two overlapping sessions returned manifest id `3`; the
  committed database contained exactly one manifest, version, and content-object registry row. The
  generated fixture is retained because artifact versions are immutable; it has no attachment and
  is explicitly identified in `docs/evidence/artifact-manifest-concurrent-replay-2026-09-18.json`.
  SQL locking/replay is `PASS`; an authenticated API-level concurrent upload remains gated by EXT-5.

### AIR-2026-019 — Absolute source paths were silently converted to relative paths

- **სტატუსი:** `READY` / **priority:** `P1` / **owner:** Ingestion
- **აღმოჩენა:** manifest path normalization stripped leading `/`, so an absolute POSIX/UNC path could
  be accepted as a different relative package path; Windows drive paths were also accepted as metadata.
- **გადაწყვეტა:** NFC/forward-slash normalization now rejects POSIX/UNC roots and drive-prefixed paths
  before manifest construction, while preserving safe relative paths.
- **Evidence:** `ArtifactManifestGeneratorTest.invalidInventoryIsRejected`; full `:api:test` PASS (176 tests).

### AIR-2026-020 — Package checksum encoding had delimiter ambiguity

- **სტატუსი:** `READY` / **priority:** `P1` / **owner:** Ingestion
- **აღმოჩენა:** concatenating path, checksum, size and MIME fields with tabs/newlines allowed legal
  filenames containing those characters to make distinct manifests serialize to the same pre-hash byte
  stream, weakening package idempotency identity without requiring a SHA-256 collision.
- **გადაწყვეტა:** canonical package checksum now uses length-prefixed UTF-8 strings, fixed-width numeric
  fields and an explicit entry count; computation streams directly into SHA-256.
- **Evidence:** `ArtifactManifestGeneratorTest.packageChecksumUsesUnambiguousLengthPrefixedFields`;
  full `:api:test` PASS (176 tests).

### AIR-2026-021 — Relation ordering declaration was ignored for array bindings

- **სტატუსი:** `READY` / **priority:** `P1` / **owner:** Data Platform
- **აღმოჩენა:** matcher assigned array ordinals in source order even when the approved relation
  declared `ordered=false`, allowing input array permutation to change the persisted edge identity.
- **გადაწყვეტა:** unordered values now receive deterministic ordinals by resolved canonical package path;
  declared ordered relations still preserve source array order.
- **Evidence:** `ArtifactMatcherTest.unorderedArrayValuesUseCanonicalPathOrder` and existing ordered
  source-order test; full `:api:test` PASS (176 tests).

### AIR-2026-022 — File extensions could assert unverified content types

- **სტატუსი:** `READY` / **priority:** `P1` / **owner:** Ingestion Security
- **აღმოჩენა:** ZIP package upload derived media type only from the original filename; a text or
  executable payload named `.pdf` could be registered under an allowed PDF policy. Imported storage
  inventories crossed a second trust boundary without checking bytes against the declared type.
- **გადაწყვეტა:** pinned Apache Tika 4.0.0 core content detection inspects streams without filename or caller
  MIME hints before upload writes and again before package manifest registration, including inventory
  imports. OOXML and OLE2 spreadsheets receive workbook-structure checks so a DOCX cannot pass as
  XLSX. CSV allows plain-text detection because generic MIME detectors do not distinguish delimited
  text from other UTF text. Type mismatches fail before registry registration.
- **Evidence:** `ArtifactContentTypeVerifierTest` covers generated XLS/XLSX workbooks and DOCX→XLSX
  rejection; ZIP spoof rejection and inventory-import spoof rejection in `ArtifactPackageServiceTest`;
  `:api:test` PASS — 184 tests, 0 failures/errors, 1 skipped (67 artifact-related tests). Tika 4.0.0
  plus workbook structure validation is deployed in remote dev; Tika 4.0.0 is in the API classpath,
  and API health is UP.

### AIR-2026-023 — Artifact package admission had no malware verdict or quarantine evidence

- **სტატუსი:** `IN_PROGRESS` / **priority:** `P1` / **owner:** Ingestion Security
- **აღმოჩენა:** upload/import trusted content after MIME and checksum validation without malware
  scanning. A detection result could not be audited in the Data Plane, and scanner outages had no
  fail-closed API outcome.
- **გადაწყვეტა:** `ArtifactMalwareScanner` is a provider boundary; the ClamAV adapter uses framed,
  bounded `INSTREAM` over a nonblocking socket with an overall deadline and byte ceiling. Admission
  requires a CLEAN verdict; unavailable/error/no verdict maps to 503, infected to 422. Infected bytes
  are written only to a deterministic private quarantine key and a hashed-source evidence row in
  migration 090; high-cardinality source paths are not persisted. Low-cardinality verdict metrics are
  emitted. ClamAV TCP must stay on a trusted private network because its protocol has no peer auth or
  transport encryption.
- **Unit evidence:** clean, infected, timeout, byte-limit and no-endpoint cases in
  `ClamAvArtifactMalwareScannerTest`; ZIP and staged inventory fail-closed plus quarantine behavior in
  `ArtifactPackageServiceTest`; API 422/503 mappings in `PlatformArtifactControllerTest`; full
  `:api:test` PASS — 193 tests, 0 failures/errors, 1 skipped (76 artifact-related).
- **Database evidence:** migration 090 applied in the remote Data Plane; transactional replay
  `ops/tests/sql/artifact-malware-quarantine.sql` prints `ARTIFACT_MALWARE_QUARANTINE_PASS` and rolls
  test rows back.
- **Open runtime evidence:** a reachable ClamAV daemon with current signatures and configured
  `StreamMaxLength`, EICAR canary pass/block, live quarantine object/audit read-back and approved
  retention cleanup. Remote dev has only ~1.2 GiB
  currently available; the official ClamAV container guide recommends ≥3 GiB, so scanner service
  provisioning is external until its resource budget or a remote scanner endpoint is approved.
  Malware scanning remains a separate open control and this change does not claim virus-free content.

### AIR-2026-014 — Access package uploads bypassed malware admission

- **სტატუსი:** `VERIFIED` (code + dev runtime) / **priority:** `P0`
- **აღმოჩენა:** `/platform/access/ingest` და `/platform/access/semantic/ingest` ატვირთულ package-ს
  `storeOriginal`-ით წერდნენ malware verdict-ის გარეშე, მაშინ როცა artifact ხაზი fail-closed scan-ს
  მოითხოვს. ეს იყო review snapshot-ის შექმნის გზა — მისი გამოყენება scanner-ის ფაქტობრივი bypass იქნებოდა.
- **გადაწყვეტა:** ერთიანი `ArtifactMalwareAdmission` gate ყველა untrusted upload-ზე storage write-მდე;
  migration 095 (`ACCESS_PACKAGE` quarantine source); `AccessAdmissionExceptionHandler` (422/503).
  შიდა, გენერირებული SQL extract (`PlatformSqlIngestionService`) upload არ არის და scope-ში არ შედის.
- **Evidence:** `docs/evidence/access-admission-and-snapshot-provenance-runtime-2026-09-18.json`;
  `AccessAdmissionControllerTest`, `ArtifactMalwareAdmissionTest`; 231 PASS.
- **შედეგი:** KIDS review snapshot-ის შექმნაც ახლა EXT-6 (clamd) გარე dependency-ზეა დამოკიდებული.

### ADR-009 effect on AIR-2026-023 and AIR-2026-014

- **სტატუსი:** AIR-2026-023 → `DEFERRED` (malware scanning removed); AIR-2026-014 scan part → `DEFERRED`.
- ClamAV rejected; all scanner/admission/quarantine code and configuration removed (ADR-009, `docs/work/DEFERRED-PLANS.md` DP-001).

### AIR-2026-015 — Migration runner re-executed applied migrations on every startup

- **სტატუსი:** `VERIFIED` (dev) / **priority:** `P0` · commit `69aea23`
- **აღმოჩენა:** `executeAndRecord` recorded migration-ებს checksum-ით ამოწმებდა, მაგრამ მაინც ხელახლა
  უშვებდა. 017/020/021 ყოველ startup-ზე ყველა KIDS dataset-ს ახალ `dataset_version`-ს უმატებდა
  (~973 version თითოეულზე, სულ 11 684) და `ingestion_contract`-ს revision 4/5 + `REVIEW_REQUIRED`-ზე
  აბრუნებდა, სანამ შემდეგი migration-ები 8/ACTIVE-ს დააბრუნებდნენ.
- **გადაწყვეტა:** recorded + checksum-equal migration გამოტოვდება (run-once); შეცვლილი checksum კვლავ fail-closed.
  `PlatformSchemaMigrationRunnerTest`.
- **Evidence:** dev ორი restart: `dataset_version` 11 684 → 11 684; contract 8/ACTIVE უცვლელი; health UP.
- **ღია:** production API იგივე runner-ს იყენებს — fix production release-ით უნდა ჩავიდეს (ჩვენ არ ვეხებით).
  დაგროვილი ზედმეტი DRAFT version-ების cleanup — ცალკე, backup-იანი migration-ით (FK-ები contract source-ებზე).

### AIR-2026-016 — KIDS R8 ingestion source registry contradicts its approved table definitions

- **სტატუსი:** `VERIFIED` (dev; migration 096, commit `355eb9a`; owner chose rev-8 reconciliation) / **priority:** `P0`
- **აღმოჩენა:** approved `contract_table_definition` (rev 8): `KIDS_RESOURCE` → `__ent_kids_resource` → canonical
  version 73. rev 8 `contract_revision_source`-ში კი ორივე row არასწორია: 568 `ACCESS.__ent_kids_resource` → 6964
  (067-მა `MAX(version)` აიღო — AIR-2026-015-ის შედეგი), 575 `ACCESS.kids_resource` → 73 (068-მა site contract-ის
  legacy `access_table_name` აიღო; ასეთი ცხრილი R8 package-ში არ არსებობს). ყველა dataset-ზე იგივე ორმაგობაა.
  შედეგი: `POST /platform/access/semantic/ingest` R8 package-ზე → 400 `Contracted Access table not found: kids_goal`
  (preview კი valid), და artifact relation (version 73) ახალ snapshot-ს (6964) არ შეეხებოდა.
- **Side effect:** batch 8 `FAILED` (9 staged load); იგივე checksum-ზე ingest მას ხელახლა გამოიყენებს.
- **გადაწყვეტა (მოლოდინში — owner decision):** rev 8 registry-ის reconciliation approved table definition-ებთან
  (locator = `ACCESS.`+access_table_name, target = canonical_dataset_version_id; გამოუცხადებელი legacy locator-ები
  მოიხსნება) ცალკე migration-ით, ან ახალი revision 9.

### AIR-2026-017 — Publication release gates were asserted by SQL, never evaluated

- **სტატუსი:** `VERIFIED` (dev, commit `c1143c8`; snapshot 52 evaluated → REVIEW_REQUIRED) / **priority:** `P0`
- **აღმოჩენა:** `PlatformPublicationService` 7 gate-ის (`SCHEMA_VALID`, `KEYS_VALID`, `RELATIONS_VALID`,
  `CLASSIFIERS_VALID`, `STATISTICAL_SEMANTICS_VALID`, `RAW_LINEAGE_VALID`, `PUBLICATION_ATOMIC`) PASS-ს მოითხოვს,
  მაგრამ მათ Java კოდი არ აფასებს. migration 072 ყველა snapshot-ს უპირობოდ უწერდა PASS-ს
  (`"source":"KIDS_R8_ACCEPTANCE"`), ხოლო `SEMANTIC_REVIEW`→`REVIEW_REQUIRED` გადასვლა მხოლოდ 052-ში, hardcoded
  ID-ებით, ხდებოდა. AIR-2026-015-მდე runner 072-ს ყოველ startup-ზე უშვებდა, ამიტომ ყოველი ახალი snapshot
  ავტომატურად იღებდა "PASS"-ს — ეს documentation-only claim-ია, doctrine-ით აკრძალული.
- **Containment:** ყალბი PASS არ იწერება; snapshot 52 (`SEMANTIC_REVIEW`, `ARTIFACT_RECONCILIATION=PASS`)
  გამოუქვეყნებელი რჩება.
- **გადაწყვეტა (შემდეგი ნაბიჯი):** generic `ReleaseGateEvaluator` port + თითო gate-ის რეალური შემფასებელი
  (contract/Data Plane მონაცემებზე), governed endpoint `POST /platform/publication/snapshots/{id}/gates`
  (evaluate + record + `SEMANTIC_REVIEW`→`REVIEW_REQUIRED` მხოლოდ ყველა PASS-ზე); 072/052-ის hardcoded
  evidence-ის ჩანაცვლება; tests (PASS/FAIL თითო gate-ზე, replay, checksum-drift).
- **დამოკიდებულება:** publish-ისთვის `PUBLISH_RESOURCE` identity — operator client-ზე `publish.execute` როლის
  მინიჭება auto-mode-მა დაბლოკა (permission grant); საჭიროა მომხმარებლის ცალსახა ნებართვა ან მისი მიერ მინიჭება.

### AIR-2026-024 — Servlet error path re-entered itself without bound

- **სტატუსი:** `READY` / **priority:** `P1` / **owner:** Serving
- **აღმოჩენა:** shared `ErrorController` returned view names (`error/404`). The API host has no template
  engine, so the name became a relative forward (`/error` → `/error/404` → `/error/error/404`); the
  resulting 404 was forwarded to `/error` again by `GlobalExceptionHandler.redirectWeb`. Observed on
  dev as `StackOverflowError` for an unmapped route (checklist 16.15).
- **გადაწყვეტა:** the error endpoint is terminal — a host that ships the template gets its page, any
  other host gets an RFC 9457 `no-store` body; exception text and failing URI are never exposed.
  `redirectWeb` does not forward from a non-`REQUEST` dispatch. The legacy web host keeps its pages.
- **Evidence:** `ErrorPathTerminationTest` (6); `:core:test` 25 PASS, `:api:test` PASS. Dev redeploy
  and unmapped-route smoke pending.

### AIR-2026-025 — Persisted semantic compatibility was never evaluated

- **სტატუსი:** `READY` / **priority:** `P0` / **owner:** Control Plane
- **აღმოჩენა:** `ContractCompatibilityService.compare` read `contract_document_json` from a row whose
  SELECT did not include that column. Both documents were always empty, so metric/unit/aggregation/
  dimension/response breaking changes between persisted revisions were reported as compatible. The
  unit test returned the column from its stub regardless of the SQL, which hid the defect
  (documentation-only claim in completion plan C-01/C-08: "persisted-contract integration").
- **გადაწყვეტა:** the statement selects the document; the test stub now returns only the columns
  the statement asks for, so the defect cannot recur unnoticed.
- **Evidence:** `ContractCompatibilityServiceTest`; full `:api:test` PASS.

### AIR-2026-026 — Site contract revisions had no governed approval and no immutability

- **სტატუსი:** `READY` / **priority:** `P0` / **owner:** Control Plane
- **აღმოჩენა:** a site contract revision became `APPROVED` only by SQL migration. `ContractApprovalGate`,
  `ContractLifecycleOrchestrator` and `persistApprovalEvidence` were tested but never called from a
  runtime path; nothing blocked a breaking revision, and an `APPROVED` row's document/checksum could be
  updated or deleted. C-01 acceptance ("breaking change იბლოკება") was not enforced anywhere.
- **გადაწყვეტა (layer: Control; authority: completion plan C-01, AGENTS.md gate):**
  - migration `099_site_contract_revision_governance.sql`: append-only
    `platform.site_contract_revision_approval` (one checksum-bound row per revision; `BREAKING`
    requires an acknowledgement), trigger `51041` (approved/superseded revisions immutable, only
    `APPROVED→SUPERSEDED`), trigger `51042` (a *new* approval may not leave two `APPROVED` revisions;
    pre-existing rows never block startup or their own repair);
  - `service/contract/approval`: `ContractApprovalCheck` strategies discovered as beans
    (`LIFECYCLE_TRANSITION`, `CHECKSUM_INTEGRITY`, `STRUCTURAL_BINDING`,
    `BREAKING_CHANGE_ACKNOWLEDGEMENT`) over measured `RevisionFacts`; a new rule is a new bean, the
    service and API do not change. `SiteContractRevisionApprovalService` serializes approvals per
    contract (`UPDLOCK,HOLDLOCK`), supersedes then approves in one transaction, appends evidence
    `geostat.contract-approval.v1` and an outbox event; replay of the same checksum is idempotent;
  - API: `POST /api/v1/platform/site-contracts/{code}/revisions/{revision}/approval/preview`
    (no write, 200/422) and `POST …/approval` (`PUBLISH_RESOURCE`; 200/400/404/409/422 RFC 9457).
  - Revision state has one authority: `site_contract_revision.status`. The parallel
    `platform.contract_revision_lifecycle` table (084) is not written by this path.
- **Failure/idempotency/rollback:** any failing check writes nothing; lost race → 409; a wrong approval
  is corrected by a new revision (approved rows are immutable by design).
- **Evidence:** `SiteContractRevisionApprovalServiceTest` (8), `SiteContractRevisionGovernanceMigrationTest`;
  `:api:test` PASS; authorization-surface preflight PASS. SQL Server trigger fixture
  `ops/tests/sql/site-contract-revision-governance.sql` (rollback-only) — **not yet run**; migration 099
  not yet applied on dev.
- **Open follow-ups:** (1) child rows of an approved revision (dataset/field/relation) are not yet
  immutable — migrations 097/098 legitimately backfilled them; needs a bounded decision. (2) revisions
  are still authored only by migration; a governed authoring API must reuse the SQL checksum convention
  (`SHA2_256` over `NVARCHAR`), which differs from `ContractChecksumBinding` (UTF-8). (3) approver ≠
  author segregation needs an author column. (4) this register repeats ids AIR-2026-014…017 for
  different findings — C-14 documentation reconciliation.

### AIR-2026-027 — Registered migration chain cannot rebuild the approved KIDS R8 contract

- **სტატუსი:** `VERIFIED` (2026-09-19, see AIR-2026-038) / **priority:** `P0` (release invariant: ordered migrations, reproducible build — B-01) / **owner:** Control Plane
- **აღმოჩენა (source-verified 2026-09-19):** `PlatformSchemaMigrationRunner` goes `053 → 057`.
  `057` recreates the table of unregistered `054`, and `058` repeats unregistered `056`, but **no
  registered migration replaces `055_kids_final_page_contract_revision.sql`, the only script that
  inserts `KIDS_PORTAL_V1` revision 8** (and its datasets/fields/relations/nodes/page bindings).
  On a fresh Control Plane `058` points the ingestion contract at revision 8 and `059…096` bind
  runtime pages, projections, governance and artifact relations to a revision that does not exist.
  Existing dev/prod databases work only because `055` was applied to them earlier; the platform is
  therefore not reproducible from source. `PlatformSchemaMigrationRegistrationTest.KNOWN_UNREGISTERED`
  documents the gap but does not close it.
- **Why it is not simply registered:** the revision insert in `055` is guarded, its child inserts are
  not. On a database that has revision 8 but no ledger row for `055`, executing it fails on
  `uq_site_contract_dataset` and blocks startup. (The file was presumably unregistered while the runner
  still re-executed every script on each start — AIR-2026-015, now fixed.)
- **Proposed fix (needs one ledger read per environment first):**
  `SELECT migration_id FROM platform.schema_migration WHERE migration_id LIKE '%05[4-6]%'`.
  If `055` is recorded everywhere → register `054/055/056` in numeric position unchanged (run-once
  makes this a no-op there, and a fresh install becomes complete). If not recorded → add an adoption
  step that records an already-present effect without executing, then register. Acceptance: a disposable
  empty Control Plane bootstraps to `APPROVED` revision 8 with 15 datasets, and `KNOWN_UNREGISTERED`
  shrinks to `000` plus the r7-only scripts `037/039/045/047` (each with a written retirement reason).
- **Not changed blind:** no SQL Server is available locally and a wrong guess blocks startup of a shared
  environment; the change waits for the ledger read and a disposable-database replay.

### AIR-2026-028 — Object Storage adapter mixed four responsibilities behind a port with refused methods

- **სტატუსი:** `READY` / **priority:** `P1` / **owner:** Data Platform (layer: Archive/Ingestion storage boundary)
- **აღმოჩენა:** `ObjectStorageService` was at once the legacy ingest store, quarantine/archive writer, bucket
  provisioner, readiness probe and the artifact byte port (SRP). `ArtifactObjectStore` declared resumable-staging
  methods as `default` bodies that throw "not supported" (ISP/LSP: a provider could satisfy the type and fail at
  runtime). The staging namespace `artifacts/staging/` was a literal in the adapter. The port had no listing
  capability, so storage could never be reconciled against the registry (checklist 14.3).
- **გადაწყვეტა:** three narrow ports — `ArtifactObjectStore` (immutable bytes + presign), `ArtifactUploadStagingStore`
  (checkpoints), `ArtifactObjectInventory` (paged listing); S3 adapters `S3ArtifactObjectStore`,
  `S3ArtifactUploadStagingStore`, `S3ArtifactObjectInventory` wired by `S3StorageConfiguration` from typed
  `S3StorageProperties`. A second provider is a second configuration class; no consumer changes. Behaviour and
  object keys are unchanged (staging prefix default equals the former literal), so existing objects stay valid.
- **Evidence:** `ArtifactPolicyAndSigningTest` (signing against the new adapter), `ApiApplicationTests.contextLoads`,
  full `:api:test` PASS. Runtime: dev redeploy pending.

### AIR-2026-029 — Package engine named one dataset format

- **სტატუსი:** `READY` / **priority:** `P1` / **owner:** Ingestion
- **აღმოჩენა:** `ArtifactPackageService` detected the dataset by `extension.equals("accdb")` and called the Access
  validator and row reader directly — a format branch inside the generic engine (AGENTS.md: no hardcoded branching;
  OCP).
- **გადაწყვეტა:** `PackageDatasetCarrier` port (`carries/validate/rows`), `AccessDatasetCarrier` bean, registry
  `PackageDatasetCarriers`; ingestion of the carried rows is the sibling port `PackageDatasetIngestor`. Supporting
  another carrier (CSV bundle, SQLite, Parquet) is a new bean pair.
- **Evidence:** `ArtifactPackageServiceTest`, `ArtifactPackageRelationPreviewTest` PASS through the registry.

### AIR-2026-030 — The package pipeline existed only as seven manual API calls

- **სტატუსი:** `READY` / **priority:** `P1` / **owner:** Ingestion (authority: ARTIFACT-ATTACHMENT-CONTRACT §7, §12, §26)
- **აღმოჩენა:** after admission an operator had to call ingest, validate, prepare-snapshot, materialize, bind,
  reconcile and gates by hand, carrying ids between calls; there was no durable progress, no retry semantics and no
  single idempotency key for one package (checklist 14.1/14.4).
- **გადაწყვეტა:** migration `101_artifact_package_run.sql` (`ingest.artifact_package_run`, unique per manifest;
  append-only `…_run_stage`, trigger 51101). `PackageRunService` executes ordered `PackageRunStage` beans and
  persists state after each; stages exchange named values (`PackageRunState`), so a new stage needs no schema or
  service change. Failure classes: infrastructure → `RETRYABLE` (worker resumes the same stage), pipeline/package
  refusal → `BLOCKED` (operator `retry`), removed stage → `STAGE_NOT_DEPLOYED`. `PackageRunWorker` is lease-guarded
  and renews the lease per run. Publication is deliberately not a stage (ADR-007).
- **Idempotency notes:** every wrapped service is replay-safe except load validation, which would move a `PREPARED`
  load back to `VALIDATED`; `ValidateLoadStage` skips a settled load.
- **Evidence:** `PackageRunServiceTest` (7), full `:api:test` 264 PASS, authorization-surface preflight PASS (36
  policy-bound controllers), schema-agnostic preflight: 0 violations. Runtime acceptance: checklist 17.4.

### AIR-2026-031 — Storage was never reconciled against the registry

- **სტატუსი:** `READY` / **priority:** `P2` / **owner:** Observability/Data Platform
- **გადაწყვეტა:** migration `100_artifact_storage_sweep.sql` + `ArtifactStorageSweepService`: one bounded page per
  tick, cursor per `(bucket,prefix)` scope, scopes derived from registered object keys plus the configured upload
  pool, grace period for in-flight admissions, binary key ordering equal to the listing order, orphan rows resolved
  as `REGISTERED` or `ABSENT`. Evidence only — deletion stays an explicit, backed-up operator decision (AGENTS.md).
- **Evidence:** `ArtifactStorageSweepServiceTest` (5). Runtime: checklist 17.4.

### AIR-2026-032 — Upload session service fused SQL, state rules and orchestration, with no unit test

- **სტატუსი:** `READY` / **priority:** `P1` / **owner:** Ingestion
- **აღმოჩენა:** `ArtifactUploadSessionService` (328 lines) held every SQL statement, compared lifecycle states as string
  literals in eleven places (`"OPEN"`, `"RETRYABLE"`, …), used the wall clock directly and therefore could only be
  exercised against a database. Checklist 7.5 cited the whole suite as its evidence; no test targeted the class
  apart from a schema-readiness check.
- **გადაწყვეტა:** `service/artifact/upload`: `UploadSessionStatus` owns the state rules (`active()`,
  `releasesQuota()`), `UploadSession` owns part geometry (`partStart`, `partLength`, `expiredAt`),
  `UploadSessionRepository` owns persistence with the original statements and lock hints. The service keeps
  orchestration and transaction boundaries and takes an injected `Clock`. Public API, SQL, idempotency fingerprint
  bytes and lazy one-part-at-a-time assembly are unchanged.
- **Evidence:** `ArtifactUploadSessionServiceTest` (11 cases); full `:api:test` 273 PASS.

### AIR-2026-033 — Residual format literal and duplicated digest code

- **სტატუსი:** `READY` / **priority:** `P2` / **owner:** Ingestion
- **გადაწყვეტა:** `MediaTypes` reads types missing from the standard registry from `artifact-media-types.properties`
  (data; append-only because a media type is part of the manifest checksum). `Sha256` is the single digest
  implementation for the artifact line (package staging, S3 adapter, identity pseudonymisation, upload fingerprint).
- **Evidence:** `ArtifactConfigurationTest`, `ArtifactContentTypeVerifierTest`, manifest/package tests PASS unchanged
  (checksums identical).

### AIR-2026-034 — Unexpected exceptions were sanitized for the client and lost for the operator

- **სტატუსი:** `VERIFIED` / **priority:** `P1` / **owner:** Observability
- **აღმოჩენა:** `GlobalExceptionHandler` answered unexpected `Exception`/`RuntimeException` with a generic body and
  wrote nothing to the log. A 500 on dev could not be diagnosed until logging was added.
- **გადაწყვეტა:** both catch-all handlers log method, URI and the exception server-side; the client body is unchanged.
- **Evidence:** the added log line located AIR-2026-035 on dev within one request.

### AIR-2026-035 — Application ObjectMapper routed every untyped value into a self-recursive PageNode deserializer

- **სტატუსი:** `VERIFIED` / **priority:** `P0` / **owner:** Serving
- **აღმოჩენა:** `JacksonConfig` tested `type.getRawClass().isAssignableFrom(PageNode.class)` — true for every
  supertype of `PageNode`, including `Object`. Any `Map<String,Object>` read through the Spring-managed
  `ObjectMapper` therefore used the PageNode deserializer, which calls `mapper.treeToValue(node, PageNode.class)`
  and re-enters itself: `StackOverflowError`. `PageNode` is abstract and is never bound from a request body
  (`NodeRequest` is), so the deserializer could not work for its own type either. The serializer had the same
  reversed test.
- **გადაწყვეტა:** the dead deserializer is removed; the serializer matches `PageNode` and its subtypes only.
- **Evidence:** `JacksonConfigTest` fails with `StackOverflowError` on the old code and passes on the fix; on dev
  `GET /platform/artifacts/package-runs/1` went from 500 to 200. Full `:api:test` 276 PASS.
- **Blast radius:** every service that injects the application `ObjectMapper` and reads untyped JSON was exposed.

### AIR-2026-036 — A shipped manifest was ordinary content and the accepted manifest kept no row lineage

- **სტატუსი:** `VERIFIED` / **priority:** `P1` / **owner:** Ingestion (authority: ARTIFACT-ATTACHMENT-CONTRACT §2, §25)
- **აღმოჩენა:** a `manifest.json` inside a package was stored and manifested like any file and never compared with
  anything; the registered manifest held file identity only, so the row-to-file candidates §25 requires existed
  nowhere until snapshot binding.
- **გადაწყვეტა:** `PackageManifestDocument` (`geostat.artifact-package-manifest.v1`) is shared by the producer tool and
  the API. The assembler ships it; admission treats the root entry as a reserved claim and accepts the package only
  when the claim equals the server-derived manifest and relation plan (no write otherwise). The derived document is
  persisted once per manifest by `ArtifactManifestDocuments` (migration 102) and served by
  `GET /platform/artifacts/manifests/{id}/document`.
- **Evidence:** `ArtifactPackageRelationPreviewTest` (exact / wrong edge / ghost file / wrong schema),
  `ArtifactManifestDocumentsTest`; dev: manifest 7, 451 files, 450 edges
  (`docs/evidence/kids-r8-package-end-to-end-runtime-2026-09-19.json`).

### AIR-2026-037 — Explicit relation-table attachments need a multi-dataset package run

- **სტატუსი:** `READY` (2026-09-19, see AIR-2026-039) / **priority:** `P1` / **owner:** Ingestion + Data Platform
- **აღმოჩენა:** contract §4.2/§4.3 describes attachments declared in package tables (`__raw_document`,
  `__rel_entity_artifact`: N:M, role, ordinal, primary). Only `SOURCE_PATH` exists. The matcher already supports
  ordered 1:N values, so the missing part is not matching but **where the edges come from**: they are rows of another
  dataset of the same package. Preview reads the package file, snapshot binding reads materialized snapshot rows; a
  rule that works in only one of them would let preview and binding disagree, which the line forbids.
- **Decision to make before code:** (1) a relation declares its edge dataset and artifact-envelope dataset by dataset
  code in the match rule; (2) a package run covers every dataset the relation names, from one ingestion batch, and
  binding resolves the sibling snapshots through that batch; (3) the carrier port gains a generic "rows of dataset X"
  read so preview uses the same projection. No engine branch per site is needed.
- **Acceptance:** a synthetic package with 1000 rows and 1500 files (§27), shared files and multi-file rows, gives the
  same edges at preview, in the accepted manifest document and after binding.

### AIR-2026-038 — A migration could be recorded as applied although part of it never ran; an empty database could not be built

- **სტატუსი:** `VERIFIED` / **priority:** `P0` / **owner:** Control Plane + Delivery (release invariant: ordered migrations, reproducible build)
- **აღმოჩენა:** `PlatformSchemaMigrationRunner` executed a script as one JDBC batch and never read its results. A
  driver reports the error of a later statement only while results are read, so such errors were invisible and the
  script was written to the ledger. Proof on dev: `011` was recorded while `contract_code` was still nullable (its
  final `ALTER COLUMN` had failed behind the unique index created just before it). A replay of the registered chain
  on an empty database failed in 7 of 95 scripts and ended without `KIDS_PORTAL_V1` revision 8 (closes AIR-2026-027:
  `054`/`055` were unregistered and `055` is the only creator of that revision).
- **გადაწყვეტა:** `SqlScriptExecutor` drains every result so any failing statement fails the script before it is
  recorded. `011`, `020`, `021`, `079`, `080`, `097` corrected for an empty database (statement order, guarded
  insert, deferred compilation; `098` owns the backfill); `054`/`055` registered; `055` ships a generic adoption
  probe (`<migration>.adopt`: one SELECT returning 1 when the effect exists) so it is never executed twice;
  corrected files are reconciled once through the existing reconciliation list, all other checksum drift still
  fails closed; `103` repairs installations where `011` never finished. The repository `055` was proven to create
  the same revision document dev holds (SHA-256 `94AFFBA7…`, 15/104/21).
- **Evidence:** `ops/tests/sql/migration-chain-fresh-replay.sh` — before 88/95, after **98/98** with r8 `APPROVED`
  (15 datasets); dev after deploy: health UP, no checksum errors, `103` recorded, `contract_code` NOT NULL.
  `docs/evidence/migration-chain-reproducibility-2026-09-19.json`. `SqlScriptExecutorTest`,
  `PlatformSchemaMigrationRegistrationTest` (every unregistered file now carries its reason); `:api:test` 283 PASS.
- **Remaining:** the production ledger was not read; its first deploy of this runner must run the replay tool's
  checks in preflight. Other scripts may hold statements that failed silently on existing databases without
  breaking an empty build; a ledger-versus-schema audit is the way to find them.

### AIR-2026-039 — `RELATION_TABLE` match rule: attachments declared in package tables (contract §4.2, §4.3, §27)

- **სტატუსი:** `READY` (source + acceptance test; no approved contract declares it yet, so there is no runtime run) /
  **priority:** `P1` / **owner:** Ingestion
- **Design actually built (simpler than the multi-dataset run sketched in AIR-2026-037):** the edges are derived once,
  at admission, from the package's relation and artifact tables, are part of the accepted manifest document
  (migration 102), and snapshot binding **replays them from that document**. Row identity is the contract key in both
  places, so no second snapshot and no cross-dataset join is needed, and preview and binding cannot disagree.
- **Pieces:** `RelationTableMatchRule` + parser (`type: RELATION_TABLE`; relation table, entity/artifact key fields,
  ordinal, optional role filter and language field, artifact table, file-name field, `packageRoot`, languages);
  capability `PackageDeclaredValues` (the rule derives its own `DeclaredEdge`s, the engine never names a rule type);
  port `PackageTableReader` (implemented by the Access carrier; a format without tables refuses such a rule);
  `DeclaredRowValues` feeds the values to the **unchanged** `ArtifactMatcher` as ordered package paths, so
  cardinality, missing attachments, orphans, policy and case checks are the existing ones.
- **Fail-closed declarations:** unknown artifact key, relation row for an unknown dataset row, two files on one
  ordinal, ordinal gaps, undeclared language — each refuses the package before any write.
- **Evidence:** `RelationTableMatchRuleAcceptanceTest` — 1000 rows, 1500 files, 1502 edges (rows with two files, one
  file shared by three rows, a foreign-role edge ignored): admission edges = document edges = binding edges; negative
  and parser cases. The KIDS package rebuilt through the shared row view is byte-identical (SHA-256 `427d12e9…`).
  `:api:test` 288 PASS; schema-agnostic preflight 0 violations.
- **To use it for a site:** approve an `artifact_relation_definition` whose `match_rule_json` has
  `"type":"RELATION_TABLE"`; nothing else changes.
