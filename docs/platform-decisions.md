# Platform Decisions — დამტკიცებული საწყისი გადაწყვეტილებები

## ADR-001 — Database topology

**გადაწყვეტილება:**

```text
geostat-system  → SQL Server Control Plane
geostat-data    → SQL Server Shared Active Data Plane
geostat-archive → SQL Server Immutable Historical Archive
S3 storage      → source files, exports, large binary artifacts
```

ახალი portal/site არ ქმნის ახალ database-ს. იგი ხდება `data_product` shared `geostat-data` ბაზაში. მხოლოდ სამართლებრივი isolation ან უკიდურესი მოცულობა იძლევა მომავალ shard-ს; shard ინარჩუნებს იმავე schema-ს.

## ADR-002 — External file authority

**გადაწყვეტილება:** ფაილი შეიძლება შეიცავდეს source data-ს და declarative source contract-ს, მაგრამ ვერ მიიღებს თვითონ გადაწყვეტილებას ახალი schema/classifier/metric-ის გამოქვეყნებაზე.

| შემთხვევა | მოქმედება |
|---|---|
| არსებული approved contract | ავტომატური validate → import → policy-ით publish/review |
| ახალი field, code, relation | preview + steward approval |
| breaking schema/unit/grain ცვლილება | ახალი version + explicit approval |
| unknown source | quarantine/catalog, public data არ იცვლება |

ეს უზრუნველყოფს „გარედან შესაძლებელი იყოს“ და ამავდროულად იცავს სისტემას დაუდეკლარირებელი მნიშვნელობის, შეცდომის ან მავნე payload-ისგან.

## ADR-003 — History and archive

`geostat-data` ინახავს აქტიურ published snapshots და მოკლე/საშუალო staging window-ს. `geostat-archive` ინახავს immutable historical snapshots, lineage და audit retention-ს. ორიგინალი Access/CSV/XLSX/JSON binary არასოდეს ინახება SQL Server blob-ად; ინახება S3 storage-ში checksum/version/object-lock policy-ით.

**Retention გადაწყვეტილება:** source artifact, archived data snapshot და quarantine artifact ინახება **1 წელი**. 1 წლის შემდეგ მათი purge კეთდება lifecycle job-ით მხოლოდ მაშინ, როცა არსებობს დამოწმებული checksum/audit trail. პატარა Core metadata — contract, schema version, publication identity, actor და audit event — არ იშლება, რადგან ის აუცილებელია lineage/reproducibility-სთვის.

## ADR-004 — Scale baseline

პლატფორმა იგეგმება მილიონობით row-ზე: background ingestion queue, batch processing, time/product partitioning, typed analytical indexes, metric aggregate cache და read/write workload isolation. საბაზო schedule არის **თვეში ერთხელ**, დამატებით დაშვებულია ხელით დაწყებული import; near-real-time pipeline ახლა არ გვჭირდება.

## ADR-005 — პირველი migration pilot

პირველი end-to-end pilot არის `kids` source. იგი source-only/read-only რჩება; ახალ platform-ში გადადის Access/API ingestion → shared data plane → archive → publication flow-ით.

## S3 bucket layout

```text
geostat-ingest       immutable uploaded source artifacts
geostat-quarantine   invalid/unapproved artifacts; restricted access
geostat-archive      retained package/snapshot exports
geostat-export       generated user exports; expiration policy
```

Key convention:

```text
<product-code>/<dataset-code>/<YYYY>/<MM>/<batch-id>/<checksum>-<original-name>
```

## ADR-006 — Production object storage deployment

**გადაწყვეტილება:** MinIO გაეშვება API container-ისგან განცალკევებულ private storage node/VM-ზე, persistent encrypted volume-ით. S3 API API-service-სთვის ხელმისაწვდომია მხოლოდ private network/TLS-ით; MinIO Console არ გამოდის public ინტერნეტში.

```text
Primary:  dedicated MinIO storage volume/node
Backup:   ყოველდღიური encrypted off-site backup/replication
Access:   service account per application role; least privilege bucket policy
Recovery: ყოველთვიური restore test
```

