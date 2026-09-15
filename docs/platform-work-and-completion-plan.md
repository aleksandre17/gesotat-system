# GEOSOTAT პლატფორმის სამუშაო და დასრულების გეგმა

ეს გეგმა KIDS R8-ის დადასტურებულ baseline-ს გარდაქმნის დარჩენილი სამუშაოების შესრულებად პროგრამად.

## მიმდინარე baseline

KIDS_PORTAL_V1 revision 8 APPROVED და PUBLISHED არის. 15 dataset გავლილია ingest-იდან serving-მდე. დადასტურებულია 36 goal, 225 resource, 230 assignment, 178 glossary row, 880 statistical observation, 297 cache row და 2,224 archive pointer. Rollback, replay, quarantine, backup/restore და DR evidence არსებობს.

## სამუშაოს პრინციპები

- Contract first.
- Fail closed.
- Idempotent execution.
- Immutable evidence.
- Schema agnostic runtime.
- Evidence based completion.

## სამუშაო პაკეტები

| ID | პაკეტი | შედეგი | Acceptance | Priority |
|---|---|---|---|---|
| W01 | Documentation authority | ერთი canonical portal და drift check | ყველა source/status სწორი და ყველა link valid | P0 |
| W02 | Management UI | schema, relation, semantic, approval და publication forms | UI to contract to Access to ingest round trip | P1 |
| W03 | JWT და authorization | OIDC/OAuth2, scopes, rotation, deny by default | negative authorization suite PASS | P1 |
| W04 | Rate quota cost | complexity, fan-out, export, timeout და concurrency limits | abuse/load tests PASS | P1 |
| W05 | Metadata negotiation | strict language/profile/version selection | negotiation matrix PASS | P2 |
| W06 | SDK generation | Java, Kotlin და Dart clients | generated client contract tests PASS | P2 |
| W07 | Format providers | SDMX XML, Parquet და ZIP | round trip/checksum tests PASS | P2 |
| W08 | High volume assurance | statistical stable-key profiles და concurrent tests | no duplicate/skip და SLO PASS | P2 |
| W09 | Operations | alerts, schedules, secret rotation, capacity და restore drills | quarterly evidence | P1 |
| W10 | Legacy retirement | unprefixed bindings და stale paths retired | no active legacy consumer | P3 |

## შესრულების რიგი

1. W01 documentation authority, W03 security design და W04 quota contract.
2. W02 management UI vertical slice და W09 operations automation.
3. W05 negotiation, W06 SDK და W07 format providers.
4. W08 scale assurance და W10 legacy retirement.

## ცვლილების სავალდებულო evidence

| Gate | მტკიცებულება |
|---|---|
| Contract | revision diff და compatibility result |
| Database | migration ledger, constraints და index audit |
| Access | package schema/hash და round trip validation |
| Ingest | row counts, quarantine და replay receipt |
| Semantics | metric/unit/aggregation/classifier approvals |
| Privacy | policy decision და negative test |
| Publication | atomic snapshot, cache count და rollback receipt |
| API | schema, relation/query და RFC 9457 tests |
| Operations | health, metrics, alert, backup/restore და DR replay |

## Definition of Done

- ყველა mandatory contract object APPROVED და version-consistent არის.
- physical migration repeatable და rollback-safe არის.
- Access artifact approved contract-ს ზუსტად ემთხვევა.
- ingest, validation, quarantine და replay idempotently მუშაობს.
- quality, privacy, lineage და reconciliation gates PASS არის.
- publication snapshot immutableა, cache შეესაბამება და rollback გამოცდილია.
- API მხოლოდ გამოცხადებულ capabilities-ს ასრულებს.
- security, quota, observability, backup და DR evidence შენახულია.
- დოკუმენტაცია runtime ledger-ს ემთხვევა.
