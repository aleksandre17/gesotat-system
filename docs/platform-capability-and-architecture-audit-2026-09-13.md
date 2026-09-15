# GEOSOTAT Platform — capability და architecture audit

**Current machine-checked checklist count (2026-09-14):**

**278 verified items**, **10 open items**.

**აუდიტის თარიღი:** 2026-09-13  
**შეფასების ობიექტი:** repository, მოქმედი KIDS R8 evidence, Control/Data/Archive Plane-ის დოკუმენტები, API implementation და migration-ები  
**სამიზნე:** მრავალსაიტიანი, contract-driven, metadata-driven, schema-agnostic data-product platform  

## მუდმივი engineering doctrine

კანონიკური repository-level policy: [`AGENTS.md`](../AGENTS.md). ეს audit ამ policy-ს შესრულების მიმდინარე evidence და gap register-ია.

ეს არის ამ პროექტის სავალდებულო გადაწყვეტილების წესი და ყველა მომავალ სამუშაოზე ვრცელდება:

> თითოეული პრობლემა უნდა გადაწყდეს მისი ზუსტი ბუნებისთვის ყველაზე შესაფერისი ინდივიდუალური მიდგომით — სრული, production-grade, canonical, contract-first, schema-agnostic, აბსტრაქტული მაგრამ პრაქტიკული, SOLID-თან თავსებადი, უსაფრთხო, ტესტირებადი, დაკვირვებადი, versioned და audit evidence-ით გამაგრებული. არ უნდა დაემატოს generic abstraction მხოლოდ abstraction-ისთვის; არ უნდა დარჩეს hardcoded business branch, დაუმტკიცებელი SQL, დუბლირებული semantic field ან documentation-only claim. ყოველი ცვლილება უნდა იყოს მკაფიოდ ორგანიზებული, backward-compatible წესით მართული და measurable acceptance-ით დამტკიცებული.

### გადაწყვეტილების 10-პუნქტიანი gate

ყოველი ახალი ცვლილება უნდა უპასუხებდეს:

1. რა ზუსტ პრობლემას აგვარებს და რა არის მისი bounded scope?
2. რომელ layer-ს ეკუთვნის — Control, Ingestion, Data, Archive, Serving, Security, Observability თუ Delivery?
3. რომელი contract/policy/schema არის authority?
4. არის თუ არა გადაწყვეტა site/provider/schema-agnostic?
5. ინარჩუნებს თუ არა domain semantics-ს და canonical boundaries-ს?
6. რა არის failure mode, rollback და idempotency წესი?
7. როგორ კონტროლდება authorization, privacy, cost და tenancy?
8. რომელი migration/versioning და compatibility წესია საჭირო?
9. რომელი unit/integration/negative/property/performance test ამტკიცებს მას?
10. რა machine-readable evidence და documentation განახლდება?

თუ ამ ათიდან რომელიმე პასუხი არ არსებობს, ცვლილება მზად არ არის production-ში გადასატანად.

## სამუშაო ჩეკლისტი — მიმდინარე სესია

