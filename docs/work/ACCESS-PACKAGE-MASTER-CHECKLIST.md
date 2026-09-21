# Access package — მთავარი checklist

თარიღი: 2026-09-20
სტატუსი: **OPEN / NOT READY**. პუნქტი "[x]" მხოლოდ მტკიცებულების ბმულით იხურება (AGENTS.md).
card: `docs/work/cards/access-package-composer/governance.json`
დოკუმენტები: [დიზაინი](ACCESS-PACKAGE-COMPOSER-DESIGN.md) · [სტანდარტი](ACCESS-PACKAGE-CANONICAL-STANDARD.md) · [აუდიტი](evidence/kids-package-full-audit-2026-09-20/AUDIT-CONCLUSION.md) · [პასუხი](evidence/kids-package-full-audit-2026-09-20/AUDIT-RESPONSE.md)

## სად ვიწყებ და სად ვასრულებ

**ვიწყებ:** წესრიგით — ერთი card, ერთი checklist, დაგროვილ ფაილების ინვენტარი, ძველი და მცდარი არტეფაქტების გამიჯვნა.
**ვასრულებ:** როცა სტანდარტის B7-ის 8-ვე ტესტი PASS არის, Codex-ის აუდიტი 0 finding-ს აჩვენებს, `engineering-governance.py` PASS არის, და PR master-ში გახსნილი არის. ამდე — NOT READY.

## ფინიშის დეფინიცია (მფლობელი, 2026-09-20)

**პირველი — Access ფაილი იდეალურამდე**, ყველაფრის გათვალისწინებით. **შემდეგ — უმაღლესი layer:** ახალი contract-ის განახლვაა, და **სრული გზა end-to-end**: შემოტანა → ბაზაში ჩაწერა → API-ზე გამოტანა. ამ გზის გარეშე სამუშაო დასრულებული არ არის.

ახალი სესია: პირველ წაიკითხე [handoff](ACCESS-PACKAGE-HANDOFF.md).

## ფაზა 0 — წესრიგი (დღეს)

- [x] governance card ამ სამუშაოსთვის; მოთხოვნების mapping; `engineering-governance.py` PASS. — `docs/work/cards/access-package-composer/governance.json`, validator PASS.
- [x] ინვენტარი: რა დოკუმენტი მოქმედი არის, რა — ჩანაცვლებული. `kids-legacy-statistical-migration-2026-09-20.json` ჩანაცვლებული არის (43 measure — მცდარი მოდელი); Increment 7 checklist-ში — იგივე. — ჩანაცვლებული მტკიცებულება და Increment 7 მონიშნული არის.
- [ ] Desktop: მოქმედი — `KIDS_PACKAGE_candidate_3.accdb` + ფოტო. ძველი საჩდელი ფაილები `_old_access_trials`-ში გადავიდა, წაშლვის გარეშე. `KIDS_PACKAGE_full` და `candidate_2` გახსნილი არის სხვა პროგრამაში — დახურვის შემდეგ გადავიდა.
- [x] scratch-ის ინსტრუმენტები → `ops/scripts/java/prototype/`, README-ით: რა არის პროტოტიპი და რა არ არის source of truth. — `ops/scripts/java/prototype/README.md`.

## ფაზა 1 — მფლობელის გადაწყვეტილებები (ჩემ გარეშე არ იხურება)

- [x] **D-1. data macro-ები.** AGENTS.md კრჟალავს "executable business logic" Access არტეფაქტში. კვალის ავტო-ჩაწერა macro-თი არის. ვარიანტი: (ა) ADR — "integrity automation" ნებადართული არის, სერვერი მას არ ენდობა; (ბ) macro-ები წავიდეს, კვალი მხოლოდ სერვერზე. თქვენი მოთხოვნა (ა)-ს ემთხვევა; ADR საჭიროა. — **გადაწყვეტილება: (ა)**, [ADR-012](../decisions/ADR-access-package-integrity-automation.md).
- [x] **D-2. სვეტის სათაურები:** სტანდარტული კოდები (`OBS_VALUE`) თუ ლოკალიზებულ caption. ახლა — კოდები (თქვენი გადაწყვეტილება); Codex — caption. სტანდარტში A-4: პრეზენტაციის არჩევანი contract-ში. — **გადაწყვეტილება: სტანდარტული კოდები**, [ADR-012](../decisions/ADR-access-package-integrity-automation.md) §5.
- [x] **D-3. რიცხვის envelope:** წყაროში 221 მნიშვნელობა 11–16 ათწილადიანია. (28,10) მათ მრგვლებს. (28,16) თუ მრგვლების წესის დეკლარაცია? — **გადაწყვეტილება: scale 16, არც ერთი ციფრი არ დაკარგდეს**, [ADR-012](../decisions/ADR-access-package-integrity-automation.md) §5.
- [ ] **D-4. steward:** 3 DRAFT projection; 43 PROVISIONAL_APPROVED inference; `AGE_GROUP` 12 კოდის სახელები (0/12).

