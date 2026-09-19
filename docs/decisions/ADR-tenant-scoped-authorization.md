# ADR-010 — Tenant-scoped authorization (ABAC)

**სტატუსი:** ACCEPTED (2026-09-19) · **Owner:** პროექტის მფლობელი
**Authority:** `docs/platform-capability-and-architecture-audit-2026-09-13.md` (P0.2, P1.5),
`docs/contract-driven-metadata-schema-agnostic-completion-plan.md` (C-11),
`docs/work/STORAGE-ARTIFACT-CLOSURE-CHECKLIST.md` 14.2
**Standards:** OWASP API Security Top 10 — API1 (Broken Object Level Authorization), API5 (Broken
Function Level Authorization); NIST SP 800-162 (ABAC); deny by default.
**Layer:** Security (cross-cutting), with one additive Control-Plane model change.

---

## 1. პრობლემა (bounded scope)

API ამოწმებს RS256 token-ს (issuer, audience, expiry, JWKS) და **ითხოვს** tenant claim-ის არსებობას
(`platform.oidc.tenant-claim`, default `tenant_id`). მაგრამ claim-ის **მნიშვნელობა** არასდროს
დარდება მონაცემის მფლობელს: `platform.data_product`-ს tenant არ ჰქონდა, ამიტომ ნებისმიერი
ავთენტიფიცირებული tenant კითხულობდა ნებისმიერ product-ს. ეს არის OWASP API1.

Scope: authorization only. Authentication, role→authority mapping, quality/publication gate-ები და
contract compilation უცვლელია.

## 2. გადაწყვეტილება

1. **Object attribute.** `platform.data_product` იღებს `tenant_key NVARCHAR(160) NULL`-ს. ერთ
   product-ს ჰყავს **ზუსტად ერთი** მფლობელი tenant ან არცერთი (UNASSIGNED). site/tenant literal
   არსად არ იწერება — არც migration-ში, არც engine-ში.
2. **Subject attribute.** caller-ის tenant არის token-ის კონფიგურირებული claim-ის მნიშვნელობა
   (`platform.oidc.tenant-claim`). Claim-ის **სახელი** ერთ property-შია; მისი **მნიშვნელობა**
   opaque რჩება პლატფორმისთვის.
3. **Decision.** ერთი bean — `TenantAccessPolicy` — პასუხობს „may this caller reach this product?“.
   არაფერს კითხულობს და არაფერს წერს; მისი პასუხი არის `TenantAccessOutcome` enum-ის ერთი წევრი.
4. **Enforcement.** `TenantAccessGuard` ხსნის object → product-ს **ერთხელ** და ითხოვს
   გადაწყვეტილებას. თითოეულ boundary-ს აქვს **ზუსტად ერთი** enforcement point.
5. **Assignment.** product-ის tenant-ზე მიბმა არის governed, idempotent და audited ოპერაცია
   (`PUT /api/v1/platform/products/{productCode}/tenant`), ხელმისაწვდომი მხოლოდ cross-tenant
   authority-სთვის.

## 3. Claims model

| ატრიბუტი | წყარო | წესი |
|---|---|---|
| caller tenant | JWT claim, სახელი `platform.oidc.tenant-claim` (default `tenant_id`) | არსებობა უკვე სავალდებულოა `OidcRequiredClaimValidator`-ით; ცარიელი/blank → DENY |
| caller authorities | `OidcAuthoritiesConverter` (scope + roles map) | business role **არასდროს** კვეთს tenant საზღვარს |
| caller kind | `OIDC` / `LEGACY` / `SYSTEM` / `ANONYMOUS` | იხ. §4 და §6 |
| product tenant | `platform.data_product.tenant_key` | `NULL` = UNASSIGNED |

## 4. Deny-by-default წესები

`TenantAccessPolicy.decide(caller, product)` — რიგი მნიშვნელოვანია:

