# Legacy dynamic-pages retirement governance

ეს დოკუმენტი არის C-09 retirement-ის canonical governance record. მისი მიზანია
legacy profile/table fallback-ის უსაფრთხო გამორთვა ისე, რომ არც consumer და არც
rollback გზა დაიკარგოს.

## 1. Consumer-impact review

Scope: `GET /api/v1/dynamic/pages/{pageId}/data` და chart path, მხოლოდ იმ
გვერდებისთვის, რომლებსაც approved contract არ აქვთ.

Required evidence:

- fallback counter-ის 30-დღიანი export: `geostat.legacy.dynamic_pages.fallback`;
- consumer/page inventory და owner თითოეულ ჩანაწერზე;
- contract-engine parity replay იგივე pageId-ებზე;
- breaking response/schema comparison;
- communication record ყველა დაზარალებულ consumer-თან.

Decision: `PENDING_STEWARD_APPROVAL` until all evidence is attached.

## 2. Deprecation window

- T0: notice გამოცხადება და fallback usage-ის baseline capture;
- T+14 days: warning headers/telemetry და owner acknowledgement;
- T+30 days: ახალი consumer-ებისთვის fallback აკრძალვა;
- T+45 days: shadow mode — contract path-ის შედეგის შედარება;
- T+60 days: fallback გამორთვა მხოლოდ approval-ის შემდეგ.

Window შეიძლება შეჩერდეს მხოლოდ documented incident/rollback trigger-ით.

## 3. Rollback plan

Rollback trigger: error-rate, response incompatibility, data-loss, privacy
violation ან approved owner-ის incident declaration.

1. გააჩერე retirement flag change;
2. აღადგინე წინა immutable config revision;
3. ჩართე fallback მხოლოდ წინა allow-list consumer-ებისთვის;
4. შეინახე incident ID, config checksum და before/after metrics;
5. გაუშვი contract parity და security regression;
6. ხელახლა გახსენი deprecation window მხოლოდ steward-ის გადაწყვეტილებით.

Rollback არის configuration-only, idempotent და არ შლის source/contract data-ს.

## 4. Steward approval record

Approval-ს ხელს აწერს დამოუკიდებელი business/data steward. repository change
ვერ ჩაითვლება approved-ად ხელმოწერის, timestamp-ის, scope-ისა და evidence
hash-ის გარეშე.

```json
{
  "recordType": "LEGACY_RETIREMENT_APPROVAL",
  "status": "PENDING",
  "scope": "KIDS dynamic-pages fallback",
  "consumerImpactReview": "PENDING",
  "deprecationWindow": "T0..T+60D",
  "rollbackPlan": "docs/legacy-retirement-governance.md#3-rollback-plan",
  "steward": null,
  "approvedAt": null,
  "evidenceSha256": null
}
```

Until this record is completed by the authorized steward, retirement remains
`READY_FOR_IMPACT_REVIEW`, never `APPROVED` or `DONE`.