## ფაზა 2 — კანდიდატ ფაილის ხარვეზები (პროტოტიპში, იაფი)

- [x] I-5: `__gs_*` 9 ცხრილის ველების დეკლარაცია — ფაილი თავის თავს სრულად აღწეროს. — 0/0/0/0, [evidence](evidence/access-package-candidate-3-2026-09-20.json).
- [x] `__gs_relation` → `__gs_field` FK (ველის დონეზე). — [evidence](evidence/access-package-candidate-3-2026-09-20.json).
- [x] `__gs_page.dataset_code`: ცარიელი სტრიქონი → NULL, FK `__gs_dataset`-ზე. — [evidence](evidence/access-package-candidate-3-2026-09-20.json).
- [x] B7-8: იმავე წყაროდან ორი გენერაცია → იგივე content digest. — დიგესტ ტოლია, [evidence](evidence/access-package-candidate-3-2026-09-20.json).
- [x] Codex-ის structural audit ხელახლა, candidate_3-ზე. — 11 → 7; დარჩენილი 7 — contract revision (4) და steward (3), [evidence](evidence/access-package-candidate-3-2026-09-20.json).

## ფაზა 2ბ — Access ფაილი იდეალურამდე (მფლობელის პირველი პრიორიტეტი; მე გადავხვიე, ვუბრუნდები)

- [ ] **ფორმები:** თითოეულ შესავსებ ცხრილზე გენერირებული ფორმა, კოდის გარეშე (ADR-012: VBA აკრჟალული არის).
- [x] **სისტემური ცხრილების და წყაროს კვალის დაცვა:** 22 SYSTEM + 4 LINEAGE ცხრილი ხელით შეცვლას უარყოფს, ქართული შეტყობინებით; კვალის ჩაწერილი არ იშლება (P-3); სტრიქონის macro-ები დაცვის პირობებში ისევ მუშაობენ. ნამდვილ Access: დაცვა 10/10, კვალი 7/7 — `protect_system_tables.ps1`, `protection_test.ps1`; [ADR-012](../decisions/ADR-access-package-integrity-automation.md) განახლებული.
- [ ] **სვეტების აღწერა ქართულად:** "რა შეიყვანო" — სტატუსის ზოლში; სვეტის სახელი სტანდარტული რჩება.
- [ ] ნამდვილ Access-ში: ფორმა გახსნა, შევსება, სისტემური ცხრილში შეცვლის მცდელობა → უარი; ფოტო.

## ფაზა 3 — dev სერვერის ჰიგიენა (დღევანდელი მცდარი ჩაწერები)

- [x] 43-measure contract-ები (bfe98140, 6d3e6ba2) და d07a0d45 — SUPERSEDED/უარყოფილი; snapshot 58, 59 — არ გამოქვეყნდეს, უარყოფა მტკიცებულებით. — 87/87 reference SUPERSEDED, API-ით, [evidence](evidence/access-package-dev-hygiene-2026-09-20.json). **ღია:** 6d3e6ba2 ისევ APPROVED — "withdraw" ტრანზიცია არ ეგზისტირებს; snapshot 58/59 — წესი, არა მდგომარება.
- [ ] კლასიფიკატორის ვერსია 10 (placeholder კოდები) — RETIRED. ვერსიები 10/11 ხელით SQL-ით ჩაწერი — AGENTS.md-ის დარღვევა; გამოსწვარება ფაზა 4-ში.
- [ ] AIR-2026-050..053 — აქტუალიზაცია.

## ფაზა 4 — contract-ის ლეგიტიმაცია

