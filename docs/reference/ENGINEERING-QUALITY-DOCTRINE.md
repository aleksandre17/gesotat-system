# Engineering Quality Doctrine

- Production-grade — შედეგი უნდა იყოს რეალურად გასაშვები, არა მხოლოდ დემო ან დოკუმენტური.
- Contract-first — კონტრაქტი/metadata არის source of truth; კოდი მას მიჰყვება.
- Metadata-driven — ქცევა განისაზღვრება გამოცხადებული metadata-ით და არა hardcoded KIDS branching-ით.
- Schema/provider/site-agnostic — ახალი provider, site, schema ან data family უნდა დაემატოს core-code-ის გადაკეთების გარეშე.
- Canonical architecture — თითოეულ არტეფაქტს აქვს ერთი owner, ერთი ფიზიკური ადგილი და ერთი authority.
- Layered architecture — Control, Ingestion, Data, Archive, Serving, Security, Observability და Delivery პასუხისმგებლობები გამიჯნულია.
- SOLID და Ports & Adapters — domain/core არ არის მიბმული კონკრეტულ DB-ზე, framework-ზე ან provider-ზე.
- Explicit boundaries — project/service/environment namespace-ები არ ირევა.
- Versioned და immutable — კონტრაქტები, mappings, policies, checksums, snapshots და evidence revision-ებით იმართება.
- Idempotent და fail-closed — განმეორებითი ოპერაცია არ აორმაგებს შედეგს; გაურკვევლობისას სისტემა აჩერებს მოქმედებას.
- Security-by-default — deny-by-default, tenant isolation, authorization, privacy და secret separation.
- Observable და auditable — ყველა მნიშვნელოვანი მოქმედება უნდა ტოვებდეს trace, metric, ledger და evidence-ს.
- Testable — unit, integration, negative, property, security, performance, recovery და conformance tests.
- Backward-compatible — ძველი კონტრაქტის/consumer-ის გავლენა ფასდება migration/deprecation წესით.
- Documentation-as-code — დოკუმენტაცია runtime/migration/contract მდგომარეობას ავტომატურად უნდა ემთხვეოდეს.
- Fail-closed release governance — implementation-ის არსებობა საკმარისი არ არის; საჭიროა test, evidence, approval და reproducible release.

ეს doctrine სავალდებულოა ყველა კოდის, კონტრაქტის, migration-ის, API-ის,
დოკუმენტაციის, ინფრასტრუქტურისა და directory-structure ცვლილებისთვის.

## Cardinal rule — platform identity and deliberate elevation

KIDS ან ნებისმიერი სხვა consumer არის **conformance case**, reference input და
რეალური acceptance scenario — არა პლატფორმის არქიტექტურული სამიზნე. ჩვენ არ
ვაპატარავებთ პლატფორმის დონეს consumer-ის სიმარტივემდე და არ ვამატებთ
consumer-specific გამონაკლისს მხოლოდ სწრაფი ინტეგრაციისთვის. Consumer უნდა
მოერგოს ჩვენს contract-first, metadata-driven, schema/provider/site-agnostic
კანონიკას.

ყოველი ინტეგრაციისას ინჟინერმა სავალდებულოდ უნდა დაინახოს სრული invisible
line: source → contract → policy → ingestion → canonical model → relation /
projection → serving → export → evidence → rollback. ზედაპირული field mapping
ან ერთი endpoint-ის წარმატება საკმარისი მტკიცებულება არ არის.

ყოველი ქეისი უნდა წარმოშობდეს კრიტიკულ კითხვებს და გაუმჯობესების შესაძლებლობას:

- სად შეიძლება გახდეს კონტრაქტი უფრო მკაფიო, ტიპირებული და versionable?
- რომელი implicit semantic, identity, lineage ან privacy assumption იმალება?
- რომელი ნაწილი არღვევს abstraction-ს, portability-ს ან provider interchangeability-ს?
- რა ახალი reusable capability, invariant, test, telemetry ან governance control უნდა დაემატოს?
- როგორ დავამტკიცებთ არა მხოლოდ happy path-ს, არამედ failure, replay, rollback,
  concurrent და cross-tenant შემთხვევებს?