| # | პირობა | შედეგი |
|---|---|---|
| 1 | `platform.tenancy.enforcement-enabled=false` | `ALLOWED_ENFORCEMENT_DISABLED` (production-ში აკრძალული, §7) |
| 2 | caller kind = `SYSTEM` | `ALLOWED_SYSTEM` |
| 3 | caller kind = `ANONYMOUS` ან caller = null | `DENIED_NO_CALLER` |
| 4 | caller-ს აქვს cross-tenant authority | `ALLOWED_CROSS_TENANT_AUTHORITY` (audit log) |
| 5 | caller kind = `LEGACY` **და** `platform.oidc.enabled=false` | `ALLOWED_LEGACY_MODE` |
| 6 | product ვერ მოიძებნა | `DENIED_PRODUCT_UNRESOLVED` |
| 7 | product UNASSIGNED | `DENIED_PRODUCT_UNASSIGNED` |
| 8 | caller tenant ცარიელი/არარსებული | `DENIED_CALLER_TENANT_MISSING` |
| 9 | `caller.tenant != product.tenant_key` | `DENIED_TENANT_MISMATCH` |
| 10 | equal | `ALLOWED_TENANT_MATCH` |

**Non-OIDC modes.** როცა `platform.oidc.enabled=false`, პლატფორმა legacy/local რეჟიმშია: არცერთი
identity არ ატარებს tenant claim-ს და tenant scoping-ის შეფასება შეუძლებელია — ამიტომ ცხადად
დეკლარირებული `ALLOWED_LEGACY_MODE` branch-ი მოქმედებს. **ეს production-ში შეუძლებელია**: bean-ის
constructor-ი აგდებს `IllegalStateException`-ს, თუ profile შეიცავს `prod`-ს და OIDC გამორთულია.
როცა OIDC **ჩართულია**, არა-JWT authentication `ANONYMOUS`-ად ითვლება და იბლოკება — legacy
გზა ჩუმად ვერ გაიხსნება production-ში.

## 5. Coverage model — declared, not a URL list

Coverage **არ** არის pattern-ების სია. ის არის **დეკლარირებული და ტესტით დაცული თვისება**:

1. **Controller-ის დეკლარაცია.** ყოველი controller `org.base.api.controller`-ში ატარებს ზუსტად ერთს:
   `@TenantScoped` ან `@TenantNeutral(reason=..., legacy=?)`. `reason` სავალდებულოა და გულწრფელი.
2. **Enforcement point.** `TenantScopedAccessInterceptor` მიბმულია **`/api/v1/**`**-ზე — ანუ მთელ API
   ზედაპირზე, რადგან `CustomRequestMappingHandlerMapping` ყველა controller-ს (გარდა `@Sign`/`@Web`/
   `@NoApiPrefix`) აძლევს `/api/v1` + package-იდან მიღებულ folder prefix-ს. გადაწყვეტს **annotation**,
   არა URL. ამიტომ ახალი controller **default-ად დაფარულია**.
3. **Fail-closed default.** `@TenantScoped` route, რომელზეც **არცერთი resolver** არ აბრუნებს
   product-ს და არ არის `@TenantScopeExemption` → **DENY**. Governed package-ში `UNDECLARED`
   controller → **DENY** (და build ტესტი ვარდება მანამდე). Package-ის გარეთ (core/mobile legacy)
   `UNDECLARED` გადის — ისინი governed API არ არიან.
4. **Resolver port.** `TenantScopeResolver`: `supports → Optional<ResolvedScope>`; interceptor
   ატარებს რიგით და იღებს პირველ პასუხს. `null` product უარყოფილია ისევე, როგორც უცხო product.

| # | Resolver | identities |
|---|---|---|
| 10 | `ContractCodeScopeResolver` | `contractCode` (path var ან request param) |
| 20 | `ProductScopeResolver` | `productCode`, `productId` |
| 30 | `DataPlaneObjectScopeResolver` | `datasetVersionId`, `snapshotId`, `datasetSnapshotId`, `manifestId`, `runId`, `datasetLoadId`, `batchId`, `uploadSessionId` |
| 40 | `IngestionContractScopeResolver` | `contractSourceId`, `contractId` |
| 50 | `AsyncOperationScopeResolver` | `operationId` (→ `platform.api_operation.contract_code` → product) |

**Method-level exemptions** (`@TenantScopeExemption(Kind, reason)`), სამივე დოკუმენტირებულია:

| Kind | მნიშვნელობა |
|---|---|
| `DEFAULT_CONTRACT` | route იყენებს approved default contract-ს; interceptor მას resolve-ს და **ამოწმებს** (approved contract-ის არარსებობა = DENY) |
| `SERVICE_ENFORCED` | identity body-შია/ატვირთულ ფაილშია — interceptor ვერ წაიკითხავს body-ს მისი გახარჯვის გარეშე; enforcement არის service-ის იმ ერთ წერტილში, სადაც identity იხსნება (reason ასახელებს წერტილს) |
| `CALLER_OWNED` | object ეკუთვნის caller-ს და service ფილტრავს owner-ით |

