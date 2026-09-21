# Generic Access Package Composer — one-page design

Date: 2026-09-20
Status: **PROPOSED — no code is written until the owner approves this page.**
Basis: [completion plan](../contract-driven-metadata-schema-agnostic-completion-plan.md) §1, C-01, C-03, C-04, C-05;
[statistical plan](COMMON-STATISTICAL-CONTRACT-PLAN.md) §6–§8, §11.4; [package organisation](ACCESS-PACKAGE-ORGANIZATION-PLAN.md).
Language note: written in English on purpose, like the improvement register, so the text is exact.

## მოკლე შინაარსი ქართულად

**მფლობელის მოთხოვნა.** R8-ის სტატისტიკური ნაწილი რთული შესავსები იყო და განახლებას აჭიანურებდა. მიზანი — **იგივე package**, მარტივი, აგნოსტიკური და საერთაშორისო გრამატიკაზე (SDMX) დაფუძნებული სტატისტიკური ნაწილით. ცალკე სტატისტიკური ფაილი ამ მიზანს არ პასუხობს; ის გაუარესება იყო.

**მთავარი აზრი.**

1. Package — დამტკიცებული site contract revision-ის დეტერმინისტული projection. Composer არ იცის KIDS და არ იცის ცხრილის სახელი; იცის მხოლოდ დახურული family enum.
2. Authority უკვე ბაზაშია: 15 dataset, 287 field, 58 relation, 12 classifier. დღეს იგივე ფაქტები სამ ადგილას დუბლირებულია — ბაზაში, R8 სკრიპტში და AccessAuthoringAdapter-ში. დიზაინი ერთ გზას ტოვებს.
3. სტატისტიკა — composer-ის ერთ-ერთი section, არა ცალკე ფაილი. Access-ში: ერთი ფაილი, 4 ჯგუფი, entity ცხრილები R8-ის მსგავსად, და **ერთი** სტატისტიკური ცხრილი 6 სვეტით, ქართული ჩამოსაშლები სიებით.
4. ახალი მაჩვენებელი, განზომილება ან dataset — contract revision; კოდის ცვლილება — არასოდეს.

**მიღების ტესტები — მტკიცებულება, არა სიტყვა.**

- A. R8 parity: composer-ის ფაილი R8-ს ცხრილ-ცხრილ ემთხვევა — სვეტები, key-ები, 21 relationship, სტრიქონების რიცხვი.
- B. სტატისტიკური ნაწილის შეცვლა იმავე package-ში; 880/880 შეჯერება; იგივე load API.
- C. მეორე, სინთეტიკური site — კოდის 0 ცვლილებით.
- D. ნამდვილ Access-ში იხსნება, 4 ჯგუფი ჩანს.
- E. უცნობი family, გატეხილი relation, დაუმტკიცებელი revision — უარი.

რიგი: A -> D -> B -> C. კოდს მფლობელის დამტკიცებამდე არ ვწერ.

---

## 0. The owner's requirement

The statistical part of the approved R8 package was hard to fill in and slowed every update. The goal is **the same
package**, in which the statistical part becomes simple, agnostic and grounded in an international grammar (SDMX),
while every other part keeps everything it has. A separate statistics-only file is not that goal; it is a regression.

## 0.1 The four things the owner asked for (2026-09-20, stated explicitly)