- [x] `docs/`-ის მოქმედი status, capability-gap და execution-audit დოკუმენტების გადახედვა
- [x] API source/controllers/services/tests-ის capability inventory
- [x] DB migration/contract/Access responsibility-ის დოკუმენტებთან შედარება
- [x] Git working-tree და release provenance-ის შემოწმება
- [x] ოფიციალურ OpenAPI, OAuth, OWASP, OpenTelemetry, DCAT და SDMX ორიენტირებთან შედარება
- [x] maturity scorecard-ის შედგენა
- [x] P0/P1/P2/P3 gap-ებისთვის მექანიზმისა და acceptance კრიტერიუმის განსაზღვრა
- [x] პირველი დასაწყები სამუშაოს არჩევა: P0.1 clean/reproducible release inventory
- [x] P0.1-ის read-only release gate script-ის დამატება (`ops/cli/validation/release-gate.ps1`)
- [x] 247 ცვლილების კატეგორიული inventory და release policy-ის ჩაწერა (`docs/release-provenance-inventory-2026-09-13.md`)
- [x] anonymous ingest/import access-ის მოხსნა `ApiSecurityConfig`-იდან; compile PASS
- [x] API test suite გაშვება authorization change-ის შემდეგ; `:api:test` PASS
- [x] duplicate Apache POI dependency declarations removed; `:api:compileJava` and dependency resolution PASS
- [x] previously unclassified repository entries classified; `unknown = 0` in provenance inventory
- [x] compatibility analyzer გაფართოვდა dataset family/grain და relation endpoint/cardinality ცვლილებებზე; `:api:test` PASS
- [x] compatibility comparison-ს დაემატა invalid revision guard და target approvalRequired signal; `:api:test` PASS
- [x] query execution budget guard დაემატა page/limit/select/include/group/order/include-limit საზღვრებით; `:api:test` PASS
- [x] Actuator-ის დამატების შემდეგ `RequestMappingHandlerMapping` bean ambiguity გასწორდა qualifier + single bean registration-ით; `:api:test` PASS
- [x] API-ს დაემატა authenticated Actuator health/info/metrics surface; `:api:test` PASS
- [x] request correlation boundary დაემატა safe `X-Correlation-Id` response header + MDC propagation-ით; API tests PASS
- [x] correlation boundary-ის safe/unsafe header და MDC cleanup regression tests დაემატა; `:api:test` PASS
- [x] API security headers hardened: SAMEORIGIN frame policy, HSTS და strict referrer policy; `:api:test` PASS
- [x] `/platform/**` და `/actuator/**` explicit API security matcher-ში შევიდა; მხოლოდ `/actuator/health` არის public; `:api:test` PASS
- [x] production cursor-signing placeholder/short secret fail-fast enforcement და regression test დაემატა; `CursorTokenServiceTest` PASS
- [x] page cursor-ის signed replay window bounded გახდა 24 საათამდე; expired-cursor regression test PASS
- [x] production JPA `ddl-auto` default შეიცვალა `validate`-ზე; schema ownership დარჩა ordered migrations-ში
- [x] local `.env.prod` required configuration presence/placeholder scan; ყველა სავალდებულო key არის SET და cursor/JWT secret-ები საკმარისი სიგრძისაა (values არ გამოქვეყნებულა)
- [x] სრული multi-module regression suite (`gradlew test`) — BUILD SUCCESSFUL (`core:test`, `api:test`; mobile-ში tests არ არის)
- [x] pre-commit quality gate: `git diff --check` PASS და release secret scan PASS
- [x] serving-cache scheduler-ს დაემატა explicit enable switch და DB failure-ზე bounded warning/retry behavior; სრული test suite PASS
- [x] serving-cache scheduler default-ად disabled გახდა non-production-ში და prod profile-ში explicit enabled; test startup-ს აღარ აქვს unintended DB retry
- [x] scheduler default/profile separation-ის შემდეგ `:api:test` PASS და startup DB retry noise აღარ ფიქსირდება
- [x] serving-cache disabled-mode DB isolation regression test დაემატა; `PlatformServingCacheServiceTest` PASS
- [x] rate-limit production parameters explicit `.env.example` და `ops/compose/projects/geostat/docker-compose.prod.yml` contract-ში გამოიცხადა
- [x] platform rate-limit admission filter დაემატა bounded client-key store-ით, `429`/`Retry-After`/RateLimit headers-ით; compile/test PASS
- [x] rate-limit filter-ის quota/headers regression test დაემატა; `PlatformRateLimitFilterTest` PASS
- [x] rate-limit state provider გამოიყო `RateLimitStore` SPI-ად და bounded in-memory implementation-ად; provider swap core filter-ის შეცვლის გარეშე; test PASS
- [x] rate-limit decision telemetry დაემატა Micrometer-ში დაბალი კარდინალობის `accepted/rejected` counter-ებით; `PlatformRateLimitFilterTest` targeted PASS
- [x] field compatibility signature-ში dataset namespace, ordinal და source mapping expression დაემატა; `:api:test` PASS
- [x] JPA `open-in-view` გამორთულია, რათა persistence context web boundary-ს გარეთ არ გაგრძელდეს; regression suite PASS
- [x] contract query admission-ს დაემატა bounded predicate-tree depth/node budget და filter/include/page/order budget; `:api:test` PASS
- [x] production JWT configuration fail-closed guard დაემატა: placeholder/მოკლე secret და არასწორი expiration startup-ზე იბლოკება; `:api:test` PASS
- [x] production bootstrap-auth fail-closed guard დაემატა: ჩართული bootstrap რეჟიმი placeholder/მოკლე token-ით startup-ზე იბლოკება; `:api:test` PASS
- [x] authorization-surface preflight დაემატა და technical acceptance/release gate-ში ჩაირთო: 31 API controller inventory, global `anyRequest().authenticated()`, explicit resource method policy და intentional public liveness/auth surfaces fail-closed წესით მოწმდება; preflight PASS
- [x] OIDC policy preflight დაემატა: realm/client, bearer-only რეჟიმი, disabled direct grants/service accounts, required roles და `tenant_id` access-token mapper fail-closed მოწმდება; runtime issuer/JWKS binding კვლავ ცალკე ღიაა
- [x] relation/aggregation expansion-ის unbounded fan-out გასწორდა: contract physical relation traversal და generic aggregation input ერთიან 1000-row safety bound-ზე გადავიდა; `:api:test` PASS
- [x] legacy retirement preflight გაფართოვდა consumer-impact/deprecation/rollback/approval-record schema checks-ით; შედეგი `READY_FOR_IMPACT_REVIEW`, ავტომატური removal არ ხდება
- [x] release-gate-ში authorization-surface და OIDC policy preflight-ების რეალური execution ჩაირთო და შედეგები `build/release-authorization-surface.json`/`build/release-oidc-policy.json`-ში იწერება; diagnostic gate PASS
- [x] release-gate preflight artifacts-ის JSON schema/status/runtimeBinding content validation დაემატა; ცარიელი, malformed ან runtime overclaiming evidence fail-closed უარყოფილია; diagnostic gate PASS
- [x] remote staging authorization smoke შესრულდა read-only რეჟიმში: public liveness `/health`=200, protected contract capability endpoint without credentials=401; JWT scope/tenant negative replay კვლავ ღიაა
- [x] production-evidence-readiness runner დაემატა და acceptance-ში ჩაირთო; ყველა external gate ერთ canonical JSON-ში იკრიბება, `releaseDecision=NOT_READY_FOR_PRODUCTION` და `PASS` არ იფიქსირება approval-ის გარეშე
- [x] release-gate-მა production-evidence-readiness runner-ის execution, JSON schema და fail-closed `NOT_READY_FOR_PRODUCTION` decision-ის validation აიღო საკუთარ თავზე; diagnostic gate PASS
- [x] evidence-bundle generator-ის repository-root resolution გასწორდა (`../../..`), რის შედეგადაც readiness/preflight/gate artifacts-ის bundle generate+verify რეალურად PASS გახდა; 5 files და SHA-256 დაფიქსირდა
- [x] remote protected-endpoint denial headers შემოწმდა: anonymous 401 პასუხი შეიცავს `nosniff` და `no-store/no-cache` policy-ს, ასევე explicit `Vary` headers-ს; read-only smoke PASS
- [x] repository whitespace hygiene გასწორდა README-ის dynamic deployment line-ზე; `git diff --check` PASS
- [x] supply-chain scanner availability rechecked locally and on the isolated host; `syft`, `trivy` და Docker Scout მიუწვდომელია, ამიტომ SBOM/vulnerability scan honest `NOT_RUN`-ად დარჩა და false PASS არ გამოცხადდა
- [x] evidence bundle root/verifier regression დაემატა technical acceptance-ში: arbitrary output location, repository-root anchored path validation და traversal rejection; acceptance replay PASS
- [x] `ContractPhysicalQueryService` physical-table resolution revision-bound გახდა: `revisionId` ახლა `site_contract_dataset`-ის EXISTS guard-ით სავალდებულოდ მონაწილეობს, რათა სხვა/უახლესი revision-ის table accidental query-ში არ მოხვდეს; სრული `:api:test` PASS
- [x] `ContractMetadataService` physical table/index introspection-იც revision-bound გახდა იგივე contract dataset EXISTS guard-ით; query და introspection ერთ revision-ს აღარ აცდება; სრული `:api:test` PASS
- [x] `ContractRuntimeValidator`-ის pre-query physical mapping guard-იც იგივე revision binding-ზე გადავიდა; validator/metadata/executor revision consistency დადასტურდა სრული `:api:test`-ით
- [x] introspection `allowedIncludes`-დან undeclared hardcoded `classification/metadata/lineage` values მოიხსნა; include surface ახლა მხოლოდ revision-scoped declared relations-ით განისაზღვრება; სრული `:api:test` PASS
- [x] release-gate-ში `git diff --check` fail-closed enforcement დაემატა; whitespace/hygiene defect release diagnostic-ში ვეღარ გაივლის
- [x] release-provenance inventory-ში ძველი `scripts/release-gate.ps1` references canonical `ops/cli/validation/release-gate.ps1` path-ზე გასწორდა; documentation drift checker PASS
- [x] JWT production guard-ის positive/negative configuration regression tests დაემატა; `:core:test` PASS
- [x] production deployment preflight დაემატა: `.env.prod` required keys, placeholder/secret policy, rate-limit parameters, compose runtime mapping და `prod` profile; preflight PASS
- [x] production runtime health evidence: `http://192.168.1.199:8083/health` returned `status=UP` with primary/data/archive/secondary SQL and object storage all `UP`; host-side preflight's internal `minio` DNS probe is documented as a network-context limitation
- [x] release provenance gate production preflight-ს სავალდებულო artifact-ად ამოწმებს; diagnostic gate + preflight PASS
- [x] strict release gate evidence tooling-ის generator-თან ერთად verifier-საც სავალდებულოდ ამოწმებს; missing verifier fail-closed
- [x] legacy/server preflight `scripts/check.sh`-ში cursor secret და rate-limit required keys/length policy აისახა; remote execution გარემოს გარეშე დარჩა დაუმტკიცებელი
- [x] CORS policy hardcoded-only რეჟიმიდან env-driven allowlist-ზე გადავიდა; production origin/pattern Compose და preflight contract-შია; `:api:test` + preflight PASS
- [x] contract/page query responses-ის cache semantics `private` გახდა (shared-proxy data leakage guard), მათ შორის ETag 304 path; `:api:test` PASS
- [x] origin/content-negotiation variance დაემატა serving responses-ს (`Vary: Origin, Accept, Accept-Language`), მათ შორის 304 path; `:api:test` PASS
- [x] canonical page და contract query endpoints-ზე JSON `Accept` negotiation explicit გახდა (`produces=application/json`), unsupported media type framework-ის 406 path-ზე გადადის; `:api:test` PASS
- [x] API security headers-ში restrictive CSP დაემატა (`default-src 'none'`, no forms/base URI, same-origin framing); `:api:test` PASS
- [x] canonical page `304 Not Modified` პასუხსაც `private` cache-control დაემატა, რათა fresh/conditional paths ერთნაირად იყოს დაცული; `:api:compileJava` + diff check PASS
- [x] contract query serving response variance-ში `Authorization` დაემატა (`Vary`), მათ შორის 304 path; `:api:compileJava` + diff check PASS
- [x] API security chain-ში `Permissions-Policy` დაემატა (`camera/microphone/geolocation=()`); `:api:test` PASS
- [x] legacy canonical page endpoint-იც contract query-ის page/limit/pageId/contractCode admission budget-ს დაექვემდებარა; `:api:test` PASS
- [x] `.env.prod` ignored/not tracked არის; preflight secret values-ს არ ბეჭდავს და diagnostic release gate PASS-ია
- [x] Actuator metrics-ს დაემატა stable `service` და `environment` tags ownership/dashboard correlation-ისთვის; `:api:test` + diff check PASS
- [x] RFC 9457 problem responses-ს დაემატა `Cache-Control: no-store`, რათა correlation/instance metadata cache-ში არ მოხვდეს; `:api:test` + diff check PASS
- [x] security `401/403` error boundary-ც ერთიანად hardened გახდა: `no-store`, `no-cache` და `nosniff`; `:api:test` + diff check PASS
- [x] Access artifact size ceiling ერთ canonical env/property-ზე გადავიდა (`PLATFORM_MAX_ACCESS_ARTIFACT_BYTES`), multipart და controller guard ერთნაირ ლიმიტს იყენებს; preflight + `:api:test` PASS
- [x] 401/403 responses-ში safe correlation ID response header/body-ში ერთიანად ბრუნდება; invalid client ID trusted value-ით არ გადის; `:core:test` + diff check PASS
- [x] rate-limit `429` response-ც hardened გახდა: `Retry-After`, `no-store`, `nosniff` და RFC 9457 media type; `PlatformRateLimitFilterTest` PASS
- [x] rate-limit `429` response-ში safe correlation ID header/body დაემატა; `PlatformRateLimitFilterTest` PASS
- [x] rate-limit surface-ს დაემატა interoperable `RateLimit-Policy` (`limit;w=windowSeconds`) header; `PlatformRateLimitFilterTest` PASS
- [x] canonical page serving response-ებსაც დაემატა `Vary: Authorization`, მათ შორის ETag 304 path; `:api:compileJava` + diff check PASS
- [x] rate-limit regression test ახლა პირდაპირ ამოწმებს `RateLimit-Policy`, `no-store` და correlation header-ს; targeted test PASS
- [x] `scripts/check.sh`-ის placeholder failure output-იდან secret value redacted გახდა; diagnostic logs-ში credential leakage აღარ ხდება
- [x] ბოლო security/config ცვლილებების შემდეგ production preflight, diagnostic release gate და `git diff --check` ერთობლივად PASS; secrets არ გამოქვეყნებულა
- [x] სრული multi-module regression + production preflight + diagnostic release gate + diff check ერთობლივად PASS; final local evidence refreshed
- [x] production CORS preflight/check-ში wildcard origins და patterns fail-closed იბლოკება; current `.env.prod` PASS
- [x] audit checklist count verifier დაემატა და release gate-ში ჩაერთო; actual `71/7` vs declared `71/7` PASS
- [x] operations runbook-ში executable production-preflight, checklist verifier და strict release-gate sequence დოკუმენტურად ჩაემატა
- [x] complete-package index-ში operations runbook და ორივე executable verification script canonical სწავლის/გაშვების entrypoint-ად დაემატა
- [x] complete-package index-ის 9 local links ყველა ვალიდურია; broken-reference sweep PASS
- [x] complete-package index release gate-ის required artifact-ად enforce-დება; diagnostic gate + checklist verifier PASS
- [x] read-only production runtime audit: `192.168.1.199` SSH BatchMode PASS; `geostat-api` healthy/restart=0, MinIO running, Docker networks present; secrets არ წაკითხულა
- [x] remote production Compose validation: deployed `ops/compose/projects/geostat/docker-compose.prod.yml` + `.env.prod` `docker-compose config --quiet`-ით PASS; secret values არ გამოქვეყნებულა
- [x] remote canonical `/health` endpoint GET `200`/`UP` evidence: primary, dataPlane, archivePlane, objectStorage და secondary ყველა `UP`
- [x] rate-limit bounded store `@ConditionalOnMissingBean(RateLimitStore)`-ით default provider გახდა; distributed provider swap bean conflict-ის გარეშეა შესაძლებელი; targeted test PASS
- [x] API/mobile Dockerfiles-ს OCI `org.opencontainers.image.revision/source` labels დაემატა და Compose build args-ით CI commit/tag-ის გადმოცემა შეუძლია; preflight/diff PASS
- [x] operations runbook-ში source→image provenance rebuild და live OCI revision equality acceptance ნაბიჯი დეტალურად ჩაიწერა
- [x] remote production read-only evidence ცალკე reproducible note-ში არქივირდა; secrets/payloads არ ინახება
- [x] Spring Boot plugin/BOM version drift გასწორდა ერთიან `3.2.3` baseline-ზე; სრული `gradlew test --no-daemon` საბოლოოდ PASS
- [x] rate-limit default provider wiring component-scan ambiguity-დან ცალკე conditional configuration-ში გადავიდა; `ApiApplicationTests.contextLoads` და სრული `gradlew test --no-daemon` PASS
- [x] release-gate-ს დაემატა optional fail-closed source→OCI image revision check (`-ImageName` + `-ExpectedImageRevision`); mismatch/unavailable image-ზე exit 1 verified
- [x] JWT filter-ის broad `import/` anonymous bypass მოიხსნა; ingest/import request-ები authentication boundary-ს აღარ გვერდს უვლის; სრული `gradlew test --no-daemon` PASS
- [x] JWT generic error path hardened: stack trace და raw exception message აღარ გადის client/log boundary-ში; სრული `gradlew test --no-daemon` PASS
- [x] legacy `xlsx-to-csv` conversion/download endpoints-ის anonymous permit და JWT-filter bypass მოიხსნა; file-fetch/export surface authenticated გახდა; სრული `gradlew test --no-daemon` PASS
- [x] remote XLSX fetch hardened: HTTPS-only, DNS private/loopback/link-local rejection, redirects disabled, connect/read timeouts და 100 MiB bounded streaming; wildcard controller CORS removed; სრული `gradlew test --no-daemon` PASS
- [x] XLSX-generated HTML/JavaScript safely escapes workbook file და sheet names; uploaded content cannot inject markup/script through display metadata; სრული `gradlew test --no-daemon` PASS
- [x] global exception boundary sanitized: unexpected/JWT/security/validation responses აღარ ასახავს raw exception messages; `IllegalArgumentException` handler სწორ ტიპს იღებს; სრული `gradlew test --no-daemon` PASS
- [x] legacy upload/MSSQL-to-Access error paths აღარ აბრუნებს raw exception text-ს და აღარ იყენებს stdout stack trace-ს; logs structured `error` boundary-ზეა; სრული `gradlew test --no-daemon` PASS
- [x] WebSocket JWT handshake აღარ წერს Authorization/task/user data-ს stdout-ზე და აღარ აბრუნებს raw JWT exception text-ს; sanitized structured warning + generic client error; სრული `gradlew test --no-daemon` PASS
- [x] დარჩენილი production stdout leakage-ები (Access output path, route mapping და login redirect) structured logger-ზე გადავიდა; sensitive path/principal data აღარ იბეჭდება; სრული `gradlew test --no-daemon` PASS
- [x] WebSocket/legacy controller logging audit-ის შემდეგ source tree-ში დარჩენილი `System.out` მხოლოდ zero-sensitive informational generation line-ადაც structured logger-ზე გადავიდა; `printStackTrace`/stderr leakage არ დარჩა; სრული `gradlew test --no-daemon` PASS
- [x] Public mobile-text validation errors აღარ აბრუნებს raw `IllegalArgumentException` ტექსტს; generic client-safe response-ით ბრუნდება; სრული `gradlew test --no-daemon` PASS
- [x] async export-ს დაემატა configurable byte ceiling (`PLATFORM_QUERY_MAX_EXPORT_BYTES`) და failure-ზე partial artifact cleanup; Compose/.env/preflight ერთიანად ამოწმებს პოზიტიურ ლიმიტს; სრული test + preflight PASS
- [x] async operation executor bounded გახდა: 2 worker + 32 queued jobs, deterministic queue-full failure და `@PreDestroy` graceful shutdown; სრული `gradlew test --no-daemon` PASS
- [x] legacy dynamic table reader-ის page admission bounded გახდა (`1..100000`) და SQL OFFSET long arithmetic-ზე გადავიდა; integer overflow/resource abuse path მოიხსნა; სრული `gradlew test --no-daemon` PASS
- [x] async operation request payload-ს დაემატა 256 KiB UTF-8 ceiling და status response-დან internal `error_detail` მოიხსნა; Compose/.env/preflight policy ერთიანად PASS; სრული test + preflight PASS
- [x] AccessFileImporter-ს დაემატა defense-in-depth extension და 1 GiB service-level size validation, controller-ის მიღმა caller-ების დასაცავად; სრული `gradlew test --no-daemon` PASS
- [x] dynamic chart profile/filter admission bounded გახდა: მაქს. 128 display fields, 64 KiB filter JSON და 32 filter rule; oversized declarative chart query fail-closed; სრული `gradlew test --no-daemon` PASS
- [x] web error redirect path-იც sanitized გახდა: `RequestDispatcher.ERROR_MESSAGE` raw exception-ის ნაცვლად safe response message-ს იყენებს; სრული `gradlew test --no-daemon` PASS
- [x] Access importer-ის progress/error boundary sanitized გახდა: WebSocket/client-ს აღარ ეგზავნება DB/driver exception ტექსტი და dead `extractMessage` helper მოიხსნა; სრული `gradlew test --no-daemon` PASS
- [x] async operation/export endpoint-ს დაემატა validated `Idempotency-Key`, request fingerprint, unique SQL Server index და concurrent retry-ზე existing operation-ის დაბრუნება; migration 079 და runner wiring; სრული test PASS
- [x] authenticated `/api/v1/build-info` endpoint და Docker runtime `BUILD_REVISION` დაემატა release provenance acceptance-ისთვის; მხოლოდ non-sensitive revision/source/timestamp ბრუნდება; სრული test PASS
- [x] legacy MSSQL→Access endpoint-ის database/table identifier boundary გამკაცრდა (`[A-Za-z_][A-Za-z0-9_]*`), connection input-ებზე null/length/control-character validation დაემატა და bracket/backtick injection path მოიხსნა; სრული test PASS
- [x] SQL Server/MySQL provider connection paths-ში TLS verification გამკაცრდა: `trustServerCertificate=false`, MySQL `useSSL/requireSSL/verifyServerCertificate=true` და public-key retrieval გამორთულია; insecure transport defaults აღარ არსებობს; სრული test PASS
- [x] provider import strategies და SQL ingestion endpoint-ებში host[:port] და quoted identifier allowlist ერთიანად გამკაცრდა; table/column/database bracket/backtick/JDBC-option injection path-ები მოიხსნა; სრული test PASS
- [x] მეორე site-ის contract-only local proof (`ContractOnlyOnboardingTest`, `ContractOnlyDatabaseReplayTest`) targeted Gradle run-ით PASS (2026-09-14 rerun); production provider acceptance ცალკე ღია gate-ად დარჩა
- [x] production deploy script now derives and exports immutable `IMAGE_REVISION` and fail-closed rejects provenance-less or dirty production checkouts before upload/build
- [x] publication service now enforces all seven declared release-gate evidence codes (`SCHEMA_VALID` through `PUBLICATION_ATOMIC`) before snapshot publication; `:api:test` PASS
- [x] current production recheck: `geostat-api` and `geostat-mobile` healthy, canonical `/health` returns all GEOSOTAT planes `UP`; unrelated co-hosted `geostat-chat-ai-api` health is tracked separately and is outside this platform scope
- [x] deploy/rollback SSH Compose invocations explicitly forward `IMAGE_REVISION`, preventing remote builds from silently falling back to `unknown` provenance; formatting/checklist validation PASS
- [x] diagnostic release gate now statically verifies deploy provenance forwarding and clean-checkout guard; `release-gate.ps1 -AllowDirty` PASS
- [x] uncached full workspace regression (`gradlew test --no-daemon --rerun-tasks`) completed with all 12 actionable tasks executed and `BUILD SUCCESSFUL`
- [x] production Compose build arguments now require non-empty `IMAGE_REVISION` (no `unknown` fallback); release gate statically verifies this invariant and passes
- [x] dynamic chart/table providers-ში metadata-sourced JDBC host/database და MySQL table identifiers allowlist-ით validated გახდა; semicolon/options და backtick URL injection path-ები fail-closed არის; სრული test PASS
- [x] legacy `/import/**` და `/xlsx-to-csv/**` surfaces-ზე resource-level method authorization დაემატა: import `WRITE_RESOURCE`, conversion/read `READ_RESOURCE`; authenticated-but-unauthorized callers fail-closed; სრული test PASS
- [x] synthetic `/test/**` surface `ADMIN` authority-ით შეიზღუდა და automobile-statistics multipart upload `WRITE_RESOURCE`-ზე გადავიდა; broad authenticated-only access მოიხსნა; სრული test PASS
- [x] ყველა დარჩენილი legacy multipart upload controller (CPI, inflation, kaleidoscope, FDI, foreign trade, social statistics, comparison portal) `WRITE_RESOURCE` method policy-ით დაიხურა; სრული controller sweep PASS
- [x] governed contract POST/PUT/PATCH negotiation boundary-ში `Content-Type: application/json` სავალდებულო გახდა; სხვა ან ცარიელი media type deterministic `415`-ით უარყოფილია; სრული test PASS
- [x] generated Access/CSV/ZIP download responses-ზე `Cache-Control: no-store, private` და `Pragma: no-cache` დაემატა; sensitive artifact browser/proxy caching აღარ ხდება; სრული test PASS
- [x] JWT issuance/parser-ში issuer და audience claims/validation დაემატა; production `.env`/Compose/preflight contract-ში `JWT_ISSUER` და `JWT_AUDIENCE` სავალდებულოა; token-confusion boundary fail-closed არის; სრული test + preflight PASS
- [x] JWT HMAC key derivation platform-default charset-ის ნაცვლად explicit UTF-8-ზე გადავიდა; cross-platform signing/verification determinism დადასტურებულია სრული test suite-ით
- [x] async operation ledger-ს დაემატა owner scope (`requested_by`), owner-scoped idempotency index და status/cancel owner-or-admin enforcement; operation ID enumeration/data leakage fail-closed არის; migration 080 + full Gradle test PASS
- [x] Windows-compatible full preflight (`scripts/check.bat --no-build`) rerun after building both production boot JARs; local artifacts, environment policy, SSH, remote containers და network checks PASS (ერთი infrastructure warning: remote `minio/` source directory deploy-ზე შეიქმნება)
- [x] სრული build-mode preflight (`scripts/check.bat`) PASS exit code-ით: Gradle/build prerequisites, environment policy, SSH, remote Compose directories, healthy API/mobile containers და network checks დადასტურდა; მხოლოდ remote MinIO source-directory creation warning დარჩა
- [x] production runtime capability recheck: deployed container-ის security/observability environment inventory read-only რეჟიმში შესრულდა; მხოლოდ JWT variables გამოჩნდა, OIDC/tenant/OTEL runtime bindings არ დადასტურდა და შესაბამისად შესაბამისი hardening/evidence gates ღიად დარჩა (`docs/remote-production-evidence-2026-09-13.md`)
- [x] deploy host-ის Docker/Compose inventory read-only რეჟიმში გადამოწმდა: GEOSOTAT stack-ში მხოლოდ API/mobile/MinIO სერვისებია; co-hosted Redis/RabbitMQ სერვისები სხვა აპლიკაციებს ეკუთვნის და არ იქნა მიბმული პლატფორმაზე (`docs/remote-production-evidence-2026-09-13.md`)
- [x] GEOSOTAT-owned shared-infrastructure contract დაემატა (`infra/ops/compose/projects/geostat/docker-compose.prod.yml`): external `geostat-net`-ზე pinned Redis, Keycloak+Postgres და OTel Collector; secrets mandatory `:?` guards-ითაა, ხოლო `scripts/deploy-infra.sh` placeholder-იან ან არასრულ `.env.prod`-ზე fail-closed ჩერდება; actual start/health evidence secrets-ისა და deployment approval-ის მიღებამდე ღიაა
- [x] GEOSOTAT shared infrastructure რეალურად განთავსდა იზოლირებულ `/home/administrator/geostat/backend/infra/geostat-platform` ქვეფოლდერში; Redis/Keycloak DB healthy, Keycloak/OTel running, parent infra compose აღდგენილია და სხვა 28 co-hosted container untouched დარჩა; API OIDC/Redis/OTel integration და end-to-end acceptance ცალკე ღია gate-ებად რჩება
- [x] `provision-infra-remote.sh`-მა fresh isolated infra provisioning-ისას `GRAFANA_ADMIN_PASSWORD` generation დაამატა; required secret set ახლა Compose-ის ყველა Grafana/Redis/Keycloak guard-ს ფარავს და deployment-secret-injection preflight PASS-ია
- [x] deployment-secret-injection preflight ახლა fresh bootstrap (`provision-infra-remote.sh`)-საც ამოწმებს; provisioning-ის გამონაკლისი მკაფიოდ მოდელირებულია, repository secret path-ები და სხვა entrypoint-ების injection contract კვლავ fail-closed რჩება
- [x] `provision-infra-remote.sh` idempotent repair დაემატა legacy `.env.prod`-ისთვის: მხოლოდ არმყოფი/ცარიელი `GRAFANA_ADMIN_PASSWORD` ემატება, არსებული secret values არ იკითხება და არ იცვლება; Compose-ის required guard-ის ნახევრად-ვალიდური მდგომარეობა გამორიცხულია
- [x] explicit `docker-compose.secure.yml` overlay და `secure-overlay-preflight.ps1` დაემატა: OIDC issuer/audience, Redis quota და OTLP endpoint secure რეჟიმში `:?` fail-closed guards-ით მოითხოვება; static preflight PASS, overlay საბაზო compose-ს implicit-ად არ ცვლის
- [x] secure overlay და მისი preflight runtime ledger-ის required implementation markers-ად დაემატა; ledger regeneration/verification-ზე დაცვის ფენის არსებობა fail-closed მოწმდება
- [x] observability infra hardening დაემატა: OTel `health_check` extension, Prometheus/Grafana/Keycloak healthchecks და Prometheus-ready dependency gating; YAML parse და secret preflight PASS, remote redeploy intentionally not asserted
- [x] OTel Collector telemetry durability გაძლიერდა `file_storage` extension-ით, persistent `geostat-otel-data` volume-ით და exporter sending queues-ით; YAML validation PASS, durable backend/alert authority კვლავ external gate-ად რჩება
- [x] production authority input contract დაემატა (`docs/production-authority-input-contract.md`): issuer/JWKS/claim mapping, tenancy, distributed quota, OTel/SLO/alerts, recovery, release authority და test window ერთ revisioned, secret-free და fail-closed ჩანაწერად განისაზღვრა; ეს governance მექანიზმია და დაუმტკიცებელ production მნიშვნელობებს დასრულებულად არ აცხადებს
- [x] distributed quota-ის ტექნიკური adapter დაემატა: Redis-backed atomic Lua counter (`RedisRateLimitStore`) provider-neutral `RateLimitStore` SPI-ით, hashed bounded keys და first-write TTL; feature flag, endpoint binding და secret-free example/preflight contract დაემატა; `:api:test` PASS; production enablement, HA/fairness/load evidence კვლავ P1 gate-ად ღიაა
- [x] API-ს OTLP metrics export wiring დაემატა Micrometer registry-ით, endpoint/step/enablement configuration-ით და production-safe disabled-by-default რეჟიმით; `:api:test` PASS; durable backend, trace correlation, SLO და alert evidence კვლავ ღიაა
- [x] production preflight გამკაცრდა: Redis quota და OTLP metrics თუ ჩართულია, endpoint-ის არსებობა, URI scheme და localhost/placeholder-ის აკრძალვა fail-closed მოწმდება; local production preflight PASS
- [x] generic evidence-bundle generator დაემატა (`ops/cli/data/generate-evidence-bundle.ps1`): source/contract/snapshot/gate file SHA-256, non-secret metadata, release reference და sidecar checksum ერთ deterministic bundle-ში იკრიბება; release gate-ში მისი არსებობა მოწმდება
- [x] evidence bundle verifier დაემატა (`ops/cli/data/verify-evidence-bundle.ps1`): sidecar checksum, schema, release reference, referenced-file hash/size და read-only immutability-ის fail-closed შემოწმება; generated bundle-ზე PASS
- [x] technical acceptance runner დაემატა (`ops/cli/validation/technical-acceptance.ps1`): release/OIDC/provider/Redis/OTel checks, synthetic acceptance, სრული API regression და audit consistency; report PASS (`build/technical-acceptance-report.json`)
- [x] documentation zero-drift checker დაემატა (`scripts/documentation-zero-drift.ps1`): required docs/scripts, W-01–W-07/A-01–A-10 scope და audit count consistency; PASS
- [x] keyset predicate-ის ascending/descending tuple, mismatch და identifier-safety regression tests დაემატა (`KeysetPredicateBuilderTest`); targeted Gradle test PASS
- [x] provider capability registry-ის provider-neutral core დაემატა (`ProviderCapabilityRegistry`) — validated metadata, capability negotiation, duplicate/retire fail-closed და targeted Gradle test PASS
- [x] provider capability registry-ის Control Plane persistence schema დაემატა migration `081_provider_capability_registry.sql`-ით; `:api:compileJava` PASS
- [x] provider capability runtime discovery დაემატა `ProviderCapabilityDiscoveryService`-ით: მხოლოდ ACTIVE rows, grouped capabilities და fail-closed metadata; targeted `ProviderCapability*Test` PASS
- [x] documentation zero-drift checker გაძლიერდა: completion plan C-01–C-14, migration 081 და provider discovery implementation-ის cross-artifact validation; checker PASS
- [x] provider-neutral semantic compatibility analyzer core დაემატა metric/unit/aggregation/dimension/policy/projection/response semantics-ის deterministic comparison-ით; invalid input fail-closed და targeted test PASS
- [x] semantic analyzer გაფართოვდა `codeLists`, `approvalRequired` და machine-readable migration guidance-ით; targeted test PASS
- [x] contract TypeScript generator-ის field identifiers sanitized გახდა (reserved words/invalid characters/leading digits); `ContractClientGeneratorServiceTest` targeted PASS
- [x] async export-ის unknown format აღარ გადადის ჩუმად JSON fallback-ზე — unsupported format fail-closed error-ად სრულდება; სრული `:api:test` PASS
- [x] provider/family-neutral `DataFamilyLifecycle` state machine დაემატა: canonical lifecycle transitions, idempotent repeat და invalid transition fail-closed; targeted test PASS
- [x] contract-revision lifecycle core დაემატა `ContractLifecycle`-ით: review gate, approve, supersede/rollback და terminal-state immutability; targeted test PASS
- [x] stable sort metadata contract დაემატა `StableSortSpec`-ით: tuple uniqueness, ASC/DESC, NULLS FIRST/LAST და required index declaration; unsafe metadata tests PASS
- [x] contract payload checksum binding დაემატა `ContractChecksumBinding`-ით: deterministic SHA-256 და constant-time verification; malformed input fail-closed და targeted test PASS
- [x] generated runtime ledger დაემატა (`ops/cli/data/generate-runtime-ledger.ps1`): canonical migration inventory, SHA-256 fingerprints, audit-count reconciliation და required implementation markers; production authority intentionally NOT_ASSERTED
- [x] runtime ledger-ში contract code/revision markers-ის extraction დაემატა; generated JSON-ში migrations და contract markers ერთ evidence ჩანაწერშია და production approval ცალკე NOT_ASSERTED რჩება
- [x] runtime ledger verifier დაემატა (`ops/cli/data/verify-runtime-ledger.ps1`): migration hash recheck, duplicate/missing ID rejection, audit-count და required-marker verification; targeted PASS
- [x] legacy dynamic-pages no-fallback regression დაემატა (`DynamicDataControllerNoFallbackTest`): disabled compatibility flag-ზე legacy reader არ გამოიძახება და request fail-closed სრულდება; targeted test PASS
- [x] strict release gate-ში runtime ledger generation/verification დაემატა; `-AllowDirty` diagnostic run PASS, clean release კვლავ სწორად fail-closed რჩება
- [x] runtime ledger tamper-resistance smoke დაემატა: შეცვლილი migration hash verifier-მა უარყო; technical acceptance-ში ჩართულია და PASS-ია
- [x] contract compatibility response გამდიდრდა `fromChecksum`/`toChecksum`/`checksumChanged` ველებით; structural breaking diff-ზე `approvalRequired=true` და regression test PASS
- [x] statistical query SQL გამოიყო `StatisticalQueryAdapter`/`CanonicalStatisticalQueryAdapter` boundary-ში; `PlatformMetricQueryService` storage table names-ს აღარ იცნობს და API regression compile/test PASS
- [x] supply-chain preflight დაემატა: Compose image inventory, floating `latest`/untagged image rejection და explicit SBOM scan `NOT_RUN` status; static preflight PASS
- [x] provider capability negotiation core დაემატა `ProviderCapabilityNegotiator`-ით: family/operation/feature/page-limit mismatch fail-closed და targeted tests PASS
- [x] provider lifecycle core დაემატა `ProviderLifecycle`-ით: register/validate/bind/health/fail/retire transitions, idempotency და fail-closed tests PASS
- [x] generated OpenAPI surface validator დაემატა `ContractOpenApiValidator`-ით: OpenAPI 3.1, info version, path/POST shape და unique operationId validation; malformed document tests PASS
- [x] generated OpenAPI runtime path validator-თან დაიბმა: `ContractOpenApiService` malformed contract-derived document-ს დაბრუნებამდე fail-closed აჩერებს
- [x] semantic compatibility golden suite გაფართოვდა ყველა contract surface set-ზე (dimensions/codeLists/filters/includes/policies/projection/response); additive vs removal classification და approval requirement PASS
- [x] supply-chain preflight strict release gate-ში ჩაერთო; `-AllowDirty` diagnostic run image pinning/security preflight-ით PASS და unpinned image-ზე fail-closed behavior დადასტურებულია
- [x] canonical `ExportFormatRegistry` დაემატა და async export მას იყენებს; JSON/CSV/NDJSON media/extension mapping ერთიანია, unknown format fail-closed; targeted test და სრული `:api:test` PASS
- [x] latest technical acceptance replay (`build/technical-acceptance-report-latest.json`) — 29/29 checks PASS, მათ შორის host layout boundary, provider capability negotiation/lifecycle governance, data-family lifecycle conformance, semantic compatibility surface analysis, provider-neutral keyset tuple semantics, OpenAPI surface validation, runtime-ledger generation/verification, tamper resistance, portability matrix planner, supply-chain preflight, full API regression, synthetic provider replay და documentation consistency
- [x] legacy `dynamic/pages` fallback explicit `platform.legacy.dynamic-pages.enabled` flag-ზე გადავიდა; production default disabled, compatibility opt-in და `ApiApplicationTests` PASS
- [x] legacy fallback usage telemetry დაემატა დაბალი კარდინალობის Micrometer counter-ით (`geostat.legacy.dynamic_pages.fallback`); API test PASS
- [x] isolated observability stack-ში Prometheus durable metrics sink, scrape config და Grafana datasource provisioning დაემატა; local Docker CLI unavailable, production deployment intentionally not asserted
- [x] portability matrix planner დაემატა (`ops/cli/portability/portability-matrix.ps1`): SQL Server/MySQL, default/non-default schema, single/composite key და large-table profiles; unavailable endpoints fail-closed `NOT_RUN` report-ით
- [x] ერთიანი technical acceptance runner დაემატა (`ops/cli/validation/technical-acceptance.ps1`): release/OIDC/provider/Redis/OTel artefact checks, synthetic acceptance, სრული API regression და audit consistency; report PASS (`build/technical-acceptance-report.json`)
- [x] evidence-bundle generator-ის multi-file input და fail-closed validation დამტკიცდა smoke test-ით: ორი canonical evidence file + gate report შეიკრა, contract/snapshot identity ცალკე ჩაიწერა და bundle/sidecar SHA-256 შეიქმნა
- [x] secret-free OIDC policy artifact დაემატა (`infra/geostat-platform/keycloak/geostat-realm.json`): bearer-only API client, least-privilege roles და `tenant_id` claim mapper; Keycloak import opt-in-ად რჩება და production approval/negative evidence-ს არ ანაცვლებს
- [x] W-03 local contract-only proof ხელახლა შესრულდა: `ContractOnlyOnboardingTest` და `ContractOnlyDatabaseReplayTest` — `:api:test` BUILD SUCCESSFUL; production provider acceptance კვლავ ცალკე gate-ია
- [x] contract revision lifecycle persistence/orchestration დაემატა: `ContractLifecycleOrchestrator`, `JdbcContractLifecycleStateStore`, migration 084; restart-safe approve/supersede/rollback transitions, idempotency და JDBC regression PASS
- [x] provider capability discovery reload atomic გახდა და stale ACTIVE providers აღარ რჩება; `ProviderHealthSupervisor`-ის deterministic priority/failover და fail-closed no-candidate test PASS
- [ ] ყველა production ცვლილების ერთ release commit/tag-ში შეტანა
- [ ] clean checkout-იდან reproducible build/deploy-ის დამტკიცება
- [ ] production OIDC/JWT/RBAC/ABAC hardening-ის დასრულება
- [x] semantic compatibility analyzer-ის implementation: dataset/field/relation signature diff, breaking-change classification, approval gate და blocking regression tests; `ContractCompatibilityServiceTest` PASS
- [x] მეორე დამოუკიდებელი site/provider-ის contract-only acceptance — local
  synthetic contract-only proof PASS; რეალური external provider acceptance
  ცალკე production gate-ად რჩება
