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

### AIR-2026-009 — Legacy request path replacement

- **სტატუსი:** `DISCOVERED` / **priority:** `P1`
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

ახალი აღმოჩენა პირველად იწერება AIR-ში, შემდეგ იქმნება card/ADR/implementation
task. Chat message, issue ან commit message შეიძლება იყოს ბმული, მაგრამ AIR არის
canonical status. კვირეული review ამოწმებს stale `DISCOVERED/DEFERRED` ჩანაწერებს,
დამოკიდებულებებს და evidence-ის ვადაგასულობას.
