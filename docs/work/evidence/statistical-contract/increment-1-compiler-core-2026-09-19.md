# Evidence — statistical contract, increment 1 (compiler core)

Date: 2026-09-19  
Kind: reproducible test evidence. Not runtime evidence; no database, API or deployed revision is involved.  
Baseline: branch `security/hardening-2`, HEAD `1ebe3fd`, working tree dirty (unrelated uncommitted work present; this increment is uncommitted).

## Reproduce

```text
cd platform/apps/geostat/backend
./gradlew.bat :api:test --tests "org.base.api.service.platform.statistical.*"
```

## Result

| Suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `StatisticalContractCompilerTest` | 21 | 0 | 0 | 0 |
| `StatisticalIngestionRoundTripTest` | 9 | 0 | 0 | 0 |

Toolchain: Java 17 (Corretto 17.0.20), Gradle wrapper, Jackcess 4.0.7, offline dependency cache.

Note: the default `api/build/test-results/test/binary/output.bin` was locked by a Gradle test worker started earlier
the same day by another session, so results were redirected with an init script to `api/build/stat-test-results/`.
The foreign process was not terminated.

## What this proves

- One engine compiles two unrelated subject areas (time-based labour counts; non-time land use with a scale-10 decimal and a product-scoped codelist) with no branch on site, product or provider.
- The grammar is closed; references are exact; measure semantics have one authority.
- Digests are deterministic; presentation changes do not move the semantic digest.
- Provider limits are negotiated before approval: vertical split, deterministic names, generated row reference.
- Ingestion is exact, order-independent and fail-closed; zero, missing and unexplained-empty are distinct.
- An ACCDB emitted through Jackcess holds `NUMERIC(28,10)` and round-trips a 28-digit value exactly; a file with a foreign or edited contract stamp is refused.

## What this does not prove

- Opening, filling and saving the file in real Microsoft Access (Jackcess is not Access).
- Anything against SQL Server: registry adapter, migrations, canonical writer, reconciliation.
- API, authorization at the HTTP boundary, workflow state, jobs, recovery, performance, SDMX conformance.
- Equivalence between the published JSON Schema and the hand-written parser (no schema validator on the classpath yet).

## Regression

The increment is additive: one new package, one new resource, two new test classes; no existing source file was modified.
`:api:compileJava` and `:api:compileTestJava` PASS for the whole module.

Full `:api:test` run: **NOT COMPLETED** at the time of writing. It was started at 21:22 and produced no suite result in
over 15 minutes; an older test worker from another session (started 20:32) is stalled the same way, so the stall
predates this change. Until a full run completes, "no regression" is argued from additivity, not proven by a run.

## Addendum — increments 2 and 3 (same day)

| Suite | Tests | Failures |
|---|---:|---:|
| `JdbcStatisticalRegistryTest` | 6 | 0 |
| `ContractWorkflowReferenceStoreTest` | 10 | 0 |
| `ContractWorkflowJdbcStoreTest` | 11 | 0 |

Package total: 57 tests, 0 failures. JDBC suites run on H2 (`MODE=MSSQLServer`) against a portable subset of the schema.
Migrations `106` and `107` are T-SQL and were **not executed anywhere**: no SQL Server was reachable from this session.
Their triggers, CHECK constraints and the filtered unique index are therefore unproven until the fresh-replay script runs.
No database, Access sample or deployed service was changed.

## Addendum 2 — HTTP surface and canonical writer (same day)

| Suite | Tests | Failures |
|---|---:|---:|
| `StatisticalContractHttpTest` (standalone MockMvc) | 5 | 0 |
| `CanonicalObservationWriterTest` (H2) | 5 | 0 |
| `org.base.api.security.tenancy.*` re-run with the new controller present | 39 | 0 |

Package total: 67 tests, 0 failures. Still unproven: Spring context start with the new beans, anything on SQL Server,
real Microsoft Access, generation job, SDMX export. Full `:api:test` regression run: still not completed in this session.