- [x] rate/quota/query-cost governance-ის implementation — calculator, bounded
  admission, Redis adapter და atomic counter implementation/test PASS; რეალური
  production binding/HA/fairness/load evidence ცალკე ღიაა
- [x] contract query execution-ში provider-neutral deterministic query-cost admission დაემატა (`QueryAdmissionBudget`): request shape-ის filters/select/group/include/order/where/limit/aggregation/distinct წონა, მათ შორის სრული nested predicate AST, overflow-safe ითვლება, configured ceiling-ზე fail-closed უარყოფა ხდება; `PLATFORM_QUERY_COST_MAX` .env/Compose/application/preflight contract-ში სავალდებულოდ ჩაიწერა; accepted/rejected გადაწყვეტილებები Micrometer counters-ად observable გახდა და პასუხში უსაფრთხო `X-Query-Cost`/`X-Query-Cost-Limit` headers ბრუნდება; targeted, სრული `:api:test` და production preflight PASS; per-tenant distributed quota/telemetry/load evidence კვლავ P1.4-ის ღია ნაწილია
- [ ] OpenTelemetry/SLO/error-budget production evidence
- [ ] სრული security, load, chaos, DR და supply-chain acceptance

**ჩეკლისტის წესი:** მონიშნულია მხოლოდ ის, რაც ამ აუდიტის ფარგლებში რეალურად შემოწმდა ან ჩამოყალიბდა. Implementation/production deployment პუნქტები შეგნებულად დაუმთავრებლად რჩება, სანამ მათი evidence არ შეიქმნება.