## ADR-007 — Publication policy

**გადაწყვეტილება:** ნაგულისხმევი მდგომარეობაა `REVIEW_REQUIRED`. დამტკიცებული contract-ის import შეიძლება ავტომატურად გაიაროს catalog, mapping, type, relation და quality validation, მაგრამ public publication მოითხოვს publisher/steward approval-ს. მხოლოდ ცალკე მონიშნულ trusted machine feed-ს შეიძლება ჰქონდეს `AUTO_PUBLISH` policy, მკაცრი quality SLA-ით.

## ADR-008 — Artifact identity, attachment and distribution

**სტატუსი:** ACCEPTED (2026-09-18) · **Authority:** `docs/reference/ARTIFACT-ATTACHMENT-CONTRACT.md`

**გადაწყვეტილება:**

1. **Content identity = SHA-256.** `ingest.artifact_object` ერთი row-ია ერთ უნიკალურ ბაიტ-შიგთავსზე; object key არის `<prefix>/<sha256>.<ext>`. ორიგინალი სახელი და path ინახება `ingest.artifact_version`-ში (manifest-ის ფარგლებში). ეს ავრცელებს ADR-ის „S3 bucket layout“ key convention-ს განაწილებად (distribution) artifact-ებზე: dedup, idempotent re-upload და path-collision-ის გამორიცხვა. Access package-ის upload receipt (`ingest.artifact`, `incoming/<date>/<uuid>-name`) სხვა grain-ია და უცვლელი რჩება.
2. **Attachment = typed edge.** `entity.artifact_attachment` (entity → artifact_version, relation code, role, language, ordinal, snapshot). მიბმის წესი ცხადდება მხოლოდ Control Plane-ში (`platform.artifact_relation_definition` + `platform.artifact_policy`); site-specific ცხრილი ან branch აკრძალულია.
3. **Match = დეკლარირებული, ზუსტი.** `SOURCE_PATH` წესი: NFC, prefix strip, package root. case-insensitive ან „უახლოესი“ დამთხვევა არასდროს ხდება ავტომატურად — ყოველი გაურკვევლობა `ERROR` issue-ა და binding ჩერდება.
4. **Publication gate.** `ARTIFACT_RECONCILIATION` PASS სავალდებულოა ყველა snapshot-ისთვის, რომლის dataset version-ს approved artifact relation აქვს; PASS evidence შეიცავს attachment-set checksum-ს და publication მოწმდება მიმდინარე set-თან.
5. **Distribution.** API აბრუნებს მხოლოდ metadata-სა და მოკლევადიან presigned GET-ს (`signed_url_ttl_seconds` policy-დან, 30–3600). URL იწერება `STORAGE_PUBLIC_ENDPOINT` host-ზე; მისი არარსებობისას distribution fail-closed (503). Edge `files.geostat.internal` ატარებს მხოლოდ signed GET/HEAD-ს.
6. **Access.** `PUBLIC_WHEN_PUBLISHED | AUTHENTICATED | RESTRICTED`. API boundary ამჟამად authenticated-ია (`READ_RESOURCE`); anonymous გახსნა მოითხოვს ცალკე security decision-ს (EXT-1).
7. **Retention.** Policy-level `retention_class`. ADR-003-ის 1-წლიანი purge ეხება source/quarantine/archive artifact-ებს; published distribution artifact, რომელსაც მოქმედი snapshot მიუთითებს, არ იშლება (`RETAIN_INDEFINITE` ან `RETAIN_WHILE_REFERENCED`).

**Trade-off:** checksum-addressed key კარგავს „ადამიანისთვის წასაკითხ“ ფიზიკურ სახელს — ეს განზრახულია; სახელი ინახება metadata-ში და `Content-Disposition`-ით ბრუნდება ჩამოტვირთვისას.

## ADR-009 — ClamAV უარყოფილია; malware scanning ამოღებული და გადადებულია