| # | Requirement | How the design answers it |
|---|---|---|
| R1 | Everything categorised — what must be filled, what is not fillable, what is system — shown as separate "sheets" | Every table **and every column** carries one authoring class in the contract metadata: `FILL` (a person types it), `PICK` (chosen from a governed list), `AUTO` (derived by the system), `SYSTEM` (contract copy, read-only), `LINEAGE` (source trail). `GroupPolicy` turns the class into the four Navigation Pane groups; nothing is grouped by table name. |
| R2 | Whatever can be filled or generated automatically must be | The writer fills every `AUTO`, `SYSTEM` and `LINEAGE` cell itself: `__gs_*`, the `__stat_*` catalog, codelists, captions, drop-down lists, keys, checksums, the unit of an indicator, the raw-to-statistics links. A person is left with period, dimension picks and the value. |
| R3 | Dynamic, abstract, canonical names; dimensions, measures, facts; the very same structure usable on another site | Column names come from a **shared concept registry**, never from a site: the SDMX cross-domain concepts `TIME_PERIOD`, `REF_AREA`, `SEX`, `AGE`, `INDICATOR`, `OBS_VALUE`, `OBS_STATUS`, `UNIT_MEASURE`. The catalog tables (`__stat_structure`, `__stat_component`, `__stat_measure`, `__stat_unit`, `__stat_attribute_attachment`) are byte-for-byte the same shape on every site; only their rows differ. A second site declares its dimensions and gets its table with zero code. |
| R4 | The link between raw data and statistical data is kept | Each observation keeps its trail: `__lin_stat_source` (class `LINEAGE`, written by the system) maps the observation key to the raw carrier / document, the JSON path and the payload checksum, exactly the evidence `__raw_kids_statistical_carrier` and `__stat_kids_statistical_input` hold today. The indicator also keeps its declared relation to the entity it describes (`__stat_metric.source_resource_id` -> `__ent_kids_resource`), as a row in `__gs_relation`. |

What a person sees in the statistical table after this: four cells to fill per row — period, age, indicator, value —
and an optional status. Everything else in the file fills itself.

## 1. Principle

**A package is a deterministic projection of one approved site contract revision.** The composer knows no site name,
no table name and no KIDS. It knows only the closed family enum: ENTITY, REFERENCE, RELATION, RAW, STATISTICAL, GEO.

## 2. The authority already exists — and is duplicated three times

Dev Control Plane, measured 2026-09-20: `site_contract_dataset` holds 15 datasets, each with `data_family`,
`access_table_name` and `load_order`; `site_contract_field` 287 rows; `site_contract_relation` 58;
`site_contract_classifier` 12.

The same facts are hard-coded a second time in the R8 script
(`ops/scripts/java/KidsPortalCanonicalAccessPackageGenerator.java`) and a third time, for statistics only, in
`AccessAuthoringAdapter`. Three authorities for one truth violates C-01. This design replaces all three with one path.

## 3. Architecture

```text
approved site contract revision
  -> PackageContractSource (port)     revision -> PackagePlan (immutable, digest)
  -> FamilySectionRegistry (SPI)      family -> section; unknown family -> refusal
       ENTITY, REFERENCE, RELATION, RAW, GEO     generic typed table + rows
       STATISTICAL                               the existing statistical compiler:
                                                 dataset -> structure_ref -> typed table
                                                 + plan §6 catalog projections
  -> GroupPolicy (pure function)      (family, row_role, editable) -> 1 of 4 groups
  -> PackageWriter (adapter, ACCDB)   the only code that knows Access
```

- `PackagePlan`: datasets, fields, keys, relations, classifiers, pages, projections, revision digest. The `__gs_*`
  system tables are filled **from this same plan** — never by hand, never from a second list.
