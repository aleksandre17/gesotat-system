# Backend target boundary

Canonical target for the existing backend modules:

- `core` — shared platform/security foundation;
- `api` — API and contract execution;
- `mobile` — mobile/statistical endpoints.

The modules remain authoritative at their current paths. `core`, `api` and `mobile` are mounted into this boundary by reversible directory junctions; Gradle, migration and deployment references therefore remain valid while the host layout is adopted.