**სტატუსი:** ACCEPTED (2026-09-18) · **Owner:** პროექტის მფლობელი · **ანაცვლებს:** AIR-2026-023, AIR-2026-014 (scan ნაწილი), checklist 4.8 / 15.2 / 15.4 / EXT-6

**გადაწყვეტილება:**

1. **ClamAV / `clamd` უარყოფილია** — არ ვიყენებთ არც როგორც service-ს, არც როგორც dependency-ს.
   მიზეზი: გამოყოფილი ≥3 GiB რესურსი და ცალკე private-network daemon-ის ოპერირება ამ ეტაპზე
   მიზანშეუწონელია; shared dev host-ზე ~1 GiB თავისუფალი მეხსიერებაა.
2. **Malware scanning მთლიანად ამოღებულია კოდიდან** — scanner port, admission gate, quarantine
   registry, `422/503` scanner mapping, metrics და `platform.artifacts.malware-*` / `PLATFORM_ARTIFACT_MALWARE_*`
   კონფიგურაცია. ფუნქცია გადატანილია **გადადებულ გეგმებში** (`docs/work/DEFERRED-PLANS.md`, DP-001).
3. **Schema:** migrations 090/095 immutable ledger-შია და რჩება; `ingest.artifact_quarantine`
   ცხრილი უმოქმედოა (კოდი მასში არ წერს). ფიზიკური წაშლა — მხოლოდ ცალკე migration-ით, backup-ით.

**Compensating controls (მოქმედი):** მხოლოდ authenticated `WRITE_RESOURCE` ატვირთვა; content-only
MIME/signature შემოწმება (Tika) extension-ის ნდობის გარეშე; entry/package ზომისა და რაოდენობის ლიმიტი
(zip-bomb); path traversal validation; approved-contract Access სტრუქტურის შემოწმება write-მდე; SHA-256
content addressing და verification; private bucket-ები; მხოლოდ მოკლევადიანი signed download.

**Trade-off / რისკი:** ატვირთული ფაილის შიგთავსი ანტივირუსით არ მოწმდება. რისკი მისაღებია მხოლოდ
სანდო, ავტორიზებული ოპერატორების ატვირთვისთვის.

**ხელახლა განხილვის trigger:** anonymous/გარე მომწოდებლის upload, public production release, ან
ორგანიზაციული scanner სერვისის ხელმისაწვდომობა.

## ADR-010 — Tenant-scoped authorization (ABAC)

**სტატუსი:** ACCEPTED (2026-09-19) · სრული ტექსტი: `docs/decisions/ADR-tenant-scoped-authorization.md`

product ეკუთვნის ერთ tenant-ს; caller-ის tenant claim უნდა ემთხვეოდეს. `TenantAccessPolicy`
deny-by-default; coverage არის დეკლარირებული და ტესტით დაცული თვისება; cross-tenant გადასვლა
მხოლოდ `PLATFORM_CROSS_TENANT` authority-ით და audit log-ით.

## ADR-011 — Legacy surface retirement

**სტატუსი:** ACCEPTED (2026-09-19) · სრული ტექსტი: `docs/decisions/ADR-legacy-surface-retirement.md`

ADR-010 §5-ის 12 `LEGACY` ზედაპირი governed platform-ის ნაწილი არ არის. ოთხი ოჯახი
(`ACCESS_UPLOAD`, `SPREADSHEET_CONVERSION`, `DATABASE_EXPORT`, `DEMO`), თითოეული ერთი feature
switch-ით, **default-ად გამორთული** → 410 Gone + RFC 9457. ჩართულიც მხოლოდ ცალკე ოპერატორული
authority-თ (`PLATFORM_LEGACY_OPERATOR`, IdP role `platform.legacy`) მიიღწევა. caller-supplied
egress (HTTP fetch, JDBC host) მოითხოვს allow-list-ს — ცარიელი სია ფუნქციას თიშავს — და
DNS rebinding გამორიცხულია ერთჯერადი resolution-ით და pinned socket-ით. prod profile-ზე
ჩართული ოჯახი აჩენს startup WARNING-ს, არა failure-ს.

