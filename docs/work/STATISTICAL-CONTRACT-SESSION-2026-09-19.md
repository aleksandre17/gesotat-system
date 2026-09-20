/mo# სტატისტიკური კონტრაქტი — სესიის ანგარიში (2026-09-19)

სტატუსი: dev-ზე მუშაობს. Release — NOT READY.  
Branch: `security/hardening-2`. Commit-ები: `8f1887c`, `9cf6bd5`, `349c90a`.  
Checklist: [იმპლემენტაციის checklist](STATISTICAL-CONTRACT-IMPLEMENTATION-CHECKLIST.md).  
Evidence: [runtime evidence](../evidence/statistical-contract-runtime-2026-09-19.json).

## 1. რა გაკეთდა

| # | ნაწილი | შედეგი |
|---|---|---|
| 1 | გადაწყვეტილებები Q01–Q50 | 50 / 50. ფაილი: `STATISTICAL-CONTRACT-OPEN-QUESTIONS.md` |
| 2 | კონტრაქტის compiler | JSON draft, ზუსტი ვერსიის ბმულები, plan, digest |
| 3 | ბაზა | migration `106`, `107`, `108`. SQL Server-ზე: 103 / 103, behaviour 26 / 26 |
| 4 | registry | წაკითხვა და ჩაწერა: propose, approve, supersede |
| 5 | workflow | DRAFT, REVIEW_REQUIRED, APPROVED. ავტორი და დამტკიცებელი — ორი პირი |
| 6 | HTTP API | `ETag`, `If-Match`, `Idempotency-Key`, RFC 9457 |
| 7 | Access ფაილი | ზუსტი `NUMERIC`, ქართული სათაურები, ჩამოსაშლი სიები |
| 8 | მონაცემის მიღება | ფაილი, ვალიდაცია, snapshot, lineage, MinIO |
| 9 | release gate-ები | snapshot 55 — `releasable=true`. გამოქვეყნება არ გაკეთდა |
| 10 | SDMX-CSV 2.0 | export plan-იდან. 3 ტესტი |
| 11 | performance | 200 000 სტრიქონი — 3.8 წამი, 21 MB, heap 515 MB |
| 12 | legacy crosswalk | 86 metric. 24 — მზა. 62 — steward-ის პასუხი სჭირდება |
| 13 | ტესტები | პაკეტი: 89 PASS. სრული `:api:test`: 457 — 454 PASS, 1 skipped, 2 FAILED |

2 FAILED ტესტი ამ სამუშაოს არ ეკუთვნის. ისინი სხვა სესიის დაუმთავრებელ legacy სამუშაოს ეკუთვნის.

## 2. dev server-ზე

- `geostat-api-dev` — ამ commit-ების კოდი.
- prod `:8083/health` — `200`. prod უცვლელია.
- Keycloak: ახალი client `geostat-contract-approver`. ძველი client-ები უცვლელია.
- ბაზაში დარჩა: proof კონტრაქტები, namespace `STAT`-ის ბმულები, dataset `RUNTIME_PROOF`, snapshot 54 და 55. გამოქვეყნებული — 0.

## 3. live-ზე ნაპოვნი და გამოსწორებული defect-ები

1. Object key prefix — ბოლო `/` სიმბოლოს გარეშე. პასუხი `400`. გამოსწორდა.
2. Measure-ის ახალი ვერსია — metric code-ის გაორმაგება. გამოსწორდა: metric = dataset + measure-ის ზუსტი ვერსია.
3. Snapshot-ის state და staged rows — release gate-ებს არ ემთხვეოდა. გამოსწორდა.
4. Gate `STATISTICAL_SEMANTICS_VALID` — ცარიელი მნიშვნელობა status-ით defect-ად ითვლებოდა. გამოსწორდა.

5. თარიღი `java.sql.Date`-ით — ძველი წლები ერთი დღით იწევდა. გამოსწორდა: `LocalDate`.
6. Batch write — გაკეთდა. dev-ზე 2 712 observation, 9 წამი.

7. Access ფაილი არ იხსნებოდა: ფორმატი 2016. Access 2016-ის ძველი build-ი მას არ ხსნის. გამოსწორდა: ფორმატი 2010. Access-ში შემოწმდა: იხსნება, ინახება, dev-ზე აიტვირთა.

8. Access-ის მარცხენა პანელში ჯგუფები: "შესავსები მონაცემები", "ცნობარები", "კონტრაქტი". გენერატორი თავად წერს. შემოწმდა Access-ში.

## 4. რა დარჩა

| # | საკითხი | ვინ |
|---|---|---|
| 1 | Access-ის სხვა ვერსიები: 32-bit, Microsoft 365 | ადამიანი |
| 8 | ფაილის გახსნისას ცხრილის ავტომატური გახსნა და ფორმები | ინჟინერი |
| 2 | 62 legacy metric: unit, denominator, multiplier | steward |
| 3 | SDMX: ოფიციალური validator, SDMX-JSON | ინჟინერი |
| 5 | 9 default — რეესტრის §6 | მფლობელი |
| 6 | DSD-ის რეგისტრაცია API-ით | ინჟინერი |
| 7 | `master`-ში merge (pull request) | მფლობელი |

## 5. Push

Push გაკეთდა 2026-09-19. მფლობელის ცხადი ნებართვა: საჯარო repo-ში push.

- Branch: `security/hardening-2`. Remote: `github.com/aleksandre17/gesotat-system`.
- გზა: HTTPS. SSH port 22 ამ ქსელიდან დახურულია.
- Force — არ. Remote `master` ამ branch-ის წინაპარია.
- submodule `stack-kit`: commit `1b2c77b` remote-ზე უკვე იყო.
- ავტორი: Aleksandre Sisvadze.
- `master` — push არ გაკეთდა. გზა: pull request `security/hardening-2` → `master`.

გაფრთხილება: repo საჯაროა. `samples/*.accdb`, `samples/chartjson.dat` და docs-ის შიდა სახელები საჯარო გახდა.
რეკომენდაცია: repo-ს private-ად გადაყვანა, ან ამ ფაილების history-იდან ამოღება.