### მიმდინარე checklist reconciliation

2026-09-14-ის ბოლო შემოწმებით: **183 verified items**, **7 open items**. `QueryAdmissionBudget`-ის targeted test PASS-ით query-cost admission-ის implementation evidence და production runtime/Docker capability recheck დაემატა; GEOSOTAT shared-infrastructure compose/deploy contract და იზოლირებულ ქვეფოლდერში რეალური deployment დაემატა secrets fail-closed წესით; production authority input contract-მა ყველა გარე დამტკიცების ველი ერთ კანონიკურ ჩანაწერში მოაქცია; Redis distributed quota adapter-ის implementation და სრული `:api:test` PASS დაემატა; Micrometer OTLP metrics export-ის production-safe wiring და `:api:test` PASS დაემატა; Redis/OTLP enabled-mode endpoint validation დაემატა production preflight-ში და PASS-ით დადასტურდა; generic evidence-bundle generator დაემატა release-gate presence check-ით და multi-file smoke test-ით დადასტურდა; secret-free OIDC realm policy artifact დაემატა opt-in import-ით; W-03 local contract-only proof ხელახლა PASS-ით დადასტურდა; `git diff --check` PASS; rate-limit decision telemetry targeted PASS და evidence-bundle verifier checksum/immutability PASS; technical-acceptance runner PASS (20/20 checks); documentation zero-drift checker PASS; keyset tuple regression PASS; provider capability registry targeted PASS; migration 081 compile PASS; provider capability discovery targeted PASS; documentation cross-artifact checker PASS; semantic analyzer targeted PASS; semantic code-list/guidance test PASS; semantic golden surface-set suite PASS; TypeScript identifier sanitizer targeted PASS; unsupported export format fail-closed and full test PASS; lifecycle state-machine targeted PASS; contract lifecycle test PASS; stable sort metadata test PASS; contract checksum binding test PASS; generated runtime ledger and contract-marker extraction PASS; runtime ledger verifier hash/audit/marker verification PASS; runtime ledger tamper-resistance smoke PASS; strict release gate runtime-ledger binding diagnostic mode PASS; contract compatibility checksum/approval regression PASS; statistical query adapter boundary compile/test PASS; provider capability negotiation core mismatch rejection and targeted tests PASS; provider lifecycle state-machine targeted test PASS; family lifecycle conformance targeted and full test PASS; semantic compatibility surface analysis targeted and full test PASS; generated OpenAPI surface validator and malformed-document tests PASS; generated OpenAPI runtime validation binding PASS; supply-chain image pinning preflight PASS (`NOT_RUN` tools explicit); supply-chain preflight release-gate binding diagnostic mode PASS; export format registry/test PASS; latest technical acceptance replay 20/20 PASS; portability matrix planner NOT_RUN-safe; dynamic-pages fallback production-disabled flag and test PASS; dynamic-pages no-fallback regression test PASS; fallback usage telemetry counter and API test PASS; isolated Prometheus/Grafana provisioning added (deployment not asserted). 2026-09-14T06:57Z-ზე strict release gate-ის ხელახალი გაშვება განზრახ FAIL-და — working tree-ში **279 status entries** არის (`98db52a-dirty`), ამიტომ clean release commit/tag და reproducible deploy ჯერ არ დასტურდება. Open items-ის სტატუსი უცვლელია ქვემოთ მოცემულ P0/P1/P2 sections-ში.

### P0.1-ის ფაქტობრივი შედეგი

> Canonical path correction: the release gate referenced by this historical paragraph is now `ops/cli/validation/release-gate.ps1`; the older `scripts/release-gate.ps1` label is retained only as historical provenance.

დაემატა `scripts/release-gate.ps1`, რომელიც ამოწმებს აუცილებელ source/docs ფაილებს, secret-pattern-ს და repository provenance-ს. `-AllowDirty` diagnostic რეჟიმი PASS-ია; მკაცრი რეჟიმი განზრახ FAIL-დება, რადგან მიმდინარე working tree-ში 247 ცვლილებაა. ეს ნიშნავს, რომ პირველი defect-ის enforcement მექანიზმი მზადაა, მაგრამ release ჯერ ვერ ჩაითვლება clean/reproducible-ად — ცვლილებები უნდა დაკომიტდეს და clean checkout-იდან build/deploy უნდა გამეორდეს.

## 1. აღმასრულებელი დასკვნა

პროექტი აღარ არის უბრალო KIDS API ან Access importer. იგი უკვე არის **Contract-Driven Metadata API and Data-Product Platform**: კონტრაქტი აღწერს dataset/page/field/relation/projection/response-ს; ingest ქმნის provenance-ს; canonical ფენები გამოყოფს entity, raw, classification, statistics და სხვა data family-ებს; serving contract-ის მიხედვით ასრულებს query-ს.