## ADR-012 — Integrity automation inside an Access package

**სტატუსი:** ACCEPTED (2026-09-20) · სრული ტექსტი: `docs/decisions/ADR-access-package-integrity-automation.md`

Access პაკეტს შეუძლია მთლიანობის შემოწმება თავის შიგნით, მაგრამ ვერ ხდება კანონიკური
ავტორიტეტი: მას შეუძლია metadata schema-ზე მითითება, არა მისი გამოცხადება ან დამტკიცება.

## ADR-013 — Repository Control Protocol

**სტატუსი:** ACCEPTED (2026-09-20) · სრული ტექსტი: `docs/decisions/ADR-repository-control-protocol.md`

პროექტის მდგომარეობა ცხოვრობს ფაილებში, არა საუბრის ისტორიაში. `docs/project/` არის
კონტროლის სიბრტყე; `docs/standards/PROJECT-OPERATING-SYSTEM.md` — გენერიკული პროტოკოლი,
პროექტის ფაქტების გარეშე. მმართველობა მანქანურად აღსრულდება
(`ops/cli/validation/rcp-verify.py`), და უკვე არსებული დოკუმენტები კლასიფიცირდება
რეესტრში, არა გადაწერით.

## ADR-014 — ორი პროგრამა, ერთი authority plane

**სტატუსი:** ACCEPTED (2026-09-21) · სრული ტექსტი: `docs/decisions/ADR-lifecycle-program-separation.md`

Rehabilitation lifecycle და statistical contract/product პროგრამა არის **ორი ცალკეული
lifecycle ერთი კანონიკური authority plane-ის ქვეშ**. განრიგები არ ერწყმის; authority ერთია.
M3/M2 და authority chain — rehabilitation-ის; M1 instance და ქვემოთ — სტატისტიკური პროგრამის.
`CTRL-MANIFEST` ერთადერთი rehabilitation roadmap-ია, `CTRL-CURRENT` — ერთადერთი state authority;
არცერთი პროგრამა მეორის ფაზაზე gate-ს ვერ აცხადებს. ხურავს `DEF-08`/`TASK-002`. Review
პირობა: პირველივე კოლიზია `PHASE-004`-ზე.

## ADR-015 — SDMX conformance boundary

**სტატუსი:** ACCEPTED (2026-09-21) · სრული ტექსტი: `docs/decisions/ADR-sdmx-conformance-boundary.md`

SDMX მართავს **მხოლოდ სტატისტიკურ პასუხისმგებლობას**: responsibility boundary-შია მხოლოდ ის,
რისი მონაცემიც არის გამოცხადებული dimension tuple-ით დაკლავიშებული observation set.
Entity/relation/resource/geo, serving API, control plane, canonical persistence და VTL —
გარეთ. თვით conformance claim-ს **`Q47` ფლობს** (Information Model 3.1 subset, `SDMX-CSV 2.0`,
`SDMX-JSON 2.0`) და აქ არ მეორდება. ტერმინი "SDMX-compatible" claim-ად აკრძალულია.

## ADR-016 — Access არის მხარდაჭერილი provider, semantic authority-ის გარეშე

**სტატუსი:** ACCEPTED (2026-09-21) · სრული ტექსტი: `docs/decisions/ADR-access-provider-status.md`

`PERMANENT` vs `TRANSITIONAL` არასწორი კითხვაა. Access არის **first-class, პოტენციურად
ხანგრძლივი provider**, რომელსაც **არ აქვს კანონიკური სემანტიკური authority**, და canonical
architecture მის მუდმივობას არ უშვებს. Provider-ის ძლიერი მხარეები boundary-ს მიღმა
გამოიყენება; მისი ლიმიტები canonical semantics-ს ვერ ასუსტებს (`declaration ≤ provider ≤
canonical`). Access სამი პასუხისმგებლობაა — physical provider, authoring surface, round-trip
carrier — და მხოლოდ პირველია მონაცემთა ბაზაზე დამოკიდებული.