ამ კითხვებზე პასუხის გარეშე ქეისი რჩება `DISCOVERY_REQUIRED` სტატუსში. აღმოჩენილი
gap-ის გამოსწორება ხდება generic capability-ით, contract/mapping revision-ით ან
versioned ADR-ით ისე, რომ გაუმჯობესება ხელმისაწვდომი გახდეს ყველა მომავალი
consumer-ისთვის. KIDS-ისთვის დაწერილი ერთჯერადი branching, hidden fallback ან
quality downgrade არქიტექტურულად არასწორია.

ეს არის უმკაცრესი cardinal invariant: **consumer არასდროს განსაზღვრავს ჩვენს
დონეს; თითოეული consumer გვამოწმებს და გვაიძულებს უფრო მაღალ, უფრო ზოგად და
უფრო დამტკიცებად დონეზე ასვლას.**

### Cardinal schema-evolution mandate

ყველა site-ისთვის გამოცხადებული schema, doctrine, contract და canonical model
არის **გასაუმჯობესებელი design surface**, არა ხელშეუხებელი მოცემულობა. ყოველი
onboarding, reconciliation, migration ან API review-ის დროს სავალდებულოდ უნდა
დაისვას კითხვა: შესაძლებელია თუ არა ამ მოდელის უფრო მკაფიო, მცირე coupling-ის,
მეცნიერულად სწორი, ოპტიმალური და reusable ფორმით წარმოდგენა.

კრიტიკული review უნდა ეძებდეს, evidence-ის საფუძველზე:

- table-ის გაყოფას, გაერთიანებას ან სხვა family-ში გადატანას;
- column-ის დამატებას, ამოღებას, გადარქმევას, ტიპის/nullable/grain-ის შეცვლას;
- დამალული მნიშვნელობის ცალკე classifier/metric/unit/dimension registry-ში
  გამოტანას;
- relation-ის დამატებას, მოხსნას, cardinality/ownership/validity-ის გასწორებას;
- duplicate, derived ან არასწორად დაჯგუფებული მონაცემის ნორმალიზაციას;
- raw, entity, relation, statistical, geo და metadata boundary-ების გამკაცრებას;
- index, keyset, partition, retention, privacy და lineage მექანიზმების
  გაუმჯობესებას;
- ისეთი generic abstraction-ის შექმნას, რომელიც სხვა site/provider-ებსაც
  დაუყოვნებლივ გამოადგებათ.

ეს არ ნიშნავს თვითნებურ schema churn-ს. ყოველი ცვლილება უნდა იყოს
**evidence-driven და versioned**: current-vs-target diff, business/technical
justification, impact/compatibility analysis, migration, rollback, test,
checksum და owner უნდა ჩაიწეროს AIR/ADR-ში. თუ არსებული მოდელი საკმარისია,
review-მ ესეც უნდა დააფიქსიროს მტკიცებულებით. დაუმტკიცებელი “გაუმჯობესება” და
consumer-ისთვის ხარისხის დაკლება ერთნაირად დაუშვებელია.

**უმაღლესი წესი:** site-ის სქემა პლატფორმას არ კარნახობს. პლატფორმა კრიტიკულად
ამოწმებს site-ის სქემას, პოულობს უკეთეს სტრუქტურულ შესაძლებლობას და მხოლოდ
დამტკიცებული, უკუთავსებადი/მიგრირებადი გზით ტოვებს საბოლოო კანონიკას.

### Meta-schema supremacy and self-evolution

ეს review ვრცელდება თვითონ **უმაღლეს სქემაზეც** — meta-schema/control-plane
schema-ზე, რომელიც განსაზღვრავს dataset, field, key, relation, projection,
policy, lifecycle და evidence არტეფაქტების აგებას. Meta-schema არ არის
ხელშეუხებელი ინფრასტრუქტურა: თუ ახალი site/provider ან ქვედა დონის აღმოჩენა
აჩვენებს, რომ მას აკლია primitive, constraint, relation type, semantic type,
versioning ან governance capability, ცვლილების განხილვა აუცილებლად ადის ამ
დონემდე.

Meta-schema-ის გაუმჯობესებისას სავალდებულოა:

- current-vs-target meta-model diff და backward-compatibility matrix;
- ყველა downstream contract/Access/SQL/API generator-ის impact graph;
- versioned migration და dual-read/dual-write ან compatibility adapter, სადაც
  საჭიროა;
