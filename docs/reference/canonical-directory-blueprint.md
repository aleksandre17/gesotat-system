# Canonical Host Directory Blueprint

**Status:** normative and enforceable  
**Scope:** the complete multi-functional, contract-driven platform repository  
**Invariant:** one physical owner per source artifact; no junctions, symlinks, vendored duplicates, or implicit source roots.

## 1. Design laws

1. The repository root is a governance boundary, not an application source tree.
2. `agent-framework/` is an empty upstream placeholder. Its `kit/` directory is intentionally empty.
3. `.agents/` owns host configuration, generated projections, evidence, telemetry and guards; it never owns application source.
4. `platform/` owns deployable product code and canonical data fixtures. Backend and frontend have separate physical ownership.
5. `ops/` owns runtime orchestration, environment templates, runbooks and operational CLI code.
6. `docs/` is the knowledge and decision system; `docs/archive/` is the only dead-by-location area.
7. Every directory has one owner, one purpose, one lifecycle and one permitted dependency direction.
8. Generated output is disposable and must never become an input source. Evidence is immutable and must never be edited in place.

## 2. Normative tree

```text
<host>/
├── README.md · AGENTS.md · CLAUDE.md · CHANGELOG.md
├── package.json · pnpm-workspace.yaml · .editorconfig · .gitattributes · .gitignore
├── agent-framework/                         [UPSTREAM PLACEHOLDER — empty]
│   └── kit/                                  [empty by policy]
├── .agents/                                 [HOST CONTROL PLANE]
│   ├── kit/                                  [version-pinned attachment copy]
│   ├── project/                             [project manifest, profile, lock, roster]
│   │   ├── roles/ · doctrine/
│   ├── knowledge/                            [private/<role> + shared]
│   ├── generated/                            [roles, skills, indexes, runtimes]
│   └── runtime/                              [receipts, telemetry, ground, guards]
├── docs/                                    [KNOWLEDGE PLANE]
│   ├── intent/ · decisions/ · reference/ · guides/
│   ├── work/                                [board, cards, evidence]
│   └── archive/                             [dead by location]
├── platform/                                [PRODUCT PLANE]
│   ├── packages/ · kits/ · tools/            [shared, versioned]
│   ├── apps/
│   │   ├── REGISTRY.json                    [product-project inventory]
│   │   └── <projectId>/                     [isolated product source]
│   │       ├── backend/                     [single project Gradle boundary]
│   │       │   ├── settings.gradle · build.gradle · gradlew*
│   │       │   ├── core/ · api/ · mobile/
│   │       └── frontend/web/                [project frontend runtime]
│   ├── data/                                 [fixtures and non-secret test data]
│   └── e2e/journey/                          [end-to-end journeys]
├── ops/                                     [OPERATIONS PLANE]
│   ├── infra/ · compose/ · config/ · runbook/ · cli/
├── samples/                                 [USER-PROVIDED SAMPLE INPUTS]
└── scripts/                                 [legacy root name forbidden; use ops/scripts]
```

Root hygiene is strict: `db/` is forbidden because migrations belong to the
owning product build boundary (`platform/apps/<projectId>/backend/core/...`);
`tmp/` is forbidden because render/intermediate output is ephemeral; and
`BOOT-INF/` is forbidden because it is unpacked JVM packaging output. Such
material is recoverable only in the ignored build archive and is never a source
or deployment authority. `samples/` is the deliberate exception: it is the
canonical, immutable, user-supplied fixture boundary and is referenced by
generators and acceptance tests.

## 3. Ownership and dependency rules

| Owner | May depend on | Must not depend on |
|---|---|---|
| `agent-framework` | nothing in host | host source, runtime secrets |
| `.agents/project` | `agent-framework/kit` manifest | application implementation |
| `docs` | source paths as references | generated output as authority |
| `platform/apps/geostat/backend/core` | standard libraries, migrations | controllers, frontend, ops credentials |
| `platform/apps/geostat/backend/api` | `core`, adapters, contracts | frontend filesystem paths |
| `platform/apps/geostat/backend/mobile` | `core`, mobile adapters | API source internals |
| `platform/apps/geostat/frontend/web` | published API contracts | backend source files |
| `ops` | platform artifacts and environment references | business-domain source |
| `ops/scripts` | declared paths and CLI interfaces | hidden working-directory assumptions |

Allowed dependency direction is: `control/documentation → product → operations → runtime evidence`. Reverse imports are defects.

## 4. Naming and placement grammar