### Service-layer enforcement points (body-borne identity)

| Route | Enforcement point |
|---|---|
| `POST /platform/publication/publish`, `/rollback` | `PlatformPublicationService.publish/rollback` → `requireProductId(request.productId())` |
| `POST /platform/ingestion/stage` | `PlatformIngestionService.stage` → `requireProductId` |
| `POST /platform/ingestion/prepare-snapshot` | `PlatformSnapshotPreparationService.prepare` → `requireDatasetLoad` |
| `POST /platform/ingestion/materialize` | `SemanticMaterializationService.materialize` → `requireSnapshot` |
| `POST /platform/artifacts/upload-sessions`, `/manifests/package[/preview]`, package-descriptor | `ArtifactPackageContractResolver.resolve` → `requireContract` |
| `POST /platform/artifacts/package-runs` | `PackageRunService.start` → `requireManifest` |
| `POST /platform/access/semantic/{preview,ingest}` | `PlatformAccessIngestionService.ingestPackage` → `requireContract` |
| `GET /platform/artifacts/entities/...` | `ArtifactDistributionService.published` → **404 masking** |

### Route-identity enforcement points (interceptor)

ყველა დანარჩენი `@TenantScoped` route იხსნება ზემოთ ჩამოთვლილი identity-ებით და მოწმდება
`TenantScopedAccessInterceptor.preHandle`-ში → 403 RFC 9457.

### Controller classification

| Controller | Scope | Route identity / exemption |
|---|---|---|
| `ContractQueryController` | SCOPED | `contractCode` |
| `ContractIntrospectionController` | SCOPED | `contractCode` |
| `ContractDiscoveryController` | SCOPED | `contractCode` |
| `CanonicalPageDataController` | SCOPED | `contractCode` param, else `DEFAULT_CONTRACT` |
| `DynamicDataController` | SCOPED | `contractCode` param, else `DEFAULT_CONTRACT` (charts route: legacy page ids, default contract) |
| `SdmxCompatibilityController` | SCOPED | `contractCode` param, else `DEFAULT_CONTRACT` |
| `PlatformMetricController` | SCOPED | `productId` |
| `PlatformVisualizationController` | SCOPED | `productId` |
| `PlatformAsyncOperationController` | SCOPED | `operationId`; submit: `contractCode` else `DEFAULT_CONTRACT` |
| `PlatformPublicationController` | SCOPED | `snapshotId`; publish/rollback: `SERVICE_ENFORCED` |
| `PlatformIngestionController` | SCOPED | `datasetLoadId`; stage/prepare/materialize: `SERVICE_ENFORCED` |
| `PlatformAccessIngestionController` | SCOPED | `contractSourceId`, `datasetLoadId` |
| `PlatformSemanticAccessController` | SCOPED | `batchId`; preview/ingest: `SERVICE_ENFORCED` |
| `PlatformSqlIngestionController` | SCOPED | `contractSourceId` |
| `PlatformArtifactController` | SCOPED | `contractCode`, `manifestId`, `snapshotId`, `uploadSessionId`; inventory/entities/session-start: `SERVICE_ENFORCED` |
| `PlatformArtifactPackageRunController` | SCOPED | `runId`; start: `SERVICE_ENFORCED` |
| `PlatformSiteContractRevisionController` | SCOPED | `contractCode` |
| `PlatformContractGovernanceController` | SCOPED | `contractId` |
| `PlatformContractMappingController` | SCOPED | `contractId`, `contractSourceId` |
| `HealthController` | NEUTRAL | liveness probe; no data product |
| `BuildInfoController` | NEUTRAL | build metadata |
| `PlatformProductTenancyController` | NEUTRAL | tenancy administration; cross-tenant authority-ით დაცული |
| `ResponseController` | NEUTRAL · **LEGACY** | სტატიკური demo payload-ები `ADMIN`-ქვეშ; dev stub — წასაშლელი |
| `XlsxToCsvController` | NEUTRAL · **LEGACY** | stateless XLSX→CSV; caller-supplied URL — retire ან dedicated authority + egress allow-list |
| `MSSQLToAccess` | NEUTRAL · **LEGACY** | caller-supplied credentials-ით გარე ბაზიდან export; product identity არ აქვს — retire ან explicit operator authority |
| `ManagedImportController` | NEUTRAL · **LEGACY** | core profile/page plane (`DataProfile`, `PageLeafNode`, per-page credentials) — `platform.data_product` identity **არ არსებობს**; ჩაწერილია როგორც ღია ხარვეზი |
| `biznes_statistika/MobileController`, `prices/{CpiCalculator,Inflation,kaleidoskope}`, `sagareo_vachroba/{FdiController,SagareoController}`, `sazogadoebastan_urtiertoba/ShedarebaPortali`, `socialuri_statistika/Salarium` (8) | NEUTRAL · **LEGACY** | per-domain Access upload core profile/page plane-ზე; product identity არ აქვთ — migrate ან explicit operator authority |