- meta-schema validator-ის, generator-ის, introspection-ის და documentation-ის
  ერთიანი განახლება;
- property/conformance tests, rollback და immutable evidence;
- proof, რომ ცვლილება generic capability-ა და არა ერთი site-ის workaround.

თუ meta-schema ზღუდავს საჭირო გაუმჯობესებას, ქვედა schema-ს “მორგება” არ არის
მისაღები გამოსავალი — უნდა გაუმჯობესდეს თვითონ meta-schema, ან blocker
დარეგისტრირდეს და განვითარება შეჩერდეს. ამ თვითგანვითარებადობას აქვს ერთი
ზღვარი: თვითონ ცვლილება ვერ შევა ძალაში საკუთარი governance/validation/evidence
გზის გარეშე.

## Data-family design rule — prefix is a boundary, not a shortcut

`__raw_`, `__cl_`, `__stat_`, `__ent_`, `__rel_` და `__gs_` არის Access
transport namespace-ები. Prefix ეხმარება ოპერატორს ოჯახის ამოცნობაში, მაგრამ
თავისთავად არ არის schema, business rule ან canonical SQL model. საბოლოო Data
Plane-ში ეს პასუხისმგებლობები generic schema-ებად და registry metadata-ად უნდა
არსებობდეს.

ყოველი table-ისთვის სავალდებულოა ერთი მოკლე passport: purpose და row grain;
surrogate და natural/business key; owner და lifecycle; source artifact/locator,
ingest batch, contract/revision; PK/UK/FK/null/domain/temporal წესები;
classification/metric/unit/dimension-ის registry კავშირები; relation cardinality
და validity; index/access-pattern მოთხოვნა; tenant/privacy/redaction policy;
checksum, row-count, reconciliation, migration და rollback evidence.

შევსება არ უნდა ნიშნავდეს SQL-ის ხელით წერას. პროფესიული გზა არის generated
template/form: სავალდებულო ველები, მაგალითები, code-list არჩევანი, relation
picker და preflight validation. საბოლოო Access/SQL artifact generator-მა უნდა
შექმნას. ერთი passport ერთნაირად მუშაობს `raw`, `classification`, `statistics`,
`entity` და `relation` ოჯახებისთვის, ამიტომ ახალი site/provider core-code-ის
გადაკეთებას არ საჭიროებს.

**Design test:** თუ table-ის დანიშნულება, row grain, key, lineage და შევსების
წესი ერთ გვერდზე ვერ აიხსნება და validator-ით ვერ მოწმდება, იგი
production-ready არ არის — თუნდაც სახელი და prefix სწორი ჰქონდეს.

## Critical architecture review protocol

### Proactive defect-and-opportunity detection

ინჟინერი არ ელოდება მომხმარებლის მიერ ხარვეზის დასახელებას. ყოველი ქეისის
დაწყებისას და დასრულებისას უნდა ჩატარდეს მიზანმიმართული “smell detection” —
დამალული სუნის ძებნა: implicit coupling, დუბლირებული მონაცემი ან ლოგიკა,
hardcoded სახელები, დაუფიქსირებელი semantic assumption, drift, არასრული
lineage, უსაფრთხოების bypass, unbounded query, არასაკმარისი rollback და ისეთი
შესაძლებლობა, რომელიც მხოლოდ ერთ consumer-ში ჩანს, მაგრამ generic capability-ად
შეიძლება გადაიქცეს.

ეს შემოწმება სრულდება ოთხი მიმართულებით: **structure** (სად რა დევს და ვის
ეკუთვნის), **semantics** (რას ნიშნავს თითოეული ველი/კავშირი), **execution** (რა
ხდება runtime-ში failure/concurrency/load-ის დროს) და **governance** (ვინ
ამტკიცებს, რა evidence რჩება და როგორ ბრუნდება უკან). თითოეული აღმოჩენა —
ხარვეზი, რისკი ან გაუმჯობესების შესაძლებლობა — იმავე სამუშაო სესიაში უნდა
დარეგისტრირდეს [Architecture Improvement Register]-ში შესაბამისი source,
priority, owner და lifecycle სტატუსით. “ვნახეთ, მაგრამ მერე გავაკეთებთ” ჩანაწერის
გარეშე დაუშვებელია.

### Blocking-discovery rule (fail-closed)