KIDS R8 არის ძლიერი, production-ზე გატარებული reference vertical slice. დადასტურებულია approved site/ingestion contract, ingest, canonical materialization, quality/privacy/reconciliation gate-ები, publication, serving cache, rollback, replay, quarantine და backup/restore/DR. ეს მნიშვნელოვანი საფუძველია.

Remote runtime note (2026-09-13): `geostat-api` container Docker healthcheck-ით `healthy` და restart count `0` არის. Host-published canonical `/health` GET აბრუნებს `200` და `status=UP`-ს (`primary`, `dataPlane`, `archivePlane`, `objectStorage`, `secondary` ყველა `UP`). Root `/actuator/health` 404-ია, ხოლო `/api/v1/actuator/health` authenticated path-ია (401 anonymous-ზე); ამიტომ Actuator root path არ ჩაითვალა public health contract-ად.
Current image provenance note: remote `geostat-api` image labels contain no `org.opencontainers.image.revision`; the newly hardened deploy/Compose path is therefore not yet evidenced in the running container and requires the next clean release deployment.

OCI provenance note: live `geostat-api` image-ს `org.opencontainers.image.revision` label არ აქვს (მხოლოდ Compose/base-image labels ჩანს), ამიტომ ახალი provenance-enabled image-ის source commit alignment ჯერ არ დასტურდება.

Remote recheck (2026-09-14): `geostat-api` კვლავ healthy/restart=0 არის და host port `8083`-ზე canonical `/health` აბრუნებს ყველა plane-ს `UP`; live `api-api` image-ში revision label ისევ არ არსებობს. ეს არის განმეორებადი evidence, მაგრამ provenance/release gate-ის დახურვად არ ითვლება.

Remote source note: `/home/administrator/geostat/backend/api` deployment directory-ში `.git` metadata არ არსებობს (`NO_GIT_METADATA`), ამიტომ live source HEAD-ის local HEAD-თან პირდაპირი შედარება შეუძლებელია.
2026-09-14 read-only inventory-ით იმავე directory-ში მხოლოდ packaged `app.jar`, Dockerfile, Compose/env და versioned jars ჩანს; source checkout ან CI metadata არ არსებობს. ახალი revision-labeled image-ის build/deploy ამ evidence-ით ვერ დადასტურდა.

თუმცა სისტემა ჯერ **არ არის დასრულებული უნივერსალური enterprise platform**. მთავარი მიზეზი აღარ არის KIDS-ის მონაცემთა მოდელი. დარჩენილი რისკებია:

1. თანამედროვე engine-ის მნიშვნელოვანი ნაწილი Git-ში untracked/დაუკომიტებელია — reproducible release და source provenance დარღვეულია;
2. JWT/OIDC authorization hardening გამორთული ან scope-ის გარეთაა — production API დაცულად დასრულებული არაა;
3. აგნოსტიკურობა ერთი სრულად დამტკიცებული data-product-ით ვერ მტკიცდება — საჭიროა მეორე, არსებითად განსხვავებული site/provider acceptance;
4. semantic/schema compatibility, rate/quota/cost და tenancy isolation ბოლომდე ავტომატურად enforced არ არის;
5. observability, SLO/error budget, performance/chaos/security/supply-chain assurance არასრულადაა წარმოდგენილი;
6. რამდენიმე დოკუმენტი ისტორიულ target/TODO-ს აღწერს და მიმდინარე ledger-ს ეწინააღმდეგება.

**საბოლოო ვერდიქტი:** არქიტექტურული ბირთვი სწორია და KIDS-ის ხაზი ძლიერი reference implementation-ია; მაგრამ „უმაღლესი საერთაშორისო production standard“ სტატუსი მხოლოდ ქვემოთ მოცემული P0/P1 gate-ების დახურვის შემდეგ შეიძლება გამოცხადდეს.

## 2. მტკიცებულებების იერარქია

კონფლიქტისას სიმართლე ამ რიგით განისაზღვრება:

1. production runtime და immutable acceptance evidence;
2. applied database migration ledger და approved contract revision;
3. version-controlled source code და CI artifact provenance;
4. მიმდინარე status/acceptance დოკუმენტი;
5. design/plan დოკუმენტები;
6. legacy მაგალითები და ისტორიული draft-ები.

ამიტომ დოკუმენტში დაწერილი `IMPLEMENTED` საკმარისი არ არის, თუ შესაბამისი source commit, build, migration და acceptance evidence არ არსებობს.

## 3. მიმდინარე შესაძლებლობების შეფასება

| სფერო | მდგომარეობა | შეფასება | აუდიტური განმარტება |
|---|---|---:|---|
| Contract/metadata architecture | ძლიერი | 4/5 | versioned contract, page/dataset/field/relation/projection/response აღწერა არსებობს |
| KIDS end-to-end data product | production-proven | 4.5/5 | ingest-დან publication/cache/rollback/DR-მდე evidence არსებობს |
| Canonical data families | ძლიერი | 4/5 | raw/entity/classification/statistics/geo/relation საზღვრები გააზრებულია |
| Dynamic query/relation execution | კარგი, ნაწილობრივი | 3.5/5 | contract-bound filters, relation graph, includes და keyset არსებობს; ყველა adapter/provider ჯერ თანაბრად დამტკიცებული არაა |
| Statistical semantics/SDMX | კარგი | 3.5/5 | metric/unit/aggregation/dimension model არსებობს; სრული SDMX 3 interchange/provider coverage არა |
| API contract/interop | კარგი baseline | 3.5/5 | OpenAPI 3.1, JSON Schema, Problem Details, ETag, export; negotiation/SDK/formats ნაწილობრივი |
| Security/authorization | კრიტიკულად არასრული | 1.5/5 | JWT hardening scope-ის გარეთ/დროებით გაუქმებული; deny-by-default production posture ვერ დასტურდება |
| Rate/quota/query-cost governance | gap | 1/5 | დოკუმენტშიც TODO-ა; resource exhaustion-ის კონტროლი სრულად არაა |
| Multi-site/schema agnosticism | დაუმტკიცებელი | 2.5/5 | design generic-ია, მაგრამ დამოუკიდებელი მეორე site + განსხვავებული provider proof აკლია |
| Data governance/compatibility | ნაწილობრივი | 4/5 | registered contract-ის dataset/field/relation compatibility analyzer და blocking test მზადაა; cross-provider semantic registry/production evidence ჯერ ღიაა |
| Observability/SRE | ნაწილობრივი | 2.5/5 | logs/health არის; end-to-end OpenTelemetry, SLO, alerting და capacity evidence აკლია |
| Resilience/DR | კარგი reference proof | 3.5/5 | KIDS backup/restore/replay არსებობს; რეგულარული automated drills და RPO/RTO governance საჭიროა |
| Test assurance | ნაწილობრივი | 3/5 | unit/integration evidence არსებობს; provider matrix, property/fuzz, mutation, soak/chaos არასრულია |
| Delivery/release governance | კრიტიკული gap | 1.5/5 | ბევრი source/migration/doc untracked-ია; clean reproducible release ჯერ ვერ დგინდება |
| Documentation governance | ნაწილობრივი | 2.5/5 | სრული მასალა არსებობს, მაგრამ status drift და ისტორიული contradiction რჩება |

## 4. რაც არქიტექტურულად სწორადაა გადაწყვეტილი

### 4.1 Authority separation

- **Control Plane** აცხადებს და ამტკიცებს მნიშვნელობასა და შესრულების წესს;
- **Access package** არის data-bearing transport და არა ახალი authority;
- **Data Plane** ინახავს canonical/materialized მონაცემს;
- **Archive Plane** ინახავს უცვლელ artifact-ს, checksum-ს, provenance-სა და retention evidence-ს;
- **API** ასრულებს მხოლოდ approved contract-ს და caller-ს physical SQL თავისუფლებას არ აძლევს.

ეს საზღვარი იცავს კონტრაქტს შემთხვევითი Access column-ის, hardcoded KIDS branch-ისა და caller-supplied SQL-ისგან.

### 4.2 Data-family separation

`raw`, `entity`, `classification`, `statistics`, `geo` და `relation` სხვადასხვა სემანტიკური ოჯახებია. მათი ერთ generic JSON/EAV table-ში ძალით მოთავსება არ ხდება. აბსტრაქცია metadata/contract დონეზეა, storage კი family semantics-ს ინარჩუნებს.

### 4.3 Versioned publication

approved contract revision, immutable snapshot, gate evidence, serving pointer და rollback ქმნის სწორ publication boundary-ს. ცვლილება live table-ის ჩუმად გადაწერით არ უნდა გამოქვეყნდეს.

### 4.4 Contract-bound query

field/filter/relation/include/sort/aggregation წინასწარ გამოცხადებულია. identifier validation და parameterized predicates ამცირებს injection-სა და დაუმტკიცებელ access path-ს.

## 5. P0 — დაუყოვნებლივი release blockers

### P0.1 სუფთა და განმეორებადი release

**Gap:** თანამედროვე controllers/services/migrations/tests/docs-ის დიდი ნაწილი untracked-ია; working tree-ში ისტორიული `AD` artifact-ებიც ჩანს.

**საჭირო მექანიზმი:**

- ყველა production source, migration და test version control-ში;
- generated logs/PDF/render output `.gitignore`-ში ან artifact repository-ში;
- ერთი release commit/tag;
- CI-ში clean checkout → build → test → migration dry-run → image build;
- container image-ს commit SHA, dependency lock და immutable digest;
- deployed digest-ის audit ledger-ში ჩაწერა.

**Acceptance:** ცარიელი checkout-იდან ერთი ბრძანებით იგივე binaries/schema იქმნება; `git status --porcelain` release build-ის ბოლოს ცარიელია; runtime `/build-info` ემთხვევა tag/digest-ს.

### P0.2 production authentication და authorization

**Gap:** JWT/OIDC hardening დასრულებულად ვერ ჩაითვლება. public read endpoints-ის არსებობა შესაძლებელია, მაგრამ administrative, ingest, approve, publish და raw/confidential access anonymous არ უნდა იყოს.

**საჭირო მექანიზმი:**

- OIDC/OAuth 2.0 resource server;
- asymmetric token validation, issuer/audience/expiry/nonce/key rotation;
- scopes: `contract.read`, `contract.write`, `ingest.execute`, `quality.approve`, `publish.execute`, `raw.read`, `admin`;
- RBAC + resource-level ABAC: tenant/site/product/dataset/classification/confidentiality;
- deny-by-default და field/row-level policy enforcement;
- service-to-service mTLS ან sender-constrained credential;
- break-glass account-ის კონტროლირებული audit.

**Acceptance:** negative authorization suite ამოწმებს ყველა endpoint/object/property-ს; revoked/expired/wrong-audience token უარყოფილია; cross-tenant access შეუძლებელია; public endpoint მხოლოდ explicit policy-ითაა public.

### P0.3 publication invariant-ის ერთიანი enforcement

**Gap:** დოკუმენტურად gate-ები არსებობს, მაგრამ invariant ყველა publish path-ზე ერთ ცენტრალურ policy decision-ად უნდა იყოს გარანტირებული.

**საჭირო წესი:** publication დასაშვებია მხოლოდ მაშინ, როცა site contract და ingestion contract ერთი compatible APPROVED revision-ია, artifact immutable-ია, ყველა სავალდებულო gate PASS-ია, approval segregation დაცულია და target snapshot ჯერ არ გამოქვეყნებულა.

**Acceptance:** API, job, script და direct worker ყველა ერთ policy service/store procedure-ს იყენებს; negative test თითოეულ prerequisite-ს ცალ-ცალკე არღვევს და publish ყოველთვის იბლოკება.

### P0.4 secrets და private artifacts

**Gap:** credentials/configuration-ის უსაფრთხო provenance აუდიტით სრულად არ დასტურდება; repository-ში build/log artifacts მრავლადაა.

**საჭირო მექანიზმი:** secrets manager/KMS, მოკლევადიანი credentials, rotation, secret scanning, private object-storage bucket policy, server-side encryption, object lock სადაც საჭიროა, signed URL-ის მცირე TTL.

**Acceptance:** repository/history/image/log-ში secret არ იძებნება; rotation test და denied anonymous object access PASS-ია.

## 6. P1 — უნივერსალური პლატფორმის აუცილებელი შესაძლებლობები

### P1.1 მეორე დამოუკიდებელი data-product proof

KIDS-ზე წარმატება engine-ის აგნოსტიკურობას მარტო ვერ ამტკიცებს. საჭიროა deliberately განსხვავებული reference product:

- სხვა table/field სახელები და სხვა hierarchy;
- composite business key;
- მინიმუმ entity + relation + classification და სასურველია geo ან raw-only family;
- სხვა physical schema ან DB provider;
- onboarding მხოლოდ contract/mapping/adapter configuration-ით, core code-ის ცვლილების გარეშე.

**Acceptance:** `git diff` engine packages-ში ცარიელია; მხოლოდ contract/seed/config/plugin ემატება; იგივე introspection/query/export/publication suite გადის.

### P1.2 semantic and schema compatibility analyzer