**LEGACY ნიშანი ნიშნავს ჩაწერილს, არა დამტკიცებულს.** ეს 12 ზედაპირი tenant-scoped **არ არის**
და ვერ გახდება ამჟამინდელი მოდელით: მათი მონაცემი core profile/page plane-შია, რომელსაც
`data_product` მიბმა არ აქვს. საჭიროა ან მათი migration governed ingestion line-ზე, ან მათთვის
ცალკე, explicit operator authority (`WRITE_RESOURCE` ამისთვის ძალიან ფართოა).

**Object-level rule (no existence oracle).** სადაც endpoint უკვე აბრუნებს 404-ს უცნობ object-ზე
(distribution), სხვა tenant-ის object აბრუნებს **იდენტურ** `ArtifactNotFoundException`-ს იმავე
ტექსტით — response ვერ გამოიყენება არსებობის შესამოწმებლად. სხვაგან 403 RFC 9457,
`Cache-Control: no-store`, მუდმივი detail (`TenantAccessDeniedException.DETAIL`), ყოველგვარი
object attribute-ის გარეშე (`TenantAccessExceptionHandler`, `ApiProblems`).

**SOLID.** policy იღებს გადაწყვეტილებას; repository ხსნის identity-ს; guard აერთიანებს ორს და
აძლევს boundary-ს ორ ფორმას (`permits*` / `require*`); resolver-ები resolver-ებად რჩებიან;
controller-ები თხელია. არცერთ service-ში არ დაემატა `Authentication` პარამეტრი.

## 6. System caller

`CurrentCaller` არის ერთადერთი injectable port, რომლითაც service იგებს ვინ ურეკავს
(`SecurityContextCurrentCaller` → `SecurityContextHolder`). Scheduler/worker/audit job-ს caller არ
ჰყავს; ის **ცხადად** აცხადებს `callers.asSystem("<JOB>", ...)`-ით. ამ scope-ის გარეთ caller-ის
არარსებობა არის `ANONYMOUS` და **იბლოკება** — ე.ი. caller-ის დაკარგვა ვერასდროს გახდება
შემთხვევითი bypass. Scope ThreadLocal-ია და მკაცრად შემოსაზღვრულია `finally`-ით.

`org.base.api`-ის ყველა `@Scheduled` მეთოდი, გადამოწმებული:

| Scheduled | guarded service-ს ეხება? | SYSTEM scope |
|---|---|---|
| `PackageRunWorker.advanceDueRuns` | კი (stage-ები: bind/reconcile/manifest document) | **`asSystem("ARTIFACT_PACKAGE_RUN")`** |
| `ArtifactRelationIntegrityAuditService.reconcileDueSnapshots` | კი (`ArtifactReconciliationService.reconcile`) | **`asSystem("ARTIFACT_RELATION_INTEGRITY_AUDIT")`** |
| `PlatformScheduledImportWorker.runMonthly` | კი (`PlatformSqlIngestionService`) | **`asSystem("monthly-platform-import")`** |
| `ArtifactObjectIntegrityAuditService.auditDueObjects` | არა — object store + repository | — |
| `ArtifactUploadSessionService.expireAndClean` | არა — sessions repository + staging store | — |
| `ArtifactStorageSweepService.sweepNextPage` | არა — inventory + sweep repository | — |
| `PlatformServingCacheService.rebuildPublished` | არა — `dataPlaneJdbcTemplate` | — |
| `PlatformOutboxProcessor.drain` | არა — JdbcTemplate | — |
| `PlatformArchiveRetentionService`, `PlatformArchivePayloadRepairService` | არა — archive JdbcTemplate + storage | — |

ეს ცხრილი ასევე ტესტით არის დაცული `TenantAccessGuardTest`-ის SYSTEM scope-ის საზღვრებით და
`PackageRunServiceTest`/`ArtifactRelationIntegrityAuditServiceTest`-ის fixture-ებით.

