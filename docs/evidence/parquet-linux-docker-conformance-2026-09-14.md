# Parquet Linux/Docker conformance evidence

- Scope: GeoStat API Parquet codec only.
- Isolation: `/home/administrator/geostat/ci/parquet-conformance` on
  `administrator@192.168.1.199`; no production service, volume, network or
  non-GeoStat project was changed.
- Runtime: Docker `gradle:8.13-jdk17`, Gradle 8.13, Linux container.
- Command: `gradle :api:test --tests *ParquetExportCodecTest --no-daemon`.
- Result: **PASS** (`BUILD SUCCESSFUL`, 10 actionable tasks; write/read
  round-trip completed).
- Corrective change: added
  `org.apache.hadoop:hadoop-mapreduce-client-core:3.3.6` to the API runtime
  dependencies after the first isolated run exposed a missing
  `FileInputFormat` class.
- Remaining: SDK codec and production-authority gates are separate scopes and
  are not implied by this conformance result.
