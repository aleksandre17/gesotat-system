# GEOSOTAT Engineering Policy

ეს არის repository-ის სავალდებულო, გლობალური engineering policy. იგი ვრცელდება ყველა ახალ ფუნქციაზე, bug fix-ზე, migration-ზე, contract-ზე, API-ზე, data family-ზე, deployment-ზე და დოკუმენტზე.

## ძირითადი წესი

ყოველი პრობლემა უნდა გადაწყდეს მისი ზუსტი ბუნებისთვის საუკეთესო ინდივიდუალური მიდგომით: production-grade, canonical, contract-first, schema/provider/site-agnostic, მკაფიო აბსტრაქციით, SOLID პრინციპებით, უსაფრთხოდ, versioned-ად, idempotent-ად, observable-ად, testable-ად და audit evidence-ით. abstraction არ უნდა დაემატოს მხოლოდ abstraction-ისთვის; hardcoded business branching, დაუმტკიცებელი SQL, credential-ები, დუბლირებული semantic meaning და documentation-only claims დაუშვებელია.

### მუდმივი ხარისხის doctrine

ყველა ახალი პრობლემა, ცვლილება და გადაწყვეტილება უნდა შესრულდეს კონკრეტული პრობლემის ბუნებისთვის ყველაზე შესაფერისი ინდივიდუალური მიდგომით — სრული, production-grade, canonical, პატერნულად გამართული, არქიტექტურულად სწორი, გრამატიკულად და სტრუქტურულად მკაფიო, კანონიკური, schema/provider/site-agnostic, აბსტრაქტული მხოლოდ საჭირო საზღვრებში, კონცეპტუალურად თანმიმდევრული, ორგანიზებული, SOLID-თან თავსებადი, უსაფრთხო, versioned, idempotent, observable, testable და audit evidence-ით გამაგრებული.

ეს ნიშნავს:

- არ გამოვიყენოთ ერთი უნივერსალური შაბლონი ყველა პრობლემაზე; ავირჩიოთ შესაბამისი pattern, boundary და ownership;
- abstraction იყოს მინიმალური, მკაფიო და პრაქტიკული — არა abstraction-ის რაოდენობა, არამედ სწორი კონტრაქტი და დაცული invariant არის მიზანი;
- ყველა გადაწყვეტილებას ჰქონდეს bounded scope, პასუხისმგებლობის layer, authority contract, failure/rollback/idempotency წესი და measurable acceptance;
- არ დარჩეს hardcoded business branch, დაუმტკიცებელი SQL, დუბლირებული semantic meaning, credential ან მხოლოდ დოკუმენტური claim;
- საბოლოო შედეგი უნდა იყოს არამხოლოდ მოქმედი, არამედ გასაგები, გაფართოებადი, backward-compatible, საერთაშორისო ნორმებთან თავსებადი და მომავალი site/provider/data family-ისთვის მზად;
- სანამ ხარისხი, უსაფრთხოება, სტრუქტურა, ურთიერთკავშირები და evidence ერთად არ არის დადასტურებული, ცვლილება არ ჩაითვალოს დასრულებულად.

## ცვლილების სავალდებულო gate

ცვლილება მზად არ არის, სანამ წერილობით და ტესტით არ არის პასუხი გაცემული:

1. რა ზუსტ პრობლემას აგვარებს და რა არის bounded scope?
2. რომელ layer-ს ეკუთვნის — Control, Ingestion, Data, Archive, Serving, Security, Observability თუ Delivery?
3. რომელი approved contract/policy/schema არის authority?
4. იმუშავებს თუ არა სხვა site/data family/provider/schema-ზე core-code branching-ის გარეშე?
5. ინარჩუნებს თუ არა canonical semantics-ს, identity-ს, relation-სა და versioning-ს?
6. რა არის failure mode, retry, idempotency, timeout და rollback?
7. როგორ არის დაცული authentication, authorization, privacy, tenancy და query cost?
8. რა migration, compatibility და deprecation წესია საჭირო?
9. რომელი unit, integration, negative, property, security, performance და recovery test ამტკიცებს ცვლილებას?
10. რომელი runtime, machine-readable და documentation evidence ახლდება?

## აკრძალული პრაქტიკა

- caller-supplied SQL/table/column/expression;
- Access artifact-ში credentials, host, password ან executable business logic;
- KIDS-specific branch generic engine-ში;
- live publication approval/gate-ის გარეშე;
- anonymous ingest, approve, publish ან confidential/raw access;
- ყველა data family-ის ერთ დაუგეგმავ JSON/EAV table-ში მოთავსება;
- source-ის გარეთ არსებული ცვლილების production-complete-ად გამოცხადება;
- generated artifact-ის source-of-truth-ად გამოყენება;
- destructive cleanup explicit scope-ისა და backup-ის გარეშე.

## Release invariant

Production release-ს აუცილებლად უნდა ჰქონდეს:

- clean tagged source commit;
- reproducible build და immutable image digest;
- ordered migrations;
- contract/policy compatibility PASS;
- authentication/authorization/tenancy negative suite PASS;
- quality/privacy/reconciliation evidence;
- observability/SLO evidence;
- backup/restore/rollback/replay evidence;
- SBOM, vulnerability scan და provenance attestation;
- current documentation linked to the deployed revision.

თუ რომელიმე მტკიცებულება აკლია, სტატუსი არის `NOT READY`, მიუხედავად იმისა, რომ ცალკეული endpoint ან KIDS flow მუშაობს.

## მუშაობის პროტოკოლი

0. ყოველი სესიისა და ნებისმიერი სამუშაოს დაწყებამდე წაიკითხე `docs/reference/CANONICAL-FULL-TREE.md` — ეს არის directory layout-ის პირველი და სავალდებულო რუკა.
1. შემდეგ წაიკითხე `docs/platform-capability-and-architecture-audit-2026-09-13.md` და მოქმედი status/contract დოკუმენტები.
2. დააფიქსირე baseline და რისკი.
3. გააკეთე ყველაზე მცირე, სწორი layer-specific ცვლილება.
4. დაამატე შესაბამისი test/evidence.
5. განაახლე checklist, contract და documentation.
6. არ მონიშნო `DONE`, სანამ runtime ან reproducible test არ ადასტურებს შედეგს.

## Canonical references

- `docs/reference/ENGINEERING-QUALITY-DOCTRINE.md` (სავალდებულო ხარისხის doctrine)
- `docs/platform-capability-and-architecture-audit-2026-09-13.md`
- `docs/reference/CANONICAL-FULL-TREE.md` (პირველი წასაკითხი directory authority)
- `docs/reference/CANONICAL-FULL-TREE-DESIGN.md`
- `docs/release-provenance-inventory-2026-09-13.md`
- `scripts/release-gate.ps1`
- `documentation/complete-package/PACKAGE-INDEX.html`