- Source directories use lowercase kebab-free names: `backend`, `frontend`, `packages`, `e2e`.
- A directory name identifies a bounded context; it never combines `src`, `build`, `data` and `docs`.
- Build output belongs beside its owning build boundary (`platform/apps/geostat/backend/build`), never at repository root.
- Secrets belong only in ignored environment/secret stores; no secret is committed to any tree.
- A new top-level directory requires an ADR, owner, lifecycle, checker rule and migration plan.
- A move is complete only when the old path is absent, the new path is physical, and all runtime references resolve.

## 5. Enforcement gates

`ops/cli/validation/host-layout-check.ps1` is the executable authority. It must fail when it finds:

- any link under `platform/`, `.agents/` or `agent-framework/`;
- a root-level `core`, `api`, `mobile`, `web`, Gradle boundary or duplicate source tree;
- root-level `db`, `tmp` or `BOOT-INF` build/source leakage;
- a non-empty upstream `agent-framework/kit`;
- a required canonical directory missing;
- an undeclared top-level source boundary;
- a source reference that points to a retired path.

The migration ledger records every physical move, checksum and validation result. Generated caches may be recreated; source and evidence artifacts may not be silently deleted.

## 6. Change protocol

For every structural change: (1) update this blueprint and ADR, (2) update the project manifest, (3) move—not copy—the artifact, (4) update build/deploy/test references, (5) run host-layout, documentation-zero-drift and technical acceptance checks, and (6) record the result in `docs/work/CONTINUE-HERE.md`.

## 7. Internal operations blueprint

The top-level `ops` boundary is intentionally subdivided by responsibility, not by programming language:

```text
ops/
├── infra/
│   └── geostat-platform/       provider stack, Keycloak, OTel, Redis, MinIO
├── compose/                    reusable service composition fragments
├── config/                     non-secret deploy configuration and schemas
├── cli/                        stable human/CI command surface only
│   ├── lifecycle/              deploy, release, rollback, start/stop entrypoints
│   ├── validation/             preflight, acceptance, contract and drift entrypoints
│   ├── data/                   migration, ingest, evidence and ledger entrypoints
│   └── portability/            provider/interoperability entrypoints
├── scripts/                    implementation executables; never public API
│   ├── shell/                  POSIX sh/bash scripts
│   ├── powershell/             PowerShell scripts
│   ├── python/                 Python tooling and generators
│   ├── java/                   JVM/Java fixture and package tools
│   └── shared/                 versioned non-secret script libraries
├── tests/                      load, soak, chaos, DR and security harnesses
└── runbook/                    human operational procedures and incident playbooks
```

`ops/cli` is the only supported command contract. It may invoke exactly one implementation in `ops/scripts`; it must not contain language-specific business logic. `ops/scripts` is not a second source tree: each executable has one physical owner and one canonical invocation path. The physical layout now follows `ops/cli/{lifecycle,validation,data,portability}` and `ops/scripts/{python,java,powershell,shell,shared}`; the `ops/cli` root is empty except for repository metadata. New files must be placed in the inner category above; an incremental physical regrouping is allowed only when all references and acceptance evidence move in the same change. This avoids duplicate wrappers and preserves reproducibility.

### Mirrored project/service topology

Every deployable project uses the same identity tuple (`projectId`, `service`,
`environment`) in each plane. The directories are parallel projections, not
copies of one another:

```text
platform/apps/<projectId>/
├── backend/{core,api,mobile}/          product source and build boundaries
└── frontend/web/                       product UI source

ops/config/projects/<projectId>/
├── shared/{templates,secrets}/         common configuration
└── services/{api,mobile,web,infra}/    service-owned configuration

ops/compose/projects/<projectId>/
├── docker-compose.dev.yml              project orchestration entrypoint
├── docker-compose.prod.yml             project orchestration entrypoint
├── services/{api,mobile,web,infra}/    service overlays/fragments (no duplicates)
└── environments/                       environment selection overlays

ops/runtime/projects/<projectId>/
├── services/{api,mobile,web,infra}/    service runtime state/evidence
└── {receipts,telemetry,ground,guards}/ shared project evidence
```

The project-level Compose files are the single orchestration authority so that
existing CI/deploy commands remain reproducible. Service directories are
strict ownership boundaries for overlays and future fragments; they must never
become competing Compose definitions. A service may read only its own config,
compose overlay and runtime namespace plus explicitly declared shared values.
This symmetry makes a new project a mechanical namespace addition and prevents
cross-service leakage without forcing a physical schema or source duplication.