- `FamilySection`: the pattern C-04 already uses (`ContractPageExecutionRegistry`). A section never knows the site.
- STATISTICAL section: the dataset binds an approved DSD through `structure_ref` (statistical plan §6, "Dataset
  binding"). Output: one typed table plus `__stat_structure`, `__stat_component`, `__stat_measure`, `__stat_unit`,
  `__stat_attribute_attachment`, all filled from the contract.
- `GroupPolicy`: editable ENTITY / RELATION / STATISTICAL -> `GROUP_DATA`; `__gs_*` and the `__stat_*` catalog ->
  `GROUP_SCHEMA`; REFERENCE -> `GROUP_CODELISTS`; RAW -> `GROUP_LINEAGE` (the four Georgian group names already
  defined in `AccessAuthoringAdapter`). No list of table names exists anywhere.

## 4. Invariants

Deterministic (same revision -> same plan digest); fail-closed (unknown family or type, dangling relation,
unapproved revision -> refusal); zero site-specific branches; provider limits through the existing
`ProviderCapabilities`; knowledge of Access lives only in the writer.

## 5. Acceptance — evidence, not words

| # | Test | Criterion |
|---|---|---|
| A | **R8 parity** | KIDS revision -> composer -> compared with the R8 file table by table: names, columns, keys, the 21 enforced relationships, row counts |
| B | **Statistical swap** | revision N+1 binds the STATISTICAL dataset to `KIDS:KIDS_INDICATORS`; the 5 legacy statistical tables are handled per statistical plan §8; 880/880 reconciliation; same load API |
| C | **Second site** | a synthetic contract with other names and a composite key -> a package with **zero code change** (C-03) |
| D | **Real Access** | opens; the four groups are visible (AIR-2026-051 closed inside the writer) |
| E | **Negatives** | unknown family, dangling relation, unapproved revision -> refusal |

Order: A -> D -> B -> C. Every step closes only with evidence.

## 6. What goes away

The R8 script becomes legacy once parity (A) passes; `AccessAuthoringAdapter`'s own `__gs_*` writing moves into the
writer; hand-written classification SQL is replaced by a governed proposal API (AIR-2026-053); today's scratch
migration scripts leave nothing in the product.

## 7. What the owner gets in Access after B

One file. Group `GROUP_DATA`: the entity tables as in R8, plus **one** statistical table of six columns
(period, age group, indicator, unit, value, status) with drop-down lists showing Georgian labels. Adding an
indicator, a dimension or a whole new statistical dataset is a contract revision — never a code change.

## 8. How the lineage group is filled (owner question, 2026-09-20)

მოკლე პასუხი: **ადამიანი — არასოდეს. წყაროს კვალს ყოველთვის სისტემა ავსებს.** გზა ორი არის — დამოკიდებულია, მონაცემი საიდან მოდის.

**1. მონაცემი წყაროდან მოდის (იმპორტი, მიგრაცია).**
adapter წყაროს ფაილს კითხულობს და კვალს თვითონ წერს, სტრიქონ-სტრიქონ:

| ცხრილი | რა ჩაიწერა | როდის |
|---|---|---|
| `__raw_document` | ფაილი: სახელი, checksum, მიღების დრო | ფაილის მიღებისას |
| `__raw_kids_statistical_carrier` | წყაროს კონტეინერი, payload checksum | parse-ისას |
| `__lin_stat_source` | თითოეულ დაკვირვების წყაროს უჯრა: JSON path, ორიგინალური ტექსტი, encoding | თითოეულ მნიშვნელობის ამოღებისას |
| `__lin_stat_crosswalk` | ძველი კოდი → ახალი მაჩვენებელი, დასკვნის მტკიცებულება | ერთხელ, მიგრაციისას |

ამ ფაილში ზუსტად ასე მოხდა: 880 დაკვირვება — 880 კვალი, R8-იდან, ხელით — არც ერთი.

**2. ადამიანი Access-ში ახალ სტრიქონს ხელით წერს.**
წყაროს უჯრა არ არის — წყარო **თვითონ ეს ფაილი** არის. ამიტომ `__lin_stat_source`-ში ამ სტრიქონის კვალი Access-ში არ ჩნდება, და ეს სწორია. კვალი სერვერზე ჩნდება, ფაილის ჩატვირთვისას:

- სერვერი ფაილის ბაიტებს უცვლელად ინახავს, sha256-ით;
- წერს: batch → ფაილი → სტრიქონის ნომერი + სტრიქონის hash → დაკვირვება;
- release gate `RAW_LINEAGE_VALID` ამოწმებს, რომ თითოეულ დაკვირვებას კვალი აქვს. კვალი არ არის — გამოქვეყნება არ არის.

ეს ნაწილი **უკვე მუშაობს** სერვერზე (დღევანდელ ჩატვირთვებში გაიარა).

**რა ჯერ არ არის აშენებული:** საპირისპირო გზა. როცა სერვერი package-ს ხელახლა გენერირებს, წყაროს კვალის ჯგუფი უკვე შევსებული უნდა მოვიდეს — ხელით შეყვანილ სტრიქონებისაც ("ფაილი sha..., სტრიქონი N"). ეს composer-ის ნაწილი არის, პროტოტიპში არ არის.

**დაცვა:** LINEAGE კლასი Access-ში ფიზიკურად ჩაკეტილი არ არის — ჯგუფი ვიზუალური ორგანიზაცია არის, არა უსაფრთხოების საზღვარი. თუ ადამიანი კვალს ხელით შეცვლის, სერვერი მას არ ენდობა: კვალს ყოველთვის თვითონ, მიღებულ ბაიტებიდან, ხელახლა აშენებს.
