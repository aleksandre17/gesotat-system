# Managed Access Package v1

ეს არის GeoStat-ის self-describing `.mdb`/`.accdb` package-ის კონტრაქტი. იგი გამოიყენება მხოლოდ ახალი dynamic import flow-სთვის. ძველი Access ფაილები უცვლელად მუშაობენ legacy endpoint-ებით.

## ფაილის შემადგენლობა

სავალდებულო ტექნიკური table-ები:

```text
__gs_package
__gs_dataset
__gs_chart
__gs_chart_filter
```

მათ გარდა ფაილი შეიცავს ერთ ან რამდენიმე რეალურ data table-ს.

## `__gs_package`

ზუსტად ერთი row.

| Column | Access type | Required | მაგალითი |
|---|---|---:|---|
| `package_code` | Short Text | ✓ | `international-ratings-2026` |
| `package_version` | Short Text | ✓ | `2026.1` |

## `__gs_dataset`

ერთი row თითო data table-ზე.

| Column | Access type | Required | მაგალითი |
|---|---|---:|---|
| `dataset_code` | Short Text | ✓ | `MAIN_ECONOMIC_INDICATOR` |
| `access_table_name` | Short Text | ✓ | `main_economic_indicator` |
| `profile_code` | Short Text | ✓ | `main-economic-indicator` |
| `data_kind` | Short Text | ✓ | `STATISTICAL` |
| `row_key` | Long Text |  | `country_id,year` |
| `required` | Yes/No |  | `True` |

`data_kind`: `TABLE`, `STATISTICAL`, `BOTH`, `LOOKUP`, `AUXILIARY`.

## `__gs_chart`

ერთი row თითო chart-ზე. Chart table-ს არ მოაქვს data; ის მიუთითებს იმავე package-ის dataset-ზე.

| Column | Access type | Required | მაგალითი |
|---|---|---:|---|
| `chart_code` | Short Text | ✓ | `gdp-by-year` |
| `dataset_code` | Short Text | ✓ | `MAIN_ECONOMIC_INDICATOR` |
| `chart_type` | Short Text | ✓ | `LINE` |
| `x_field` | Short Text | ✓ | `year` |
| `y_field` | Short Text | ✓ | `gdp` |
| `series_field` | Short Text |  | `country_id` |
| `aggregation` | Short Text |  | `SUM` |
| `publication_mode` | Short Text |  | `DRAFT` |

v1 chart types: `LINE`, `BAR`, `STACKED_BAR`, `AREA`, `PIE`, `DONUT`, `TABLE`.

## `__gs_chart_filter`

ნულიდან ბევრი row თითო chart-ზე.

| Column | Access type | Required | მაგალითი |
|---|---|---:|---|
| `chart_code` | Short Text | ✓ | `gdp-by-year` |
| `field` | Short Text | ✓ | `country_id` |
| `operator` | Short Text | ✓ | `EQ` |
| `value` | Long Text |  | `1` |

v1 operators: `EQ`, `NE`, `IS_NULL`, `NOT_NULL`. SQL ტექსტი არასოდეს იწერება Access package-ში.

## `main-economic-indicator` pilot

```text
__gs_package
  international-ratings-2026 | 2026.1

__gs_dataset
  MAIN_ECONOMIC_INDICATOR | main_economic_indicator | main-economic-indicator
  | STATISTICAL | country_id,year | True

__gs_chart
  gdp-by-year | MAIN_ECONOMIC_INDICATOR | LINE | year | gdp | country_id | SUM | DRAFT
```

ამ pilot-ის `main_economic_indicator` data table უნდა შეიცავდეს target table-ის ზუსტად ამ column-ებს (იგივე სახელებითა და რიგით):

```text
id, country_id, year, gdp, gdp_per_capita, inflation, population, group_id, gdp_per_capita_ppp
```

სხვა dataset-ისთვის source column-ების სახელები და რიგი მის დამტკიცებულ target table-ს უნდა დაემთხვეს. v1 importer არ ქმნის და არ ცვლის child DB schema-ს.

## ოპერაციული პროცესი

1. ატვირთე ფაილი `/imports/access/preview`-ზე.
2. გაასწორე ყველა `UNMAPPED` ან validation error.
3. გაუშვი `/imports/access/execute` მხოლოდ წარმატებული preview-ის შემდეგ.
4. შეამოწმე `GET /imports/access/{jobId}`.
5. Data steward აქვეყნებს chart definition-ს `PUBLISHED` სტატუსით.
6. გამოიყენე `GET /dynamic/pages/61/data` და `GET /dynamic/pages/61/charts/gdp-by-year`.

## მნიშვნელოვანი შეზღუდვები

- package ვერ ირჩევს საკუთარ database/server/schema-ს;
- ყველა `profile_code` და `access_table_name` წინასწარ უნდა იყოს დამტკიცებული core registry-ში;
- child DB schema არ იცვლება Access package-ის საფუძველზე;
- production import-მდე აუცილებელია preview-ის წარმატებული შედეგი.