**Gap:** revision approval-მდე ყველა breaking/semantic ცვლილება ერთიანი analyzer-ით არ ფასდება.

**Analyzer-მა უნდა შეამოწმოს:**

- field add/remove/rename/type/nullability/default;
- key/index/relation cardinality და cascade behavior;
- classifier version/hierarchy/code retirement;
- metric/unit/scale/aggregation/dimension compatibility;
- filter/operator/sort/include/response shape ცვლილება;
- privacy classification, retention და quality threshold;
- page/dataset/source/projection binding;
- backward/forward/full compatibility policy.

**Acceptance:** analyzer ქმნის machine-readable diff-ს, severity-ს, migration plan-სა და blocking reason-ს; breaking revision approval migration/consumer acknowledgement-ის გარეშე შეუძლებელია.

### P1.3 policy-as-code

Quality, privacy, publication, retention, tenancy და query-cost წესები scattered `if`-ებად არ უნდა დარჩეს.

**მექანიზმი:** versioned policy registry, typed inputs/outputs, deterministic decision log, policy test fixtures, approval workflow და fail-closed behavior.

**Acceptance:** ერთი policy decision შეიძლება audit correlation ID-ით აღდგეს; policy version snapshot-ს ებმის; შეცდომისას privileged operation უარყოფილია.

### P1.4 rate, quota და query-cost governance

**საჭიროა:**

- per-client/tenant/product token-bucket ან equivalent rate limit;
- response-ში სტანდარტული rate-limit information;
- AST-based query complexity: depth, fan-out, joins, filters, group cardinality, estimated rows/bytes;
- endpoint timeout, concurrency, export size, page size და include limits;
- admission control, cancellation და backpressure;
- noisy-neighbor isolation.

**Acceptance:** overload test-ზე latency bounded რჩება, excess request deterministic `429`/problem response-ს იღებს, მძიმე query execution-მდე იბლოკება და quota usage observable-ია.

### P1.5 tenancy და data isolation

**საჭიროა:** tenant/site/product ownership-ის explicit მოდელი; ყველა resource key-ში tenant scope; database row-level security ან equally strong repository policy; tenant-specific encryption/retention/quota; cache key და cursor scope-ში tenant identity.

**Acceptance:** cross-tenant fuzz/negative suite; cache/cursor/export/object pointer leakage ნულია.

### P1.6 observability და SRE

**საჭიროა:** OpenTelemetry trace/metric/log correlation; ingest batch, contract revision, snapshot, page, query fingerprint და tenant attributes; RED metrics API-ზე, stage duration/error ingest-ზე; queue lag, cache hit, gate failure, reconciliation drift; dashboards, alerts, SLO და error budget.

**Acceptance:** ერთი request-იდან source query/cache/export-მდე trace ჩანს; ერთი ingest batch-ის სრული critical path ჩანს; SLO breach alert test მუშაობს; sensitive values telemetry-ში redacted-ია.

### P1.7 automated reconciliation and evidence

ყოველ release/snapshot-ზე ავტომატურად უნდა შეიქმნას signed evidence bundle:

- source/artifact checksum და row count;
- accepted/rejected/quarantined count;
- FK/key/index/relation integrity;
- classifier/metric/unit/dimension validity;
- privacy/quality policy results;
- materialized/cache count და checksum;
- source commit/image/migration/contract/snapshot identity;
- approver და timestamps.

**Acceptance:** bundle immutable-ია და მისგან audit replay შესაძლებელია.

## 7. P2 — თანამედროვე API/data-product maturity

### P2.1 სრული HTTP/API lifecycle

- strict `Accept`, `Content-Type`, `Accept-Language` და profile/version negotiation;
- idempotency key mutation/ingest endpoints-ზე;
- optimistic concurrency (`ETag`/`If-Match`);
- deprecation და `Sunset` policy;
- async operation contract: submit/status/cancel/result/expiry;
- webhooks ან SSE long-running completion/data-product events-ისთვის;
- consistent RFC 9457 problem taxonomy და correlation ID;
- generated OpenAPI/JSON Schema, executable examples და contract tests.

### P2.2 format/provider plugin architecture

Provider SPI უნდა ფარავდეს JSON, CSV, NDJSON, SDMX-JSON/CSV/XML, Parquet/ZIP export-ს და მომავალ source adapters-ს. Provider აცხადებს capabilities-ს; engine აკეთებს negotiation-ს და არ შეიცავს format-specific branching-ს.

**Acceptance:** provider ჩანაცვლება registry/config-ით ხდება; unsupported capability deterministic `406/422`-ით ბრუნდება; golden-file interoperability tests არსებობს.

### P2.3 catalog და lineage interoperability

Internal metadata-ს უნდა ჰქონდეს DCAT 3 projection dataset/data-service discovery-სთვის და W3C PROV-compatible lineage export. ეს internal schema-ს RDF-ით ჩანაცვლება არ არის — ეს interoperability projection-ია.

### P2.4 data correction და temporal semantics

საჭიროა valid-time/transaction-time ან მკაფიო revision policy, late-arriving observations, correction/supersession, tombstone, classifier effective period და reproducible “as-of snapshot” query.

### P2.5 developer experience

- generated TypeScript/Java/Kotlin/Dart clients;
- sandbox და fixture contracts;
- CLI: validate/diff/compile/test/publish;
- consumer-driven contract tests;
- searchable catalog, examples და copy-paste recipes;
- error remediation guidance.

## 8. P3 — მასშტაბი და advanced capabilities

ეს შესაძლებლობები საჭიროა მაშინ, როცა workload ამას ამართლებს; მათი ნაადრევი დამატება ზედმეტი სირთულე იქნება:

- CDC/streaming ingestion და event-time watermark;
- transactional outbox + CloudEvents-compatible envelope, idempotent consumer, DLQ/replay;
- distributed cache invalidation;
- partitioning/sharding და workload-aware routing;
- query planner/cardinality statistics/materialized projection selection;
- multi-region read serving და failover;
- columnar analytical serving;
- privacy-enhancing outputs: suppression, rounding, k-anonymity/differential privacy — მხოლოდ დამტკიცებული business policy-ით.

## 9. უსაფრთხო software supply chain

აუცილებელია:

- SBOM თითო image/release-ზე;
- dependency/container/IaC/secret scanning;
- pinned dependencies და reproducible builds;
- signed image/artifact და provenance attestation;
- protected branch, mandatory review და CI gates;
- threat model და abuse-case tests;
- patch/vulnerability SLA.

**Acceptance:** უცნობი ან დაუმოწმებელი artifact production-ში ვერ განთავსდება; critical vulnerability release-ს ბლოკავს; provenance deployment-მდე მოწმდება.

## 10. ტესტირების დაკარგული სიღრმე

არსებულ unit/integration/acceptance ტესტებს უნდა დაემატოს:

1. contract compiler property-based და fuzz tests;
2. relation graph cycle/depth/fan-out tests;
3. identifier/injection/authorization abuse tests;
4. schema migration backward/forward compatibility tests;
5. SQL Server + მინიმუმ ერთი განსხვავებული provider matrix;
6. concurrency tests: publish race, replay race, keyset traversal concurrent inserts/updates;
7. performance baseline, load, soak და capacity envelope;
8. failure injection: DB/object store/queue/network timeout;
9. backup restore drill და measured RPO/RTO;
10. mutation testing critical policy/compiler code-ზე;
11. golden response tests ყველა data family/format/profile-ზე;
12. accessibility/UI tests — როცა Control Plane UI აშენდება.

## 11. სამიზნე არქიტექტურის სრული ხაზი

### 11.1 სისტემის canonical identity და პასუხისმგებლობების საზღვრები

GEOSOTAT არის **contract-driven, metadata-driven, schema-agnostic, multi-site data-product platform**. მისი საწყისი წერტილი არის Control Plane-ში დამტკიცებული, versioned და immutable contract revision; ქვედა ფენები ამ revision-ს ასრულებენ და საკუთარ ბიზნეს-ლოგიკას კონტრაქტის გვერდის ავლით არ ამატებენ.

ფენების კანონიკური პასუხისმგებლობა:

1. **Control Plane** — კატალოგი, contract registry, dataset/field/relation/projection metadata, compatibility analysis, identity/tenancy policy, workflow/approval და operation registry. აქ იქმნება და მტკიცდება საიტის კონტრაქტი.
2. **Ingestion Plane** — artifact-ის მიღება, fingerprinting, staging, validation, quarantine, mapping, materialization, reconciliation და evidence generation. აქ წყარო კონტრაქტის მიხედვით გარდაიქმნება canonical მონაცემად.
3. **Data Plane** — raw, entity, classification, statistics, geo, relation და snapshot/materialization მონაცემების შენახვა. ეს არის მონაცემთა canonical runtime store და არა კონტრაქტის ავტორი.
4. **Archive Plane** — უცვლელი source artifact, checksum, provenance, retention, confidentiality და object-lock/restore მტკიცებულება. Archive-ში ინახება წყაროს სიმართლე, არა executable logic.
5. **Serving Plane** — contract compiler, authorization/policy enforcement, relation-graph execution, provider/family adapters, filter/aggregation/projection execution, cache, export და API/event delivery. Response კონტრაქტიდან დინამიკურად გენერირდება.

სისტემის ძირითადი მიმართულებაა: **approved contract → ingestion/materialization → approved snapshot → governed serving response**. Access artifact არის ingest-ის წყარო/ტრანსპორტი; ის არ არის კონტრაქტის ავტორი და არ უნდა შეიცავდეს credentials-ს, executable logic-ს ან publication authority-ს.

სავალდებულო invariants:

- ყველა layer-ს აქვს მკაფიო owner, version და audit trail;
- schema/provider/site ცვლილება contract revision-ით იმართება და generic engine-ში hardcoded branch არ ემატება;
- publication, deployment და runtime activation მხოლოდ შესაბამისი approved/verified evidence-ის შემდეგ ხდება;
- authorization, tenancy, privacy, quota, observability, rollback და idempotency cross-cutting კონტროლებია;
- production-complete ნიშნავს ერთდროულად reproducible release-ს, negative security suite-ს, დამოუკიდებელი provider-ის acceptance-ს, overload/telemetry evidence-ს, immutable reconciliation bundle-ს და measured DR/rollback შედეგებს;
- ნებისმიერი დაუმტკიცებელი external value (issuer, tenant policy, RPO/RTO, alert route, deploy authority ან test window) fail-closed მდგომარეობაში რჩება და დოკუმენტურად არ ითვლება შესრულებულად.

```text
Authors / Stewards / Operators
              │
              ▼
┌─────────────────────────────────────────────────────────────┐
│ CONTROL PLANE                                               │
│ catalog ─ contract registry ─ compatibility ─ policy        │
│ workflow/approval ─ identity/tenancy ─ operation registry   │
└──────────────────────────┬──────────────────────────────────┘
                           │ approved immutable revision
                           ▼
┌─────────────────────────────────────────────────────────────┐
│ INGESTION PLANE                                             │
│ receive ─ fingerprint ─ stage ─ validate ─ quarantine       │
│ map ─ materialize ─ reconcile ─ evidence                    │
└───────────────┬───────────────────────────────┬─────────────┘
                │                               │
                ▼                               ▼
┌──────────────────────────────┐  ┌───────────────────────────┐
│ DATA PLANE                   │  │ ARCHIVE PLANE             │
│ raw/entity/classification    │  │ immutable artifact        │
│ statistics/geo/relation      │  │ checksum/provenance       │
│ snapshot/materialization     │  │ retention/object lock     │
└───────────────┬──────────────┘  └───────────────────────────┘
                │ approved snapshot
                ▼
┌─────────────────────────────────────────────────────────────┐
│ SERVING PLANE                                               │
│ contract compiler ─ policy ─ relation graph ─ query planner │
│ family/provider adapters ─ cache ─ export ─ API/events      │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
                    Consumers / Sites

Cross-cutting: OIDC/ABAC · OpenTelemetry · SLO · audit ·
supply-chain provenance · backup/DR · rate/quota/cost
```

## 12. შესრულების სწორი რიგი

### ეტაპი A — სიმართლისა და უსაფრთხოების აღდგენა

1. repository inventory და generated/source კლასიფიკაცია;
2. ყველა production ცვლილების commit; stale artifacts-ის quarantine/ignore;
3. clean CI build და immutable release provenance;
4. OIDC/JWT + RBAC/ABAC + deny-by-default;
5. secret rotation/scanning;
6. centralized publication policy invariant.

### ეტაპი B — უნივერსალურობის მტკიცება

7. compatibility analyzer;
8. მეორე განსხვავებული data-product contract-only onboarding;
9. მეორე DB/provider test matrix;
10. tenant isolation და cross-tenant negative suite;
11. provider capability registry.

### ეტაპი C — production control

12. rate/quota/query-cost admission control;
13. OpenTelemetry + SLO/alerts;
14. signed reconciliation/evidence bundle;
15. load/soak/failure/DR automation.

