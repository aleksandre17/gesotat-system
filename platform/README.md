# Platform boundary

`platform/apps/geostat/backend` and `platform/apps/geostat/frontend` are the canonical host boundaries.

Current implementation remains in the existing Gradle modules (`core`, `api`, `mobile`) and `web` until a validated relocation is completed. This prevents build, migration, deployment and runtime-contract breakage. The authoritative mapping is `.agents/project/project.json`.