This separation follows two complementary rules: static product files and variable runtime state must be distinct (the Filesystem Hierarchy Standard separates `/usr`-like static content from `/var`-like state) ([FHS](https://refspecs.linuxfoundation.org/FHS_3.0/fhs-3.0.html)); deploy-specific configuration must remain outside code, and one-off administrative processes must run from the same release environment as the application ([Twelve-Factor config](https://12factor.net/config), [admin processes](https://12factor.net/admin-processes)).

## 8. Folder-by-folder placement contract

| Path | What belongs there | Function / forbidden content |
|---|---|---|
| `.agents/kit` | pinned upstream capability schemas | attachment only; no application code |
| `.agents/project` | project/profile/lock/roster manifests | declares identity, ownership and versions |
| `.agents/project/roles` | host role overlays | role-specific policy; no secrets |
| `.agents/project/doctrine` | numbered project laws | immutable governance decisions |
| `.agents/knowledge/private` | role-private working knowledge | access-scoped notes; never canonical contracts |
| `.agents/knowledge/shared` | shared agent knowledge | reusable, reviewed knowledge |
| `.agents/generated/roles` | generated role projections | disposable generated output |
| `.agents/generated/skills` | generated skill projections | disposable generated output |
| `.agents/generated/runtimes` | generated runtime descriptors | derived manifests only |
| `.agents/runtime/receipts` | execution receipts | append-only evidence |
| `.agents/runtime/telemetry` | local telemetry exports | non-secret diagnostic data |
| `.agents/runtime/ground` | observed external facts | provenance-bound observations |
| `.agents/runtime/guards` | guard results and locks | fail-closed control state |
| `docs/intent` | vision, roadmap, registry | why and where the platform goes |
| `docs/decisions` | ADRs and declared decisions | durable architectural choices |
| `docs/reference` | contracts, blueprints, audits | normative reference material |
| `docs/guides` | learning and operator guides | explanatory procedures |
| `docs/work` | board, cards and evidence links | active delivery coordination |
| `docs/archive` | superseded material | dead by location; never runtime authority |
| `platform/apps/geostat/backend/core` | domain model, canonical migrations, shared ports | framework-independent foundation |
| `platform/apps/geostat/backend/api` | HTTP/API application and adapters | contract execution and serving |
| `platform/apps/geostat/backend/mobile` | mobile application runtime | mobile-specific delivery |
| `platform/apps/geostat/frontend/web` | web UI/BFF/templates/static assets | consumes published API contracts only |
| `platform/packages` | reusable product packages | versioned libraries, not deployment state |
| `platform/apps/REGISTRY.json` | product project inventory | one-to-one product/config/runtime mapping |
| `platform/apps/<projectId>` | isolated project source root | no cross-project source imports |
| `platform/apps` | additional deployable applications | independently deployable products |
| `platform/kits` | product integration kits | schemas and adapters for consumers |
| `platform/tools` | product-owned developer tools | tools coupled to product APIs |
| `platform/data` | non-secret fixtures and test datasets | reproducible test inputs only |
| `platform/e2e/journey` | end-to-end journeys | acceptance scenarios and fixtures |
| `ops/infra` | provider infrastructure stacks | Keycloak, Redis, OTel, MinIO, DB; no business code |
| `ops/compose` | reusable Compose fragments | composition only, no credentials |
| `ops/config` | deploy-time configuration boundary | shared schema plus project-scoped values |
| `ops/config/projects/<projectId>/shared` | common project configuration | only intentionally shared variables |
| `ops/config/projects/<projectId>/services/api` | API-specific configuration | API-only values and credentials |
| `ops/config/projects/<projectId>/services/mobile` | mobile-specific configuration | mobile-only values and credentials |
| `ops/config/projects/<projectId>/services/web` | frontend/BFF configuration | web values; no database secrets |
| `ops/config/projects/<projectId>/services/infra` | infrastructure configuration | provider bindings and infra-only secrets |
| `ops/cli/lifecycle` | operational lifecycle commands | deploy/start/stop/release/rollback |
| `ops/cli/validation` | validation and release gates | fail-closed checks and acceptance |
| `ops/cli/data` | migration/ingest/evidence commands | data-plane administration |
| `ops/cli/portability` | provider/interoperability runners | matrix and conformance execution |
| `ops/scripts/shell` | raw POSIX implementations | invoked by CLI or CI, never business API |
| `ops/scripts/powershell` | raw PowerShell implementations | Windows automation implementation |
| `ops/scripts/python` | generators and analysis tooling | deterministic tooling with pinned inputs |
| `ops/scripts/java` | JVM fixture/package tooling | executable utilities, not product source |
| `ops/scripts/shared` | versioned script libraries | common helpers only |
| `ops/tests` | load, chaos, DR, security harnesses | evidence-producing tests |
| `ops/runbook` | human operational procedures | step-by-step incident and recovery guidance |
| `ops/compose/projects/<projectId>` | project deployment compositions | project orchestration only; no product source |
| `ops/compose/projects/<projectId>/services/api` | API composition fragment | API service build/network/health wiring |
| `ops/compose/projects/<projectId>/services/mobile` | mobile composition fragment | mobile service wiring |
| `ops/compose/projects/<projectId>/services/web` | web composition fragment | frontend service wiring |
| `ops/compose/projects/<projectId>/services/infra` | infrastructure composition fragment | project-owned infrastructure wiring |
| `ops/compose/projects/<projectId>/environments` | environment composition overlays | dev/test/staging/prod selection only |
| `platform/apps/<projectId>/<service>/Dockerfile` | service build contract | image build only; no environment secrets |
| `samples` | user-provided input examples | never imported as production source |
| `documentation/complete-package` | generated documentation publication | derived artifact, not source authority |
| `db` | legacy/import boundary when explicitly referenced | must not become a second migration authority |

## 9. Environment and secret-file contract

Environment files are configuration artifacts, not application source. Their canonical ownership is:

```text
ops/config/
├── schema/                         shared key/type/sensitivity schema
└── projects/
    └── <projectId>/
        ├── shared/                 common project configuration
        │   ├── templates/ · secrets/
        └── services/
            ├── api/                API-only templates, overlays and secrets
            ├── mobile/             mobile-only templates, overlays and secrets
            ├── web/                web-only templates, overlays and secrets
            └── infra/              project infrastructure overlays and secrets

ops/runtime/projects/<projectId>/    receipts, telemetry, ground facts and guards
```

Rules:

- `templates/` may be committed and contains placeholders only; real credentials never enter Git.
- `secrets/` is ignored, permission-restricted and preferably replaced in production by a secret manager reference.
- A deploy receives one resolved environment manifest through `--env-file` or an equivalent secret-provider injection; the application never searches arbitrary parent directories.
- Names are orthogonal variables (`DB_*`, `STORAGE_*`, `OIDC_*`, `PLATFORM_*`, `OTEL_*`), with schema validation before Compose or application startup.
- Environment selection is an explicit deploy input, not a hidden filename convention. Local, CI, staging and production may share schema but must not share secret material.
- Every key has an owner, type, sensitivity classification, allowed source, default policy and rotation/deprecation rule in `ops/config/schema`.
- Resolved environment files are operator-managed outside the repository and are
  supplied through `GEOSTAT_ENV_FILE`/Compose `--env-file`; the repository keeps
  only the placeholder template under
  `ops/config/projects/geostat/shared/templates/common.env.example`. The
  `shared/secrets` directory is reserved as an empty compatibility boundary and
  any repository-local `*.env` there is rejected. Service-specific values belong
  under `services/<service>` in the external secret manifest; repository-root
  environment files remain forbidden.

## 10. Multi-project isolation contract

This host may contain multiple independent projects. They are tenants of the
repository, not modules of one coupled application. The canonical extension is:

```text
projects/                         [optional multi-project registry]
├── <project-a>/                  manifest, source boundary and project docs
└── <project-b>/                  manifest, source boundary and project docs

ops/config/projects/
├── REGISTRY.json                 canonical project inventory and isolation rules
├── <project-a>/                  schema, templates, overlays, secrets
└── <project-b>/                  schema, templates, overlays, secrets

ops/runtime/projects/
├── <project-a>/                  receipts, logs, telemetry, locks
└── <project-b>/                  receipts, logs, telemetry, locks
```

Isolation rules:

- Every project has a globally unique `projectId` and its own manifest, release
  stream, environment namespace, secret namespace, data sources and evidence.
- A project may consume only versioned artifacts from `platform/packages`,
  `platform/kits` or explicitly approved shared services. It may not import
  another project's source, migrations, database credentials, runtime state or
  private configuration.
- Shared code is dependency-inverted: stable contracts and ports live in
  versioned packages; project adapters implement those ports locally. No
  project-to-project filesystem paths are allowed.
- Compose project names, container names, networks, volumes, buckets, database
  schemas, Redis prefixes, OTel resource attributes and evidence paths must be
  namespaced by `projectId`.
- CI, release, deploy, rollback, backup and DR commands receive `projectId`
  explicitly and fail closed when it is missing or ambiguous.
- Environment files are never shared by copying. A common template may be
  reused, but every resolved value is project-scoped and separately rotated.
- Cross-project communication is allowed only through published API/event
  contracts with explicit authorization, schema versioning and observable
  ownership; direct database joins are forbidden.
- The layout checker must reject duplicate project IDs, cross-project source
  imports, unqualified service names and unscoped secret/runtime paths.

The current `gesotat-system` tree is the canonical GEOSTAT project boundary.
Adding another project requires a project manifest and isolation acceptance;
it must not be placed inside `platform/apps/geostat/backend/core`, `api` or `mobile`.