### ეტაპი D — interoperability და developer platform

16. strict negotiation და format providers;
17. DCAT/PROV/სრული SDMX projections;
18. SDK/CLI/sandbox/consumer contract tests;
19. API lifecycle/deprecation/event delivery;
20. documentation-as-code drift gate.

## 13. „დასრულებულია“ როდის შეიძლება ითქვას

პლატფორმა production-complete ჩაითვლება მხოლოდ თუ ერთდროულად სრულდება:

- clean tagged source-იდან reproducible deploy;
- authentication/authorization/tenancy negative suite PASS;
- KIDS და მეორე განსხვავებული site core-code ცვლილების გარეშე მუშაობს;
- compatibility analyzer breaking change-ს ბლოკავს;
- rate/quota/cost overload test PASS;
- trace/SLO/alert evidence არსებობს;
- reconciliation/lineage/approval bundle immutable-ია;
- rollback, replay, backup/restore და measured RPO/RTO PASS;
- API/provider interoperability matrix PASS;
- documentation current runtime/migration ledger-იდან ავტომატურად გენერირდება;
- არცერთი production capability მხოლოდ დოკუმენტურ განცხადებად არ რჩება.

## 14. რა არ უნდა გაკეთდეს

- ახალი universal EAV/JSON table, რომელიც ყველა family semantics-ს შლის;
- caller-supplied SQL/table/column/expression;
- KIDS-specific branch generic engine-ში;
- Access package-ში credentials, executable logic ან publication authority;
- approval-ის გარეშე auto-publish;
- framework-ისთვის microservice-ებად ხელოვნური დაშლა workload-ის მტკიცებულების გარეშე;
- „ყველა შესაძლო სტანდარტის“ ერთდროულად დანერგვა რეალური use case/acceptance-ის გარეშე.

სიძლიერე მოდის მკაფიო invariants-იდან, ავტომატური enforcement-იდან და მტკიცებულებიდან — არა მხოლოდ abstraction-ების რაოდენობიდან.

## 15. გამოყენებული შიდა მტკიცებულებები

- `docs/KIDS-R8-current-status-and-acceptance.md`
- `docs/api-modernization-capability-gap.md`
- `docs/api-contract-execution-audit.md`
- `docs/final-physical-database-architecture.md`
- `docs/final-unified-physical-virtual-contract.md`
- `docs/international-standards-reference-architecture.md`
- `docs/three-database-physical-dictionary.md`
- API controllers/services/tests, DB migrations და მიმდინარე Git working tree

## 16. გარე ნორმატიული ორიენტირები

