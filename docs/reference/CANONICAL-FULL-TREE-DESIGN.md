# Canonical Full Tree — Detailed Design

ეს არის სრული განმარტებითი companion დოკუმენტი `CANONICAL-FULL-TREE.md`-ისთვის.
Tree ფაილი გამოიყენება სწრაფი ორიენტაციისთვის; ეს ფაილი აღწერს ownership-ს,
ფენებსა და ცვლილების წესებს.

## Planes

- `agent-framework/` — upstream L0–L3; `kit/` ყოველთვის ცარიელი placeholder-ია.
- `.agents/` — host control plane: project manifest, roles, knowledge, generated output და runtime evidence.
- `platform/` — product/data plane: იზოლირებული project source, packages, kits, tools, fixtures და E2E.
- `platform/apps/geostat/frontend/geostat-system-app/` — Control Plane/Admin UI; contract/policy management surface.
- `platform/apps/geostat/frontend/web/` — legacy product web runtime; retirement is gated on migration acceptance.

ძველი upload UI/flow არ იშლება ავტომატურად: იგი აღინიშნება legacy-ად და
მოიხსნება მხოლოდ მაშინ, როდესაც ყველა project/service-ის migration,
contract-only acceptance და production rollback evidence დადებითია.
- `ops/` — operations plane: infrastructure, Compose, configuration, commands, scripts, tests და runbooks.
- `docs/` — knowledge plane: intent, decisions, reference, guides, work და archive.
- `samples/` — მომხმარებლის მიერ მოწოდებული immutable input/fixture-ები.
- `documentation/complete-package/` — derived published documentation.
- `build/` — ignored generated output; source authority არ არის.

## Ownership

| არტეფაქტი | ერთადერთი canonical owner |
|---|---|
| product source | `platform/apps/<projectId>` |
| domain/migrations | `platform/apps/<projectId>/backend/core` |
| API runtime | `platform/apps/<projectId>/backend/api` |
| Compose orchestration | `ops/compose/projects/<projectId>` |
| service config | `ops/config/projects/<projectId>/services/<service>` |
| runtime evidence | `ops/runtime/projects/<projectId>` |
| fixtures | `samples/` |
| normative documentation | `docs/` |

## Mirroring rule

ყველა project და service ერთსა და იმავე identity tuple-ს იყენებს:
`projectId + service + environment`.

```text
platform/apps/<projectId>                 source
ops/config/projects/<projectId>           configuration
ops/compose/projects/<projectId>          deployment composition
ops/runtime/projects/<projectId>          runtime evidence/state
```

ეს არის parallel projection და არა ფაილების ასლი. თითოეულ service-ს config,
compose overlay და runtime namespace მხოლოდ საკუთარ საქაღალდეში აქვს.

## Compose rule

`docker-compose.dev.yml` და `docker-compose.prod.yml` პროექტის ერთადერთი
orchestration entrypoint-ებია. `services/{api,mobile,web,infra}` შეიცავს მხოლოდ
service-specific overlay/fragment სივრცეს და ვერ გახდება მეორე, კონკურენტი
Compose authority.

## Forbidden root boundaries

- `db/` — migrations-ის დუბლიკატი; migrations მხოლოდ core-შია.
- `tmp/` — დროებითი render/intermediate output; მხოლოდ ignored build-ში.
- `BOOT-INF/` — unpacked JVM packaging output; source tree-ში აკრძალულია.
- root `api/`, `core/`, `mobile/`, `web/` — legacy source boundaries.
- root `.env.*` — environment configuration მხოლოდ `ops/config`-ში.

## Extension protocol

ახალი project ემატება იგივე ოთხ projection-ში; ახალი service — შესაბამისი
service namespace-ში. ცვლილების შემდეგ სავალდებულოა:

1. blueprint-ის განახლება;
2. project registry/manifest-ის განახლება;
3. ფიზიკური move, არა copy;
4. ყველა reference-ის გასწორება;
5. host-layout და documentation drift შემოწმება.

ერთი ფაილი — ერთი owner, ერთი lifecycle, ერთი authority.
