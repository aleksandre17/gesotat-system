# როგორ ვისწავლოთ ეს სისტემა

ეს დოკუმენტაცია არ მოითხოვს წინასწარ ცოდნას. ყოველი ტერმინი ისწავლება სამი ფენით:

1. **მარტივი განმარტება** — რას ნიშნავს ყოველდღიურ ენაზე;
2. **პროფესიული განმარტება** — როგორ გამოიყენება პროგრამულ სისტემებში;
3. **ჩვენი გამოყენება** — სად ჩანს ეს geostat/KIDS პლატფორმაში.

## სწავლის მეთოდი

ყოველ თემაზე დასვი ექვსი კითხვა:

1. რა პრობლემას აგვარებს?
2. რომელ boundary-ში მუშაობს?
3. რა არის მისი input და output?
4. რა შეიძლება გაფუჭდეს?
5. როგორ ვამტკიცებთ, რომ სწორად მუშაობს?
6. რა არის მისი lifecycle და retirement წესი?

## ძირითადი მეთოდოლოგიები

- **Threat modeling** — საფრთხეების წინასწარი აღმოჩენა;
- **Defense in depth** — ერთი დაცვის გარღვევამ მთელი სისტემა არ უნდა დაანგრიოს;
- **Least privilege** — თითოეულ actor-ს მხოლოდ საჭირო უფლება აქვს;
- **Fail-closed** — გაურკვევლობისას უარი, არა ნებართვა;
- **Shift-left security** — უსაფრთხოების შემოწმება development-ის დასაწყისშივე;
- **Test-driven change** — ცვლილებას წინ უძღვის შემოწმებადი ქცევა;
- **Evidence-driven release** — production release-ს ახლავს გაზომილი მტკიცებულება.

## ხარისხის მოდელი

ხარისხი არ ნიშნავს მხოლოდ „კოდი გაეშვა“. ჩვენ ვითხოვთ:

```text
Correctness + Security + Reliability + Operability
+ Portability + Maintainability + Evidence
```

## არქიტექტურული პრინციპები

- separation of concerns;
- explicit boundaries;
- single source of truth;
- dependency inversion;
- immutable/versioned artifacts;
- idempotency;
- observability;
- backward compatibility;
- automation over manual procedure.