- [OpenAPI Specification 3.1/3.2](https://spec.openapis.org/oas/) და JSON Schema dialect;
- [IETF RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html) — HTTP Problem Details;
- [IETF RFC 9333](https://www.rfc-editor.org/rfc/rfc9333.html) — RateLimit fields;
- [IETF RFC 9700](https://www.rfc-editor.org/rfc/rfc9700.html) — OAuth 2.0 Security Best Current Practice;
- [OWASP API Security Top 10 (2023)](https://api-security.owasp.org/editions/2023/en/0x11-t10/);
- [OpenTelemetry](https://opentelemetry.io/docs/specs/) specifications და semantic conventions;
- [W3C DCAT 3](https://www.w3.org/TR/vocab-dcat-3/) და [PROV-O](https://www.w3.org/TR/prov-o/);
- [SDMX 3.0](https://sdmx.org/standards-2/);
- [NIST Secure Software Development Framework](https://csrc.nist.gov/pubs/sp/800/218/final) (SSDF).

## 17. მიმდინარე დარჩენილი სამუშაო — canonical summary (2026-09-14)

მიმდინარე აუდიტით დარჩენილია 7 ძირითადი პუნქტი:

1. **Release provenance**
   - ყველა ცვლილების ერთ approved commit/tag-ში გაერთიანება;
   - clean checkout-იდან reproducible build/deploy;
   - image digest და commit SHA-ს runtime evidence;
   - `git status`-ის სუფთა მდგომარეობა release-ის ბოლოს.

2. **Production security**
   - OIDC issuer/JWKS-ის რეალური კონფიგურაცია;
   - asymmetric token validation, issuer/audience/expiry/key rotation;
   - RBAC + ABAC;
   - tenant/site/product/dataset/field-level isolation;
   - deny-by-default და negative authorization suite;
   - service-to-service authentication.

3. **მეორე დამოუკიდებელი provider/site**
   - სხვა schema/table names;
   - composite key;
   - entity + relation + classifier (+ სხვა family);
   - სხვა DB/provider;
   - contract-only onboarding core-code ცვლილების გარეშე;
   - სრული introspection/query/export/publication acceptance.

4. **Rate/quota/query-cost governance**
   უკვე გაკეთებულია:
   - query-cost calculator/admission;
   - Redis distributed adapter;
   - atomic Lua counter;
   - bounded filters, fan-out, depth, export და request limits.

   დარჩენილია:
   - API-ს production Redis-ზე რეალური binding;
   - per-tenant quota;
   - HA/failover;
   - fairness/noisy-neighbor isolation;
   - overload/load evidence;
   - usage telemetry.

5. **OpenTelemetry/SLO**
   უკვე გაკეთებულია:
   - OTel Collector ინფრასტრუქტურა;
   - API-ს OTLP metrics wiring.

   დარჩენილია:
   - durable telemetry backend;
   - traces + logs correlation;
   - dashboards;
   - SLO/error budget;
   - alert destinations;
   - alert firing test;
   - sensitive-field redaction evidence.

6. **Reconciliation/evidence და publication assurance**
   - signed immutable evidence bundle;
   - source/artifact checksum;
   - row/FK/relation reconciliation;
   - quality/privacy/semantic checks;
   - snapshot/cache checksum;
   - rollback/replay evidence.

7. **სრული production acceptance**
   - security regression;
   - load/soak/capacity;
   - chaos/failure;
   - backup/restore;
   - DR replay და measured RPO/RTO;
   - supply-chain/security scan;
   - approved release/deploy authority და test window.

დადასტურებული მიმდინარე შედეგია **147 verified / 7 open** (`scripts/audit-checklist-count.ps1`-ის PASS).  
ანუ არქიტექტურული და ტექნიკური საფუძველი მნიშვნელოვნად გაკეთებულია, მაგრამ production-complete სტატუსამდე აკლია რეალური გარემოს დამტკიცებული პარამეტრები და მათი დამოუკიდებელი evidence.

## 17A. 2026-09-14 continuation evidence

Production-only blockers და საჭირო authority inputs ერთ ფაილშია თავმოყრილი: [`production-blockers-and-required-authority.md`](production-blockers-and-required-authority.md).

- `PlatformRateLimitFilterTest` და სრული `:api:test` კვლავ PASS-ია rate-limit telemetry wiring-ის შემდეგ.
- `ops/cli/data/verify-evidence-bundle.ps1` generated bundle-ზე checksum/schema/referenced-file/read-only checks-ით PASS-ია.
- remote host `192.168.1.199`-ზე GEOSOTAT-ის Redis, Keycloak, Keycloak DB, OTel Collector, API და MinIO containers არიან გაშვებული; co-hosted stacks არ შეხებია.
- remote `geostat-api` container-ის non-secret environment inspection-ში მხოლოდ legacy `JWT_*` bindings გამოჩნდა; OIDC/JWKS, tenant policy, Redis quota და OTLP API binding-ის production evidence არ არსებობს.
- strict release gate კვლავ fail-closed-ად მუშაობს: working tree dirtyა და release tag/clean reproducible deploy ვერ დასტურდება.

ეს არის evidence boundary და არა production approval; დაუმტკიცებელი შვიდი gate არ უნდა მოინიშნოს `[x]`-ად.

## 18. დარჩენილი სამუშაოს execution backlog

ქვემოთ მოცემული შვიდი workstream ამ audit-ის მოქმედ სამუშაოდ ითვლება. თითოეული იხურება მხოლოდ implementation + automated test + production evidence + independent review-ის ოთხივე პირობის დაკმაყოფილებისას.

**Goal scope decision (2026-09-14):** ეს შვიდი workstream არის მიმდინარე goal-ის სრული და მოქმედი scope. წინა ჩანაწერებში არსებული დუბლირებული/ძველი task-ები ამ backlog-ით ჩანაცვლებულია; ახალი სამუშაო ერთეულები მხოლოდ W-01–W-07-ის შიგნით დაემატება.

| ID | Workstream | შესრულებული ნაწილი | დარჩენილი ნაწილი | დახურვის მტკიცებულება | მიმდინარე მდგომარეობა |
|---|---|---|---|---|---|
| W-01 | Release provenance | source inventory; release-gate; evidence generator/verifier; OCI revision check | approved signed tag/commit; clean checkout build/deploy; deploy ledger | clean checkout replay, OCI revision match, signed release record | OPEN — დარჩა 4 ბლოკი |
| W-02 | OIDC/RBAC/ABAC/tenancy | Keycloak realm policy; fail-closed JWT საფუძველი; technical policy artefact | რეალური issuer/JWKS binding; asymmetric validation; RBAC/ABAC; tenant enforcement; negative suite | expired/wrong-audience/revoked/cross-tenant negative suite | OPEN — დარჩა 5 ბლოკი |
| W-03 | Independent provider proof | local second-provider contract-only tests; database replay test; core-code independence check | რეალური დამოუკიდებელი DB/provider; განსხვავებული schema; composite key/relation/classifier publish replay | core-code diff empty, full contract-only acceptance report | OPEN — დარჩა production acceptance |
| W-04 | Distributed quota and cost | query-cost admission; Redis adapter; atomic Lua counter; rate-limit telemetry; bounded limits | production Redis binding; per-tenant budgets; HA/failover; fairness/noisy-neighbor; overload/soak evidence | shared-state evidence, 429/cost reports, load/soak results | IMPLEMENTATION PARTIAL; PRODUCTION OPEN — დარჩა 6 ბლოკი |
| W-05 | Observability/SLO | OTel Collector; OTLP metrics wiring; telemetry redaction hooks | durable backend; traces/log correlation; dashboards; SLO/error budget; alert routes; firing test | trace path, redaction, alert firing and SLO evidence | IMPLEMENTATION PARTIAL; PRODUCTION OPEN — დარჩა 6 ბლოკი |
| W-06 | Reconciliation/DR/publication | evidence bundle generator/verifier; checksum and immutable metadata checks | real backup target; timed restore; replay; rollback; measured RPO/RTO; publication evidence | immutable bundle, timed restore/replay and publication rollback report | OPEN — დარჩა 6 ბლოკი |
| W-07 | Full security and resilience acceptance | technical acceptance runner; API regression; synthetic checks; release/evidence tooling | supply-chain scan; approved test window; real security/load/chaos/DR; abort/rollback; signed reports | approved test window, signed reports, no unresolved critical findings | OPEN — დარჩა 6 ბლოკი |

### Execution order and dependency rule

`W-01 → W-02/W-04/W-05 → W-03 → W-06 → W-07`.

W-02, W-04 და W-05-ის runtime activation აკრძალულია შესაბამისი authority record-ის `VERIFIED` სტატუსის გარეშე. W-06-ის publication/rollback evidence ვერ ჩაითვლება W-01-ის immutable release identity-ის გარეშე. W-07 მხოლოდ W-02–W-06-ის წინაპირობების დაკმაყოფილების შემდეგ იძლევა საბოლოო production verdict-ს.

### Acceptance reconciliation — 2026-09-14

- [x] Full API regression after Access fixture path and duplicate YAML-root correction — `BUILD SUCCESSFUL`.
- [x] Technical acceptance replay — latest runner **29/29 checks PASS**.
- [x] OIDC runtime preflight added and bound to technical acceptance; it
  fail-closes on non-HTTPS issuer/JWKS URLs and validates JWKS key structure,
  while reporting `NOT_RUN` when operator endpoints are intentionally absent.
- [x] GeoStat runtime health — primary/data/archive/secondary DB, object storage and API `UP`.
- [x] Redis authenticated staging ping — `PONG` (secret excluded from evidence).
- [x] KIDS R8 ingest/materialization, reconciliation, rollback/replay and checksum-verified backup/DR — confirmed by the current R8 acceptance record.
- [x] Immutable staging evidence bundle generated and verified from technical
  acceptance, remote read-only replay, secret-injection preflight and readiness
  reports (`build/geostat-staging-evidence-bundle-r31.json`; SHA-256
  `9c4debcf0fd616325a46090c79d54f74020cbe368d62523c2b662daf0703f1ad`).
- [ ] Protected API query/export replay with an approved JWT.
- [ ] SQL Server/MySQL portability execution with approved endpoints.
- [ ] Durable telemetry/alert firing and new publication evidence bundle.
- [ ] Signed production authority, steward approval and approved test window.

### Introspection semantic capability closure — 2026-09-14

- [x] `allowedAggregations` is now derived from the approved metric registry for
  the requested dataset, with only universal `COUNT` added by the query model.
- [x] Generic execution is free of family-specific branching: serving,
  aggregation, totals and filter execution are delegated from
  `CanonicalPageDataService` to `ContractPageExecutionRegistry` and
  `ContractPhysicalQueryService`; no family-specific SQL or physical table
  names remain in the orchestration service. Compile, synthetic provider
  acceptance and full technical acceptance pass.
- [x] The serving dispatch boundary now uses `PageDataAdapterRegistry` selected
  from contract-declared `data_family`, with an explicit UNKNOWN fallback to
  the contract physical-query adapter; compile, API regression and technical
  acceptance pass. Family handler extraction and semantic hardcoding removal
  remain tracked by the open item above.
- [x] Full API regression after the change: `BUILD SUCCESSFUL`.
- [ ] Production OIDC/provider/telemetry/DR/authority gates remain open and are
  not substituted by this local implementation evidence.

- [x] Query-plan aggregation validation is registry-driven and revision-scoped;
  hardcoded operation whitelist removed; full API regression `BUILD SUCCESSFUL`.
- [x] Keyset read path is family-neutral: statistical `nodeKind` rejection was
  removed; stable-key/profile enforcement remains contract/adapter-owned; full
  API regression `BUILD SUCCESSFUL`.
- [x] Page response relations are revision-scoped contract metadata; the
  hardcoded KIDS relation list was removed and full API regression passed.
- [x] Keyset cursor resume now runs the approved contract compiler before
  cursor/data-plane execution; full API regression passed.
- [x] `orderBy` fields and direction are validated against declared contract
  fields/aliases (`ASC`/`DESC` only) before execution; full API regression passed.
- [x] `include`/`includeLimits` are now restricted to revision-scoped declared
  relation codes; undeclared implicit includes fail closed; full API regression passed.
- [x] Read-only remote staging smoke harness added and executed: five GEOSOTAT
  containers PASS, health HTTP 200, protected endpoint HTTP 401; report saved to
  `build/remote-staging-smoke.json`.
- [x] remote staging smoke harness is now a required release/technical
  acceptance artefact (execution remains explicitly read-only and external
  production approval is not inferred).
- [x] Keyset cursor signed revision/snapshot claim is now checked against the
  current approved page contract before data-plane execution; stale cursors fail
  closed; full API regression passed.
- [x] Canonical page, SDMX and async export controllers no longer hardcode a
  KIDS contract default; omitted contract resolves from approved governance
  state and fails closed if none exists; full API regression passed.
- [x] Access ingestion no longer infers KIDS table prefixes from dataset names;
  source locator is the sole contract authority. Schema-agnostic runtime
  preflight scanned 188 Java files with zero violations; full API regression passed.
- [x] schema-agnostic runtime preflight is executed as a mandatory technical
  acceptance check; latest technical acceptance is **29/29 PASS**.
- [x] Introspection aggregation capabilities are revision-scoped through the
  site contract dataset binding; full API regression passed.
- [x] Remote smoke harness no longer embeds a KIDS contract literal; protected
  probe contract is explicit input. Explicit KIDS staging run: **7/7 PASS**.
- [x] SDMX codelist endpoint now discovers the active reference page from the
  approved contract instead of hardcoded page 12; full API regression passed.
- [x] SDMX key parsing now maps segments to approved DSD dimensions by order;
  hardcoded `AGE_GROUP` filter removed and invalid undeclared keys fail closed;
  full API regression passed.
- [x] Transactional-outbox polling is now explicitly feature-gated
  (`platform.outbox.enabled`, default enabled for production); the application
  context fixture disables it together with external schedulers, preventing
  test-only placeholder databases from generating retry noise while preserving
  fail-closed production behavior; full API regression `BUILD SUCCESSFUL`.
- [x] Spring context test configuration now imports a credential-free
  test-only resource (`application-test.yml`) instead of the developer-local
  override, disables scheduling side effects, and supplies explicit test
  datasource/dialect values; context regression `BUILD SUCCESSFUL`.
- [x] Removed credential-bearing `api/src/main/resources/application-local.yml`
  from the production resource set; replaced it with a placeholder-only
  `ops/config/templates/geostat/application-local.yml.example`. BootJar now
  has deterministic duplicate-entry handling, packages successfully, and a
  JAR inventory scan confirms no `application-local` resource is embedded.
- [x] Removed the parallel credential-bearing mobile local override and two
  recovered session dumps containing historical DB/S3 secrets; repository-wide
  non-build secret scan now finds only explicit `CHANGE_ME` placeholders and
  environment-variable references.
- [x] Legacy frontend web `application.yml` no longer contains a hardcoded JWT
  secret or DB endpoint/credential; it uses environment references and
  production-safe `ddl-auto`/logging defaults.
- [x] Added `secret-boundary-preflight.ps1` and bound it to technical
  acceptance; it scans Geostat runtime/config scope for literal credentials or
  private endpoints and passes with only placeholders/environment references.
  Deployment secret files were removed from the repository and are now supplied
  externally.
- [x] Production-evidence readiness now includes the secret-boundary result as
  a first-class check (`PASS`), while release authority, OIDC, provider,
  telemetry, DR and resilience statuses remain fail-closed and explicit.
- [x] Release-gate Git hygiene handling is robust to non-fatal CRLF advice:
  `git diff --check` exit status remains authoritative, while harmless stderr
  warnings no longer create false gate failures; `release-gate.ps1 -AllowDirty`
  diagnostic run PASS.
- [x] Secret-boundary preflight now rejects any repository-local
  `ops/config/projects/geostat/shared/secrets/*.env` file by location, in
  addition to scanning literal values; current scan is `PASS` with no secret
  files present.
- [x] Preflight, bootstrap and deploy scripts use the operator-supplied
  `GEOSTAT_ENV_FILE` injection boundary (or an untracked runtime fallback),
  and compose development follows the same path; deleted repository secret
  files are no longer referenced by executable deployment paths.
- [x] Deployment-secret-injection preflight covers every Geostat deploy,
  bootstrap, preflight and compose entrypoint; it passes and rejects legacy
  repository secret paths by construction.
- [x] Read-only staging replay against `192.168.1.199` passed for all five
  GEOSOTAT containers, API health (`200`) and protected KIDS contract endpoint
  without credentials (`401`) and with a malformed bearer token (`401`); report is stored in
  `build/remote-staging-smoke-latest.json`.
- [x] Remote infrastructure readiness replay passed: authenticated Redis
  `PONG` and Keycloak internal readiness port `9000` open; credentials and
  mutations are excluded (`build/remote-infra-readiness.json`).
- [x] In-network OIDC runtime replay passed: `geostat` discovery and an RSA/
  RS256 signing key were verified from the API container; only redacted issuer,
  JWKS URI and key metadata are persisted (`build/remote-oidc-readiness.json`).
- [x] API now has an explicit opt-in OIDC resource-server path using issuer
  discovery, JWKS asymmetric validation and issuer/audience validators; legacy
  HMAC mode remains the default until production authority enables
  `PLATFORM_OIDC_ENABLED=true`. API compile and `ApiApplicationTests` pass.
- [x] OIDC audience policy is isolated in the provider-neutral
  `OidcAudienceValidator`, with positive, negative and invalid-policy unit tests;
  targeted Gradle verification passes.
- [x] OIDC audience validation is explicitly fail-closed for null tokens and
  null audiences, with regression coverage in `OidcAudienceValidatorTest`.
- [x] OIDC resource-server construction now rejects a missing issuer or
  audience in code before decoder creation; `ApiSecurityConfigOidcPolicyTest`
  covers both invalid and complete policy configurations.
- [x] OIDC issuer policy now rejects malformed/relative URIs and requires
  HTTPS when the production profile is active; URI validation has regression
  coverage and remains compatible with non-production internal endpoints.
- [x] Production-profile HTTPS enforcement has an explicit regression test;
  an HTTP issuer is rejected while the non-production validation path remains
  available for isolated internal staging.
- [x] OIDC decoder validation now requires a non-empty `tenant_id` claim via
  `OidcRequiredClaimValidator`; missing, blank and null-token cases fail closed
  with regression coverage. Cross-tenant equality enforcement remains a
  separate production policy gate.
- [x] OIDC authentication now maps standard scopes and configurable nested role
  claims (default `realm_access.roles`) to Spring authorities through
  `OidcAuthoritiesConverter`; missing claims and null tokens are safe, with
  targeted tests. Final role/tenant policy approval remains external.
- [x] Tenant claim name is configurable (`OIDC_TENANT_CLAIM`, default
  `tenant_id`) and is propagated through application config, Compose and the
  production preflight; generic claim validation remains provider-neutral.
- [x] OIDC policy preflight now accepts the configured tenant claim name and
  resolves the mapper by its declared claim metadata instead of hardcoding
  `tenant_id`; default policy replay remains PASS.
- [x] OIDC role-to-authority mapping is configurable (`OIDC_ROLE_AUTHORITY_MAP`)
  and translates provider roles to the platform's declared resource authorities
  without endpoint-specific branching; scope, nested-role, alias and malformed
  mapping tests pass.
- [x] OIDC role mapping rejects duplicate role definitions instead of silently
  overwriting them; ambiguity regression coverage passes fail-closed.
- [x] OIDC audience, tenant-claim and role-mapping policy objects are validated
  before JWKS decoder discovery, so invalid authorization configuration cannot
  trigger a partially initialized network-backed decoder.
- [x] Production preflight now requires a non-empty `OIDC_ROLE_AUTHORITY_MAP`
  whenever OIDC is enabled and verifies its Compose declaration, preventing an
  authenticated token from entering the API without a declared authority map.
- [x] Production preflight parses the role-authority mapping, rejects malformed
  entries and duplicate roles before deployment, matching the runtime parser's
  fail-closed semantics.
- [x] OIDC role extraction accepts both JSON array claims and a provider's
  single-string role claim representation, while preserving configurable
  role-to-authority aliases and fail-closed handling of unsupported values.
- [x] Production Compose declares the OIDC opt-in bindings and production
  preflight now requires HTTPS issuer/audience whenever OIDC is enabled; missing
  or insecure runtime configuration fails closed.
- [x] Post-Keycloak-import remote replay passed: all five GEOSOTAT containers,
  API health and both anonymous/malformed-token denial checks remain PASS
  (`build/remote-staging-smoke-post-oidc.json`).
- [x] Canonical production-evidence handoff documents all ten remaining gates,
  required operator inputs, evidence commands and fail-closed closure rules in
  `docs/production-evidence-handoff.md`.
- [x] Release-gate literal-secret scan now covers both backend and frontend
  Geostat application trees, so a future UI/service config regression cannot
  bypass the same security gate; diagnostic release gate remains PASS.
- [x] Production-evidence readiness summary initializes and reports `FAIL`
  explicitly (alongside `PASS`, `NOT_RUN` and `NOT_ASSERTED`), preventing a
  malformed or failed check from corrupting the evidence artifact; normal
  readiness replay remains fail-closed (`NOT_READY_FOR_PRODUCTION`).
- [x] Access R8 fixture/context reconciliation completed: canonical fixture
  tests resolve `kids-portal-v1-canonical-r8-final.accdb` without worker-CWD
  assumptions, assert canonical prefixed physical tables and the full immutable
  `__raw_document` provenance envelope, while `ApiApplicationTests` uses
  isolated test datasource placeholders, explicit Hibernate dialect and
  test-only security settings; canonical generator/audit Gradle tasks now use
  `ops/scripts/java` as their single tool source.
- [x] Keyset service no longer exposes an unbound legacy overload; callers must
  provide the signed snapshot/revision claim and stale cursors fail closed;
  full API regression passed.
# Current verification update — 2026-09-15T08:30Z

- Read-only release-gate replay executed against the current worktree.
- Clean release remains unproven: `HEAD=98db52a9495ec17501aa20165efe47c502706a9b`,
  `describe=98db52a-dirty`, `statusEntries=645`; the gate correctly fails closed
  and no `-AllowDirty` result is treated as production evidence.
- Remote active container provenance remains separate from the canonical
  `backend/infra/geostat-platform` deployment and requires an approved clean
  migration before production activation.

## Current verification update — 2026-09-15T08:46Z

- API and mobile production boot artifacts rebuilt with Gradle
  (`--rerun-tasks`): `BUILD SUCCESSFUL`.
- Technical acceptance rerun: **33/33 PASS**; runtime ledger remains
  **278 verified / 10 open**.
- KIDS R8 delivery preflight no longer couples acceptance to a fixed check
  count; standalone and technical-runner invocation are schema-stable and
  cycle-safe.
- Evidence bundle `build/geostat-staging-evidence-bundle-r69.json` was
  generated and verifier-checked (SHA-256
  `90a09f91c8d4976a290706d42aeb85b3e255816fb3d6ec07bbc1b33253a92b90`).
- No production activation is claimed: clean approved release, operator
  secrets, canonical remote Compose replacement, external identity/tenant
  policy and measured resilience evidence remain required.
