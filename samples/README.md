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

`cids-managed-access-package-v2-template.accdb` და `cids-v2-*.accdb` არის გარდამავალი illustrative package-ები. ისინი არ წარმოადგენენ საბოლოო model-ს: საბოლოო reference იყენებს ცენტრალიზებულ semantic/visualization registry-ს, ამიტომ source Access package არ ატარებს chart data-ს და ახალი dataset ფიზიკურ business table-ს არ ქმნის. იხილეთ [Unified Canonical Data Platform](../docs/unified-canonical-platform-final.md).
