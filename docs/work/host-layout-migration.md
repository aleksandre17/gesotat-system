# Host layout migration

## Current state

The canonical host boundary is scaffolded. `agent-framework/kit` is intentionally empty. `.agents/kit` is the versioned attachment point, not upstream source.

`platform/apps/geostat/backend` physically contains the canonical Gradle modules `core`, `api`, and `mobile`; `platform/apps/geostat/frontend/web` physically contains the frontend module. There is one source boundary, with no junctions, symlinks, duplicate module trees, or linked folders. The backend Gradle settings explicitly register the frontend project so the dependency graph remains complete.

Operational infrastructure is physically under `ops/infra`; all repeatable operational automation is physically under `ops/cli`. The former root `infra/` and `scripts/` boundaries are retired.

The project identity is now consistent across product, configuration, runtime
and registry layers: `platform/apps/geostat`,
`ops/config/projects/geostat`, `ops/runtime/projects/geostat` and
`ops/config/projects/REGISTRY.json`. Future projects must be sibling namespaces
with their own manifests and isolation boundaries.

## Safety gate

Run `pwsh -NoProfile -File scripts/host-layout-check.ps1`. The checker verifies physical directories, the single backend Gradle boundary, empty upstream placeholders, and absence of links or legacy root module directories. No unknown or user-owned artifact is classified as “extra” by filename alone.

The physical relocation was completed only after the source targets were verified and the full technical acceptance suite passed. Generated build outputs and legacy evidence files are retained where they are not source duplicates; they are not module boundaries and are not linked directories.