- [x] **4.1 რიცხვის envelope** (ADR-012 D-3): scale 16-მდე; მიგრაცია 111 + 112; chain replay 107/107 ნამდვილ SQL Server-ზე; dev-ზე 10 408 მნიშვნელობა — ჯამი და checksum უცვლელი; 102/102 ტესტი — [evidence](evidence/statistical-numeric-envelope-2026-09-20.json). **ინციდენტი:** dev 13 წუთი გაჩერებული იყო ჩემი ბრალი — მტკიცებულებაში ჩაწერილი.
- [x] **4.2 end-to-end, შემოტანა → ბაზა, ზუსტად:** contract `KIDS:KIDS_INDICATORS` 3.0.0 (OBS_VALUE (28,16)), ოთხი თვალი; authoring file → R8-ის lexical ტექსტიდან შევსებული → load → snapshot 61. **880/880 ზუსტი ბაზაში, მათ შორის 221-ვე გრჟელი მნიშვნელობა**; release gates — releasable. [evidence](evidence/kids-indicators-exact-end-to-end-2026-09-20.json).
- [x] **4.3 API-ზე გამოტანა, ზუსტად:** read endpoint dev-ზე (კომპილაცია კონტეინერში რესტარტის წინ); API — 880 სტრიქონი snapshot 61-დან; **880/880 ზუსტი API-ის საზღვარზე, 221-ვე გრჟელი მნიშვნელობაც** — [evidence](evidence/kids-indicators-exact-end-to-end-2026-09-20.json). **ზუსტი გზა შემოტანა → ბაზა → API დამტკიცებული არის მხოლოდ სერვერის სტატისტიკურ ფაილზე. მფლობელის მოთხოვნილი გაერთიანებული package (candidate_3) ამ გზაზე ჯერ არ გაიარა — ფინიში დახურებული არ არის.**
- [ ] **4.3-ინციდენტი (AIR-2026-054, P0):** publish ერთ snapshot-ს არ აქვეყნებს — პროდუქტის **ყველა** REVIEW_REQUIRED snapshot-ს. publication 15-მა 14-ი 90 წამით ჩაანაცვლა; rollback-ით დაბრუნებული. ნაშთი: 53, 57–61 PUBLISHED სტატუსში; 53 — **ჩემი არ იყო**.

- [ ] კლასიფიკატორის propose/approve API, ოთხი თვალის წესით (AIR-053); ვერსია 11 მისი გავლით ხელახლა.
- [ ] DIMENSION_GROUP ატრიბუტი, რომელი კოდის მიხედ იცვლება (AIR-052) — grammar + compatibility წესი.
- [ ] KIDS site contract revision N+1: STATISTICAL dataset → structure_ref; ძველი → ახალი mapping (184 სვეტი); compatibility report; deprecation წესი; ოთხი თვალი.

## ფაზა 5 — composer პროდუქტში (სამი გენერატორის ნაცვლად ერთი)

- [x] **5.1 PackageContractSource → PackagePlan (digest) + AuthoringPolicy/Group:** ერთი პლანი, რომელიდან გენერატორაც და loader-აც მოდის. 6/6 ტესტი, საიტი — არა KIDS (მიწის რეესტრი, composite key). ნამდვილი KIDS r8 dev-ზე: 15 dataset, 21 relation, key-ს გარეშე 0, dangling 0 — [evidence](evidence/access-package-composer-plan-2026-09-20.json). **აღმოჩენა:** Control Plane 104 ველს აცხადებს, R8 ფაილი — 122.
- [ ] **5.2** FamilySectionRegistry (სტატისტიკური section — structure_ref-ით) + PackageWriter.
- [ ] **5.3** loader იმავე plan-იდან კითხულობს — ფაილი და სერვერი ერთმანეთს შეხვდებიან.
- [ ] provider ფაქტები F-1…F-7 writer-ში, თითოეულ თავის ტესტით.
- [ ] **რისკი G-1:** სერვერი Linux არის; query-ები და macro-ები მხოლოდ ნამდვილ Access წერს. ამიტომ: სერვერი ბაზის ფაილს გენერირებს, Windows-ის "finishing + acceptance" worker ავსებს და B7-5 ტესტი გაშვებს. ეს არქიტექტურული გადაწყვეტილება ADR-ში.
- [ ] R8 სკრიპტი და AccessAuthoringAdapter-ის `__gs_*` — legacy, parity-ის შემდეგ.

## ფაზა 6 — მიღება (B7, 8 ტესტი)

- [ ] 1 structural audit 0 (ახლა 7) · [x] 2 self-description · [ ] 3 parity/mapping revision-ით · [x] 4 lexical 880/880 · [x] 5 live Access 7/7, 4 ასლი · [ ] 6 სერვერის round-trip + ნეგატივები · [ ] 7 მეორე საიტი, კოდის 0 შეცვლა · [x] 8 repeat generation.

## ფაზა 7 — release

- [ ] `ACCESS-PACKAGE-ORGANIZATION-PLAN.md` acceptance; ახალი checksum/provenance; STATISTICAL-CONTRACT-IMPLEMENTATION-CHECKLIST შეჯერება.
- [ ] commit + PR — მხოლოდ თქვენი ნებართვით.