## 7. Cross-tenant authority

- property: `platform.tenancy.cross-tenant-authority`, default **`PLATFORM_CROSS_TENANT`** — განზრახ
  **არა** არსებული business role (`READ_RESOURCE`/`WRITE_RESOURCE`/`PUBLISH_RESOURCE`/`ADMIN`);
- ენიჭება IdP-ში role→authority map-ით (`OIDC_ROLE_AUTHORITY_MAP`), ე.ი. platform code-ში literal არაა;
- ყოველი გამოყენება ლოგდება (`tenancy.cross-tenant-access caller=<kind:sub> authority=... product=...`),
  **token-ის შიგთავსის გარეშე**;
- `@PreAuthorize("hasAuthority(@tenantAccessPolicy.crossTenantAuthority())")` — property ერთადერთი წყაროა.

**Fail-closed production guards** (არსებული bootstrap/cursor/issuer guard-ების სტილში):
`prod` profile-ზე `platform.tenancy.enforcement-enabled=false` ან `platform.oidc.enabled=false`
აჩერებს გაშვებას.

## 8. Cache / cursor safety

- `CursorTokenService` ხელს აწერს `(contractCode, pageId, [snapshot, fingerprint, keys, values])`-ს
  და `verify`-ზე ადარებს contract-სა და page-ს. contract → ზუსტად ერთი product → ზუსტად ერთი
  tenant, ამიტომ cursor უკვე tenant-scoped-ია; სხვა tenant-ის cursor-ის გამოყენებას ხელს უშლის
  boundary (a)-ს enforcement, რადგან იგივე `contractCode` მოწმდება მოთხოვნის დასაწყისში.
- `ContractQueryController`/`CanonicalPageDataController` ETag-ი პასუხის შიგთავსის SHA-256-ია და
  პასუხი `private, max-age=60, must-revalidate` + `Vary: ..., Authorization` — shared cache არ ინახავს
  და Authorization-ის ცვლილება ცალკე entry-ს ქმნის.
- `PlatformServingCacheService` და serving cache rows product/snapshot-ით არის keyed.
- **დასკვნა:** cross-tenant leak არ მოიძებნა; ამ ADR-ში არაფერი არ შეცვლილა cache/cursor კოდში.

## 9. Migration / compatibility / rollout

- `104_data_product_tenancy.sql` (Control Plane, რეგისტრირებული `PlatformSchemaMigrationRunner`-ში
  103-ის შემდეგ): additive, idempotent, არა-დესტრუქციული. ერთ batch-ში შესრულებისას ყველა
  statement, რომელიც ახალ სვეტს ასახელებს, გადატანილია `EXEC(N'...')`-ში.
- `platform.data_product_tenant_assignment` — append-only history (trigger 51104): `previous_tenant_key`,
  `tenant_key`, `transfer`, `reason`, `assigned_by`, `assigned_at`. transfer არსებული მფლობელისგან
  მოითხოვს reason-ს (CHECK constraint).
- **Backward compatibility:** არცერთი არსებული public method signature არ შეცვლილა caller-ის
  გადასაცემად. სამი constructor-ს დაემატა ახალი dependency
  (`ArtifactDistributionService`, `ArtifactPackageContractResolver`, `PackageRunWorker`).
- **Rollout (dev):** `deploy → assign → verify`. migration-ის შემდეგ **ყველა product UNASSIGNED-ია
  და ამიტომ უარყოფილია** ჩვეულებრივი caller-ისთვის, სანამ არ მოხდება governed assignment. ეს
  განზრახულია (fail-closed); assignment ერთი idempotent PUT-ია product-ზე.
- **Rollback:** `platform.tenancy.enforcement-enabled=false` (მხოლოდ არა-production) აბრუნებს ძველ
  ქცევას; schema additive-ია და rollback-ს არ საჭიროებს.

## 10. Failure modes

| მდგომარეობა | ქცევა |
|---|---|
| product UNASSIGNED | DENY (403 / 404 distribution-ზე) — deploy-სა და assignment-ს შორის |
| token-ს აკლია tenant claim | token უკვე უარყოფილია `OidcRequiredClaimValidator`-ით (401) |
| Control Plane მიუწვდომელია resolution-ისას | `DataAccessException` → არსებული handler → 503; **არასდროს allow** |
| worker caller-ის გარეშე | `ANONYMOUS` → DENY, თუ `asSystem` არ არის გამოცხადებული |
| cross-tenant authority არასწორად დარიგებული | ლოგდება ყოველ გამოყენებაზე; audit trail-ით აღმოჩენადი |
| assignment race | `UPDLOCK, HOLDLOCK` + transaction; idempotent ხელახალი assignment |

