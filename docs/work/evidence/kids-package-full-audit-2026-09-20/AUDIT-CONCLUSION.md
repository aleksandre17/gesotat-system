# KIDS_PACKAGE_full.accdb — სრული აუდიტის დასკვნა

თარიღი: 2026-09-20  
შემოწმებული არტეფაქტი: `C:\Users\Test-User\Desktop\KIDS_PACKAGE_full.accdb`  
SHA-256: `5AD1C5C61C2C73A6F9261813590CEF390DBA12479C6B8AE65616DB0971640EBB`  
აუდიტის მტკიცებულება: [`structural-audit.txt`](structural-audit.txt)

## გადაწყვეტილება

სტატუსი: **NOT READY — canonical/production acceptance უარყოფილია**.

Audit tool-მა დააფიქსირა 11 finding: 7 structural/contract findings და 4 intrinsic-quality findings. duplicate dataset/field/relation identity ან dangling reference ამ read-only შემოწმებაში არ აღმოჩნდა, რაც დადებითი შედეგია, მაგრამ ზემოთ ჩამოთვლილი blockers საკმარისია rejection-ისთვის.

ფაილს აქვს ღირებული და საკმაოდ სრული მონაცემთა ნაწილი, მაგრამ მოქმედი საერთო სტატისტიკური კონტრაქტის, Access package-ის წესებისა და fail-closed release invariant-ის მიხედვით მისი მიღება ახლა დაუშვებელია. იგი უნდა განიხილებოდეს როგორც კანდიდატი/მიგრაციის წყარო, არა როგორც Control Plane-ის authority ან უკვე დამტკიცებული შევსების package.

## რა დადასტურდა

- ფაილი იკითხება read-only Jackcess-ით; ზომა არის 4,395,008 bytes.
- package identity-ში ფიქსირდება `KIDS_PORTAL`, `KIDS_PORTAL_V1`, revision `8`, snapshot load mode და `ACCESS_CANONICAL_R8_PAGE_MANIFEST` profile.
- აღმოჩნდა 30 dataset declaration, 156 field declaration, 6 page, 6 projection და 32 physical relationship.
- სტატისტიკურ ნაწილში არის 1 structure, 7 component, 1 measure, 7 representation, 10 unit და 880 typed observation. Observation-ის key არის `TIME_PERIOD × AGE × INDICATOR`, value არის numeric `OBS_VALUE`, ხოლო სტატუსი ცალკე attribute-ად ინახება.
- provenance-ის ძირითადი ჯაჭვი არსებობს: 456 `__raw_document`, 43 carrier, 880 raw statistical source row და crosswalk/inference ველები.
- ყველა აღმოჩენილ table-ს აქვს rows და 30-დან უმეტესობას აქვს physical primary key; 32 relationship-ის არსებობა მიუთითებს, რომ ავტორმა relational enforcement-ის შექმნა სცადა.

## Blocking findings

### P0 — package-ის ორი authority table-ს identity enforcement არ აქვს

`__gs_key`-ს აქვს 25 row, მაგრამ არც index და არც primary key. `__gs_relation`-ს აქვს 32 row, მაგრამ არც index და არც primary key. ამით duplicate key declaration ან duplicate relationship code ფიზიკურად დასაშვებია. ეს პირდაპირ ეწინააღმდეგება schema hierarchy-ს, stable identity-სა და contract-first validation-ს.

საჭიროა მინიმუმ:

- `__gs_key`: composite unique identity, რომელიც ამ package contract-ში დამტკიცდება, და primary/unique semantics-ის შემოწმება;
- `__gs_relation`: stable relationship identity, unique endpoint/cardinality invariant და შესაბამისი index;
- validation, რომელიც duplicate rows-სა და unkeyed relation targets-ს უარყოფს.

### P0 — მიმდინარე გეგმისა და ფაილის სტატისტიკური სახელები არ ემთხვევა

მოქმედი გეგმა აღწერს `__stat_metric`, `__stat_kids_statistical_input` და `__rel_kids_statistical_semantic_binding`-ს. ფაილში ეს ცხრილები არ არსებობს. მათ ნაცვლად ჩანს `__raw_stat_crosswalk`, `__raw_stat_source` და `__stat_observation`.

ეს შეიძლება იყოს უკეთესი ახალი mapping, მაგრამ ამჟამად იგი არ არის დამტკიცებული contract revision/crosswalk-ით. ამიტომ არ შეიძლება ჩაითვალოს silent rename-ად ან ავტომატურ გაუმჯობესებად. საჭიროა ერთ-ერთი მკაფიო გადაწყვეტილება:

1. დაბრუნდეს მიმდინარე გეგმით მოთხოვნილი სახელები და semantics; ან
2. დამტკიცდეს ახალი versioned contract revision, სადაც ძველი → ახალი mapping, lossless lineage, consumer compatibility და deprecation წესია აღწერილი.

### P1 — artifact-ში ერთდროულად რამდენიმე სტატისტიკური მოდელი ცხოვრობს