თუ აღმოჩენილი საკითხი არის **BLOCKER** — მისი უგულებელყოფა გამოიწვევს არასწორ
განვითარებას, დაარღვევს უსაფრთხოების/მონაცემის/კონტრაქტის invariant-ს, ან
შემდეგი ნაბიჯი მის გარეშე ვერ იქნება ვალიდური — მიმდინარე სამუშაო დაუყოვნებლივ
ჩერდება. საკითხი AIR-ში რეგისტრირდება `priority: P0`, `status: TRIAGED`-ით,
იზოლირდება მისი blast radius და იწყება გამოსწორება ან უსაფრთხო containment.

შემდეგ ეტაპზე გადასვლა აკრძალულია, სანამ blocker-ს არ ექნება verified fix,
დამოუკიდებელი evidence, regression test და საჭირო rollback plan. დროებითი
გამონაკლისი დასაშვებია მხოლოდ versioned ADR-ით, owner-ით, ვადის/trigger-ის და
compensating controls-ის მითითებით; ჩუმი bypass ან “მოგვიანებით” გადადება
ვალიდურ გადაწყვეტილებად არ ითვლება.

ყოველი ახალი ქეისი გადის ოთხი ხედვის ერთობლივ შემოწმებას:

1. **Scientific correctness** — არის თუ არა grain, identity, time, unit,
   dimension, aggregation და null semantics ერთმნიშვნელოვანი და რეპროდუცირებადი;
2. **Systems correctness** — ჩანს თუ არა სრული chain source → contract → policy
   → storage → relation → projection → response → evidence → rollback;
3. **Engineering quality** — არის თუ არა abstraction reusable, provider-neutral,
   idempotent, observable, testable და bounded რესურსებში;
4. **Organizational operability** — შეძლებს თუ არა სხვა ინჟინერი სისტემის
   გაგებას, შევსებას, შეცდომის დიაგნოსტიკას და უსაფრთხო rollback-ს ერთიანი
   passport/template-ით.

Review-ის დროს ინჟინერმა აქტიურად უნდა მოძებნოს დამალული გაუმჯობესება:

- implicit ველი ან business rule უნდა გადაიქცეს typed contract metadata-ად;
- განმეორებადი mapping ან adapter უნდა გადაიქცეს reusable registry/capability-ად;
- ხელით შესავსები ან შეცდომისკენ მიდრეკილი ნაბიჯი უნდა გადაიქცეს generator,
  preflight ან schema lint-ად;
- შესაძლო drift-ს უნდა ჰქონდეს checksum/cardinality/semantic alarm;
- ყველა რთულ ქეისს უნდა ჰქონდეს differential, property, negative, replay და
  concurrency test;
- performance, privacy და lineage მოთხოვნები უნდა იყოს query admission-სა და
  response policy-ში, არა მხოლოდ დოკუმენტაციაში;
- აღმოჩენილი გაუმჯობესება უნდა გავრცელდეს generic core-ზე და არ დარჩეს
  consumer-specific patch-ად.

Review-ის დასასრული არის არა მხოლოდ `PASS/FAIL`, არამედ **improvement record**:
რა აღმოვაჩინეთ, რომელი invariant დავამატეთ, რომელი reusable capability გაჩნდა,
რომელი ტესტი/ტელემეტრია ადასტურებს და რომელი consumer-ები მიიღებენ სარგებელს.
თუ ასეთი record არ არსებობს, ქეისი სრულად გააზრებულად არ ითვლება.

## Consumer-conformance rule

No reference site, legacy client or pilot product may lower the platform's
quality bar. Consumers conform to the canonical contract and its security,
lineage, semantic and evidence requirements. The platform core is never
weakened, forked or hardcoded to fit one consumer. A mismatch is handled by an
anti-corruption adapter, an approved contract/mapping revision or a governed
migration. When consumer behavior conflicts with a platform invariant, the
invariant wins and the consumer is changed.

## Non-regression and continuous-improvement rule

Integration must never trade away an existing platform guarantee for consumer
convenience, legacy compatibility or delivery speed. Every onboarding path must
preserve all declared principles, controls, boundaries, evidence requirements
and quality gates, and should improve them where the consumer exposes a gap.
KIDS and every future project are required to conform completely to this
doctrine. A compatibility adapter may translate representations, but may not
weaken security, semantics, lineage, validation, observability, portability,
abstraction or release governance. Any proposed exception requires an explicit
versioned ADR, owner, expiry date and compensating controls; an undocumented
exception is invalid.

