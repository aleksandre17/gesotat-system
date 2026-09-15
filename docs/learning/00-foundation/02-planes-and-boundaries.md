# Planes და საზღვრები

```text
Control Plane  — contract, revision, policy, registry, approval
Ingestion      — validate, quarantine, materialize
Data Plane     — raw/entity/statistics/classification/geo/relation
Archive Plane  — immutable payloads, snapshots, backups
Serving Plane  — introspection, query, export, cache
Security       — identity, authorization, tenancy, secrets
Observability  — metrics, traces, logs, SLO, evidence
Delivery       — build, scan, release, deploy, rollback
```

ერთი არტეფაქტი უნდა ეკუთვნოდეს ერთ owner-ს, ერთ ფიზიკურ ადგილს და ერთ authority-ს.
