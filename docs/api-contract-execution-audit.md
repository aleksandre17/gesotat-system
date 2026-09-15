# API Contract Execution Audit

## Scope

This audit verifies the statement that the contract query changes do not introduce caller-controlled SQL or new KIDS-specific execution branching.

## Findings

| Control | Result | Evidence |
|---|---|---|
| Caller-supplied SQL/table names | PASS | Query requests contain fields, operators and relation names only; `ContractQueryCompiler` validates identifiers against approved contract metadata and aliases. |
| SQL parameterization | PASS | Values are bound through `JdbcTemplate` parameters; dynamic fragments are produced only from validated metadata identifiers/approved dimension ids. |
| Relation execution | PASS | `ContractRelationGraphExecutor` and `ContractWhereEvaluator` operate on contract-shaped graph data; relation names are validated before execution. |
| KIDS-specific branching added by modernization | PASS | New query, cursor, negotiation, export and serializer paths are contract/page driven. KIDS defaults remain backward-compatible endpoint defaults only. |
| Default contract resolution | PASS | `ApprovedContractResolver` selects the latest approved contract from the registry; dynamic/page reads no longer require a KIDS code as their execution default. |
| Existing family adapters | ACCEPTED BOUNDARY | Canonical statistical storage and legacy materialization retain family adapters; these are explicit adapter boundaries, not hidden caller branches. |
| Alias governance | PASS | Migration 078 seeds aliases from every approved/published contract field for every data family. |

## Deliberate boundaries

The platform still contains explicit canonical adapters for ENTITY, STATISTICAL, REFERENCE, RAW, GEO and RELATION families. A statistical adapter may use the canonical `statistics.series`, `statistics.observation` and dimension tables because that physical model is the approved family contract. Replacing that storage adapter is a future physical-provider change, not a query-contract violation.

KIDS-specific names appear only in approved seed contracts, default compatibility parameters, and the legacy materialization adapter. They are not accepted as an unvalidated query-language escape hatch.

## Verification

- `./gradlew :api:test` — PASS.
- `./gradlew :api:bootJar -x test` — PASS.
- Production API container — `healthy` after the latest build.
- Control Plane migrations 076–078 — applied and checksum-recorded.

## Conclusion

The modernization statement is proven for the newly added execution paths: no user-provided SQL is accepted, and no new KIDS-specific branching was introduced. Existing KIDS defaults/adapters are explicitly documented compatibility boundaries and must be removed only through a separately approved contract/provider migration.
