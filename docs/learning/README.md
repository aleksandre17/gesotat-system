# Platform Learning Path

ეს არის პროექტის შემეცნებითი დოკუმენტაციის canonical tree. თითოეული თავი აგებულია ერთი ფორმულით:

```text
კონცეფცია → არქიტექტურა → ნაბიჯები → კოდი/რექვესთი → გამოწვევა → გადაწყვეტა → ტესტი → მიღებული სარგებელი
```

## სასწავლო მიმდევრობა

1. `00-foundation/` — სისტემის საწყისი წერტილი და საერთო ენა
2. `01-security/` — identity, OIDC, PKCE, JWT/JWKS, RBAC/ABAC და tenancy
3. `02-contracts/` — contract-first და metadata-driven execution
4. `03-data-layers/` — Control/Data/Ingestion/Archive/Serving planes
5. `04-runtime/` — request lifecycle, relations, projections და pagination
6. `05-reliability/` — observability, SLO, backup, DR და release evidence
7. `06-interoperability/` — provider portability, SDMX/Parquet/ZIP და SDK

ყოველ თავში საბოლოო `CHECKLIST.md` აღნიშნავს რა არის შესწავლილი, რა არის დადასტურებული და რა არის ღია.

## დოკუმენტაციის cardinal learning rule

ეს წესი სავალდებულოა ყველა ახალი დოკუმენტისთვის, ნებისმიერი თემის შემთხვევაში:

```text
საბაზისო ცნებები
  → სწორი კითხვები და პასუხები
  → პრობლემის/მიზნის ფორმულირება
  → არქიტექტურული საზღვრები
  → ტექნოლოგიები და სტანდარტები
  → ნაბიჯ-ნაბიჯ lifecycle
  → კოდი, კონფიგურაცია და request/response
  → გამოწვევები და ალტერნატივები
  → უსაფრთხოება და failure modes
  → ტესტები და acceptance
  → evidence, ოპერირება და შემდეგი დონე
```

ახალი მასალა დასრულებულად არ ჩაითვლება, თუ მკითხველს არ შეუძლია უპასუხოს:

1. რა არის ეს მარტივი ენით?
2. რატომ გვჭირდება?
3. რომელ ფენაში/owner-ში ცხოვრობს?
4. რა შედის და რა გამოდის?
5. რა ტექნოლოგიებსა და სტანდარტებს იყენებს?
6. რა შეიძლება გაფუჭდეს და როგორ ვიქცევით?
7. როგორ დავტესტოთ და რა evidence ადასტურებს შედეგს?
8. როგორ ემატება, იცვლება, versionდება და retirement-დება?

ეს არის **progressive disclosure + teach-back + evidence-driven learning**: ჯერ საფუძველი, შემდეგ მექანიზმი, ბოლოს production-grade proof.