ფაილი შეიცავს ახალ SDMX-ს მსგავს typed model-ს (`__stat_structure`, `__stat_component`, `__stat_measure`, `__stat_observation`) და legacy-oriented raw/crosswalk model-ს. ეს შეიძლება იყოს სწორი layered design, მაგრამ ownership საზღვრები და read/write authority დოკუმენტურად არ არის ამ ფაილში გამიჯნული. მომხმარებელმა არ უნდა შეძლოს raw/crosswalk/contract metadata-ის თავისუფლად შეცვლა და ამით approved semantics-ის შეცვლა.

### P1 — ყველა projection მზად არ არის

`__gs_projection`-ის შემოწმებულ rows-ში `KIDS_RESOURCE_ENTITY` და `KIDS_GLOSSARY_ENTRY_ENTITY` `DRAFT` მდგომარეობაშია, მაშინ როცა package identity revision 8-სა და page manifest-ს ატარებს. ასეთი ფაილი სრულად გამოსაქვეყნებელ site package-ად ვერ ჩაითვლება. ყველა საჭირო projection უნდა იყოს `READY`-ზე, ან შესაბამისი page უნდა იყოს explicit non-published scope-ში.

სტრუქტურულმა აუდიტმა დამატებით `DRAFT` იპოვა `KIDS_SUBCATEGORY_RELATION`-ზეც. ეს ნიშნავს, რომ draft projection-ების რაოდენობა ორით არ შემოიფარგლება.

### P1 — key ordering weakly typed არის

`__gs_key.key_order` Access-ში `TEXT` ტიპისაა. ეს უშვებს ათისა და ორის ლექსიკურ/რიცხვით განსხვავებულ დალაგებას და ამცირებს compiler-ის determinism-ს. იგი უნდა იყოს numeric integer/Long, დადებითი და unique per dataset/key role.

## შევსების მოხერხებულობა

სტრუქტურული აუდიტისას ყველა შემოწმებულ column-ზე `caption=` ცარიელია. ამავე დროს package-ის ძირითადი ცხრილების დიდი ნაწილი ტექნიკური `__*` სახელებითაა. ეს ნიშნავს, რომ plain Access table editing მომხმარებლისთვის არ არის საკმარისად გასაგები და უსაფრთხო.

ამ ფაილს არ ვაცხადებ „მოსახერხებლად შესავსებად“. საჭიროა generated authoring surface:

- ცალკე user-facing fill tables/forms მხოლოდ კონტრაქტით გამოცხადებული dimensions/measures/attributes-ით;
- Georgian/English captions, descriptions, units და validation messages;
- lookup controls versioned codelist-ებიდან;
- ტექნიკური metadata, raw lineage, crosswalk და contract snapshot read-only ჯგუფში;
- initial view „შესავსები მონაცემები“;
- Navigation Pane grouping-ის runtime evidence და repeat-generation idempotency test.

Jackcess structural audit-ით Access forms, macros, navigation categories და user permissions ვერ დადასტურდა; მათი არსებობა ამ დასკვნაში არ ივარაუდება. ეს ცალკე Access runtime acceptance-ად უნდა შემოწმდეს.

## დადებითი არქიტექტურული მიმართულება

ფაილში ჩანს სწორი მიმართულებები: immutable-looking package revision, explicit dataset/field registry, typed observations, versioned classifier references, source row keys, checksums, crosswalk inference evidence და physical foreign-key relationships. ეს საფუძველი გამოსადეგია, მაგრამ evidence-ის არსებობა semantic approval-ს არ ცვლის. განსაკუთრებით `PROVISIONAL_APPROVED` inference rows-ისთვის საჭიროა steward decision ან მკაფიო migration status.

## მიღებამდე სავალდებულო სამუშაო

1. გაასწორე `__gs_key` და `__gs_relation` identity/index/PK invariants და დაამატე duplicate/negative tests.
2. დაამტკიცე სტატისტიკური table mapping მიმდინარე საერთო გეგმასთან; rename/replacement მხოლოდ versioned contract revision-ით.
3. დააფიქსირე ერთი authority chain: Control contract → generated package metadata → fill surface → raw lineage → canonical observations. User-editable metadata არ უნდა გახდეს authority.
4. დაასრულე ან explicit non-published-ად მონიშნე ყველა `DRAFT` projection/page binding.
5. შექმენი captions/forms/lookups და დაადასტურე მათი გამოყენებადობა Access runtime-ში.
6. გაუშვი real Access generate → open → fill → validate → import → reconciliation round-trip; დაამატე tenant/security, retry/idempotency, rollback და stale-contract negative cases.
7. მხოლოდ ამის შემდეგ განაახლე `ACCESS-PACKAGE-ORGANIZATION-PLAN.md`-ის acceptance checklist და შექმენი ახალი checksum/provenance artifact. არსებული ფაილის ხელით ჩასწორება source-of-truth ცვლილებად არ ჩაითვალოს.

მიმდინარე აუდიტის tooling ახლა fail-closed მუშაობს: შვიდი finding-ის არსებობისას Gradle task აბრუნებს failure-ს. ეს მტკიცებულება არის structural audit; runtime, import, publication და production readiness ამ ფაილით არ დადასტურებულა.
