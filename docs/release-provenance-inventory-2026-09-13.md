# Release provenance inventory — 2026-09-13

ეს ფაილი არის `ops/cli/validation/release-gate.ps1`-ის პირველი inventory-ს შედეგის განმარტება.

## სტატუსი

```text
repository HEAD: 98db52a9495ec17501aa20165efe47c502706a9b
working-tree entries: 247
strict release gate: FAIL (expected while tree is dirty)
diagnostic secret scan: PASS
```

## კატეგორიები

| კატეგორია | რაოდენობა | release policy |
|---|---:|---|
| production source | 64 | commit-ში უნდა მოხვდეს code review-ით |
| migration/database | 3 | commit-ში, თანმიმდევრული migration ledger-ით |
| tests | 3 | commit-ში და CI-ში სავალდებულო |
| configuration/deployment | 10 | commit-ში მხოლოდ non-secret template/config |
| documentation | 4 | canonical docs-ში commit; generated render — artifact store-ში |
| generated/legacy artifacts | 156 | source commit-ში არა; archive/artifact repository-ში ან უსაფრთხოდ retirement |
| unknown | 0 | ყველა აღმოჩენილი entry კლასიფიცირებულია ქვემოთ |

## ადრე `unknown`-ად აღმოჩენილი entry-ების კლასიფიკაცია

| entry | საბოლოო კლასი | release policy |
|---|---|---|
| `.env.example` | configuration template | commit-ში დასაშვები; secrets მხოლოდ placeholders |
| `samples/chartjson.dat` | generated/fixture data | source-of-truth არაა; artifact/fixture policy-ით |
| `docs/archive/source-notes/d.txt` | documentation/source note | archived provenance input |
| `samples/contracts/kids-portal-v1-r7-full-contract.json` | contract fixture | versioned test fixture; production authority არაა |
| `samples/` | sample/input data | non-production fixture; credentials აკრძალულია |
| `scripts/build_final_knowledge_artifacts.py` | build tooling source | commit-ში და CI-ში გაშვებადი |
| `ops/scripts/python/build_kids_admin_panel_blueprint.py` | build tooling source | commit-ში და CI-ში გაშვებადი |

## მიღების კრიტერიუმი

Release candidate მხოლოდ მაშინ შეიძლება გამოცხადდეს, როცა:

1. `source`, `migration`, `tests`, `config` და canonical `documentation` დალაგებულია და committed არის;
2. `generated`/legacy ფაილები source tree-დან გამოყოფილია და მათი retention/owner ცნობილია;
3. `unknown = 0` (ამ inventory-ში შესრულებულია);
4. `pwsh -NoProfile -File ops/cli/validation/release-gate.ps1` PASS-ს აბრუნებს;
5. clean checkout-იდან build, test, migration dry-run და image digest reproducibly ემთხვევა release evidence-ს.

ამ inventory-ის დროს მექანიზმი დავამატე, მაგრამ თვითონ commit/retirement არ გამიკეთებია, რადგან არსებული ფაილების წაშლა ან commit მომხმარებლის explicit release გადაწყვეტილებაა.