## General engineering standards

- **SOLID** — Single Responsibility, Open/Closed, Liskov Substitution,
  Interface Segregation და Dependency Inversion.
- **Clean/Hexagonal Architecture** — domain core ცენტრში; adapters და
  infrastructure გარეთ; dependency direction ყოველთვის inward.
- **DDD და bounded contexts** — domain language, aggregate boundaries,
  entities, value objects, domain services და explicit anti-corruption layers.
- **Design patterns** — მხოლოდ საჭიროებისას: Strategy, Adapter, Factory,
  Ports & Adapters, Repository, Specification, State Machine, Command,
  Pipeline, Saga/Outbox და Circuit Breaker.
- **GRASP და cohesion/coupling discipline** — მაღალი cohesion, დაბალი coupling,
  მკაფიო ownership და ცვლილების მინიმალური blast radius.
- **API standards** — versioned contract, OpenAPI/JSON Schema, RFC 9457
  errors, idempotency keys, pagination, caching, content negotiation და
  backward-compatible evolution.
- **Data standards** — explicit identity, keys, constraints, normalization,
  lineage, provenance, temporal semantics, quality rules და migration discipline.
- **Security standards** — least privilege, deny-by-default, OWASP ASVS/API
  principles, secret separation, threat modeling, auditability და privacy by design.
- **Reliability standards** — timeout, retry budget, idempotency, circuit
  breaker, bulkhead, graceful degradation, rollback და disaster recovery.
- **Testing standards** — test pyramid, unit/integration/contract/property/
  negative/security/performance/chaos/recovery/conformance coverage.
- **Observability standards** — structured logs, metrics, traces, correlation
  IDs, OpenTelemetry, redaction, SLO/error budget და actionable alerts.
- **Delivery standards** — 12-factor configuration, immutable artifacts,
  reproducible builds, SBOM/provenance, CI gates, progressive rollout და
  evidence-based release.
- **Documentation standards** — Diátaxis, ADR/MADR, docs-as-code, canonical
  ownership, versioning და zero-drift validation.
- **Improvement governance** — ყველა აღმოჩენა და გაუმჯობესების შესაძლებლობა
  გადის canonical Architecture Improvement Register-ში (`docs/work/
  ARCHITECTURE-IMPROVEMENT-REGISTER.md`) და მხოლოდ evidence-იანი lifecycle-ით
  იხურება.

ეს სტანდარტები გამოიყენება კონტექსტის შესაბამისად; არცერთი pattern ან
abstraction არ ემატება მხოლოდ ფორმალური შესაბამისობისთვის. არჩევანი უნდა იყოს
პრობლემის ბუნებით გამართლებული და acceptance evidence-ით დადასტურებული.

## Documentation-as-learning cardinal

ყველა ახალი ტექნიკური დოკუმენტი უნდა იყოს ერთდროულად reference და სასწავლო
მასალა. ავტორმა უნდა ააგოს ტექსტი საბაზისო ცნებებიდან evidence-მდე და უპასუხოს
მკითხველის აუცილებელ კითხვებს:

1. რა არის საკითხი მარტივი ენით?
2. რა პრობლემას აგვარებს და რატომ არის საჭირო?
3. რომელ layer-ს, owner-სა და boundary-ს ეკუთვნის?
4. რა არის input, output და lifecycle?
5. რომელი ტექნოლოგია, მეთოდოლოგია და საერთაშორისო სტანდარტი გამოიყენება?
6. რა ალტერნატივები და trade-off-ები არსებობდა?
7. რა შეიძლება გაფუჭდეს და როგორ მუშაობს fail-closed გზა?
8. როგორ ვტესტავთ, ვზომავთ და ვამტკიცებთ შედეგს?
9. როგორ ხდება versioning, migration, deprecation და retirement?

სავალდებულო სასწავლო მიმდევრობაა:

```text
foundation → concept → architecture → mechanism → example
→ failure modes → implementation → tests → evidence → operations
```

ეს წესი მოქმედებს KIDS-ისა და ყველა მომავალი provider/site-ის დოკუმენტაციაზე.