## 11. Tests

- `TenantAccessPolicyTest` — allow/deny-ს **ყველა** branch, legacy mode, blank claim, business role
  ვერ კვეთს საზღვარს, cross-tenant authority, production guard-ები.
- `TenantAccessGuardTest` — ყველა scope kind, denial-ი object attribute-ის გარეშე, unresolved ≡ foreign,
  SYSTEM scope-ის საზღვრები.
- `TenantScopedAccessInterceptorTest` — ყველა resolver-ის identity ოჯახი აღწევს guard-ს; scope-ის
  გარეშე scoped route **იბლოკება**; `DEFAULT_CONTRACT` exemption resolve-ს და ამოწმებს; approved
  contract-ის არარსებობა = DENY; `SERVICE_ENFORCED`/neutral გადის; governed package-ის გარეთ
  undeclared controller გადის; duplicate resolver-ები უარყოფილია.
- **`TenantScopeCoverageTest` — coverage guard** (`PlatformSchemaMigrationRegistrationTest`-ის
  სტილში, omission შეუძლებელია): (1) `org.base.api.controller`-ის ყოველი `@RestController`/
  `@Controller` ატარებს ზუსტად ერთ tenancy annotation-ს და `@TenantNeutral`-ს აქვს არა-ცარიელი
  reason; (2) ყოველი `@TenantScoped` handler-ის route ატარებს **რეალური resolver-ების** მიერ
  ამოსახსნელ identity-ს ან დოკუმენტირებულ `@TenantScopeExemption`-ს; (3) scoped ზედაპირი ჩუმად ვერ
  შემცირდება neutral-ად; (4) ყოველი legacy neutral ზედაპირი თავის reason-ში ცხადად წერს `LEGACY`-ს.
- `ArtifactDistributionTenantScopeTest` — **negative / no existence oracle**: სხვა tenant-ის entity
  იძლევა იდენტურ ტიპსა და message-ს, როგორც არარსებული; denial-ი არ კითხულობს contract-ს და store-ს.
- `ProductTenancyServiceTest` — idempotency, transfer-ის უარყოფა, reason-ის სავალდებულოობა, audit args.
- `PlatformProductTenancyControllerTest` — `no-store`, 200/404/409/403 RFC 9457.
- `DataProductTenancyMigrationTest` — additive, idempotent, `EXEC`-deferred, append-only, literal-free,
  რეგისტრირებული numeric order-ში.

## 12. Trade-off / ღია საკითხები

- product → tenant resolution ყოველ request-ზე ერთი control-plane SELECT-ია, cache-ის გარეშე.
  განზრახულია (assignment მაშინვე უნდა მოქმედებდეს); საჭიროებისას short-TTL cache ცალკე
  გადაწყვეტილებაა.
- `POST /platform/artifacts/manifests/inventory` contract-ზე მიბმული არაა და product scope-ს არ
  ატარებს; მას იცავს `WRITE_RESOURCE` და შემდგომი contract binding-ის guard (`SERVICE_ENFORCED`).
- Data/Archive Plane-ის row-level tenant column არ დაემატა: tenant მემკვიდრეობით გადადის
  product-იდან dataset → dataset_version → snapshot ჯაჭვით.
- **12 legacy NEUTRAL ზედაპირი** (ცხრილი §5) product identity-ს არ ატარებს და tenant-scoped ვერ
  გახდება ამ ცვლილების ფარგლებში; ისინი ჩაწერილია როგორც ღია ხარვეზი, არა როგორც უსაფრთხო.
- `PlatformAsyncOperationController#status/cancel`: operation, რომელსაც `contract_code` არ აქვს
  (შესაძლო legacy row), product-ს არ ამოხსნის და **იბლოკება**; caller-ის owner check რჩება მეორე,
  დამოუკიდებელ კონტროლად. დღეს ყველა operation contract-ით იქმნება.
- `PlatformSemanticAccessController#preview` არაფერს წერს და არაფერს აბრუნებს governed data-დან
  (მხოლოდ ატვირთული პაკეტის სტრუქტურას), ამიტომ `SERVICE_ENFORCED`-ად არის ნიშნული; ingest-ის
  იგივე პაკეტი enforcement-ს გადის.
