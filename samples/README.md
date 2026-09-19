# Managed Access Package sample

`managed-access-package-pilot.accdb` არის GeoStat Managed Access Package v1-ის პატარა, სინთეზური ნიმუში.

- აქვს აუცილებელი `__gs_*` metadata table-ები;
- შეიცავს `main_economic_indicator` data table-ს, სამი სატესტო row-ით;
- შეიცავს `gdp-by-year` chart definition-ს, რომელიც იმპორტის შემდეგ შეინახება `DRAFT`-ად.

ჯერ გამოიყენეთ მხოლოდ `POST /api/v1/imports/access/preview`. ფაილი არ გაუშვათ production `execute` endpoint-ზე, რადგან იგი რეალურ target table-ში მონაცემის ჩაწერას ცდილობს. სრული კონტრაქტი აღწერილია [Managed Access Package v1](../docs/managed-access-package-v1.md)-ში.

იმავე fixture-ის თავიდან შესაქმნელად გამოიყენება [გენერატორი](../scripts/AccessPackageFixtureGenerator.java).

## Semantic Access Package v3

`semantic-access-package-v3-template.accdb` არის ახალი პლატფორმის მკაცრი სემანტიკური კონტრაქტის საცნობარო ფაილი. ის აჩვენებს ერთ ფაილში არასტატისტიკურ entity-ებს, მათი კავშირებსა და სტატისტიკურ observation-ს. მისი ტექნიკური `__gs_*` ცხრილები აღწერს ყველა წყაროს სვეტს, გასაღებს, კარდინალობასა და declarative projection-ს; თავად ფაილი არ შეიცავს SQL target-ს, credential-ს ან publication ბრძანებას.

მისი ხელახლა შესაქმნელად გამოიყენება [SemanticAccessPackageV3Generator.java](../scripts/SemanticAccessPackageV3Generator.java). ფაილი არის DRAFT reference და არა Kids-ის დამტკიცებული mapping.

`cids-children-portal-full-data.accdb` არის ბავშვთა და მოზარდების პორტალის სრული archive, შექმნილი `chartjson.dat`-იდან. იგი არ არის Managed Import Package: CIDS-ისთვის core registry-ში ჯერ არ არსებობს target profile/mapping. ფაილშია `cids_chart_catalog`, `cids_chart_values` და `cids_chart_payload` — 98 chart dataset-ის source data სრულად.

`kids-children-portal-full-data.accdb` არის პირდაპირი, schema-preserving export რეალური `kids` SQL Server database-დან: `files`, `glossary`, `goals`, `goals_titles`. ეს არის ბავშვების პორტალის სასურველი source-of-truth Access ფაილი.

`kids-portal-v1-canonical-r7.accdb` არის მიმდინარე production canonical package (contract revision 7, package 6.0.0): 21-ვე ცხრილს აქვს physical primary key, 21 კავშირი Access-ში referential integrity-ითაა enforced, 43 carrier-ს აქვს metric/unit/aggregation/quality/confidentiality binding და 880 lossless input cell source-იდანაა გაშლილი. carrier-ში raw JSON არ დუბლირდება. სრული acceptance record იხილეთ [kids-production-access-acceptance.md](../docs/kids-production-access-acceptance.md)-ში.

`kids-portal-v1-canonical-r8-final.accdb` არის KIDS Portal contract `KIDS_PORTAL_V1` revision 8-ის canonical source package, რომელიც გამოიყენეს dev snapshot 52-ის ingestion-ში. SHA-256: `746487cedee93c73b99492038321c138a1bcd7f2e1d9d49fe165332be6cb8211`. მასთან ერთად `kids-r8-resource-package.zip` არის სრული contract-bound package (`KIDS_RESOURCE`): Access ფაილი და ზუსტად ის 450 ფაილი, რომელსაც `__ent_kids_resource`-ის 225 row მიუთითებს (451 entry). ის იქმნება მხოლოდ approved contract descriptor-იდან, ცხრილის, key-ის ან path-ის hardcode-ის გარეშე: `../ops/scripts/shell/artifact-package-assemble.sh KIDS_PORTAL_V1 8 KIDS_RESOURCE kids-portal-v1-canonical-r8-final.accdb ../platform/apps/geostat/frontend/kids/public/files kids-r8-resource-package.zip ../docs/evidence/kids-r8-resource-package-assembly-2026-09-18.json`. row→file რუკა და preview იწერება assembly evidence-ში; build deterministic-ია. ამ ნიმუშის მისაღები გარემო dev/test-ია; production-ში პირდაპირი ingest არ გაუშვათ.

`cids-managed-access-package-v2-template.accdb` და `cids-v2-*.accdb` არის გარდამავალი illustrative package-ები. ისინი არ წარმოადგენენ საბოლოო model-ს: საბოლოო reference იყენებს ცენტრალიზებულ semantic/visualization registry-ს, ამიტომ source Access package არ ატარებს chart data-ს და ახალი dataset ფიზიკურ business table-ს არ ქმნის. იხილეთ [Unified Canonical Data Platform](../docs/unified-canonical-platform-final.md).

`kids-portal-v1-canonical-r8-build-8.0.1.accdb` is a new build of the same rows (`__gs_package.package_version` 8.0.1, produced by `:api:stampAccessPackageVersion`), and `kids-r8-resource-package-8.0.1.zip` is the complete package built from it: the Access file, the 450 files its 225 rows reference and a shipped `manifest.json` (452 ZIP entries). It was admitted on dev as manifest 7 and run end to end to snapshot 53 (`REVIEW_REQUIRED`); see `../docs/evidence/kids-r8-package-end-to-end-runtime-2026-09-19.json`. Dev/test only.
