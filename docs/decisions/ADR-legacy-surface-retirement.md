# ADR-011 — Legacy surface retirement (gate, dedicated operator authority, egress allow-lists)

**სტატუსი:** ACCEPTED (2026-09-19) · **Owner:** პროექტის მფლობელი
**Authority:** `docs/decisions/ADR-tenant-scoped-authorization.md` (ADR-010 §5, §12),
`docs/work/ARCHITECTURE-IMPROVEMENT-REGISTER.md` AIR-2026-044,
`docs/legacy-retirement-governance.md`
**Standards:** RFC 9110 §15.5.11 (410 Gone), RFC 9457 (problem details);
OWASP API Security Top 10 — API5 (Broken Function Level Authorization), API7 (SSRF);
NIST SP 800-53 SC-7 (boundary protection / restricted egress); deny by default.
**Layer:** Security (cross-cutting) + Delivery (configuration contract). No schema change.

---

## 1. პრობლემა (bounded scope)

ADR-010-ის §5 ცხრილში 12 ზედაპირი `@TenantNeutral(... LEGACY ...)`-ია: მათი მონაცემი core
profile/page plane-შია და `platform.data_product` identity **არ აქვთ**, ამიტომ tenant-scoped ვერ
გახდებიან. მიუხედავად ამისა ისინი გაშლილი იყო ფართო business authority-ებით
(`WRITE_RESOURCE` / `READ_RESOURCE` / `ADMIN`), ანუ ნებისმიერი ჩვეულებრივი ოპერატორი აღწევდა:

- `XlsxToCsvController` — caller-supplied URL-ის fetch (SSRF ზედაპირი);
- `MSSQLToAccess` — caller-supplied database host + credentials (arbitrary outbound JDBC);
- `ManagedImportController` + 8 per-domain Access upload controller — ungoverned ჩაწერა;
- `ResponseController` — სტატიკური demo payload-ები.

Scope: ამ ზედაპირების **ხელმისაწვდომობა და egress**. მათი migration governed ingestion line-ზე
ამ ADR-ის ნაწილი **არ არის** და ღია რჩება.

## 2. გადაწყვეტილება

1. **ისინი არ არიან governed platform-ის ნაწილი.** თითოეული ოჯახი არის ერთი ცხადი feature
   switch, **default-ად გამორთული**, და გამორთულ მდგომარეობაში მარშრუტი პასუხობს
   **410 Gone**-ს RFC 9457 problem+json-ით. 404 იქნებოდა ცრუ განცხადება („ასეთი მარშრუტი
   არასდროს არსებობდა"); 410 არის ზუსტად ის სტატუსი, რომელიც RFC 9110 §15.5.11-ით ნიშნავს
   განზრახ და მუდმივად ამოღებულ, ოდესღაც ცნობილ რესურსს — და ეს არის ის, რაც მიგრაციაზე მყოფ
   ოპერატორს უნდა ეთქვას.
2. **ჩართულ მდგომარეობაშიც** ისინი მოითხოვენ **ცალკე ოპერატორულ authority-ს**
   (`PLATFORM_LEGACY_OPERATOR`), არა business role-ს. authority ენიჭება IdP-ში
   `platform.legacy` role-ით `platform.oidc.role-authority-map`-ის გავლით — platform code-ში
   literal არ არის (იგივე მექანიზმი, რაც ADR-010 §7-ის `platform.admin=PLATFORM_CROSS_TENANT`).
3. **Egress deny-by-default.** `XlsxToCsvController`-ის remote fetch და `MSSQLToAccess`-ის
   სამიზნე host მოითხოვს **ცხად allow-list-ს**; **ცარიელი სია თიშავს ზუსტად იმ ერთ
   შესაძლებლობას**, არა მთელ ზედაპირს (იხ. §6a.4).
4. **DNS rebinding.** სახელი იხსნება **ზუსტად ერთხელ**, ყველა დაბრუნებული მისამართი მოწმდება
   და socket უკავშირდება სწორედ იმ pinned მისამართს (`RemoteFetchGuard.PinnedAddressSocketFactory`).
   TLS-ის SNI და hostname verification თავდაპირველ hostname-ზე რჩება, ამიტომ pinning transport-ს
   არ ასუსტებს. check-სა და connect-ს შორის მისამართის გამოცვლა შეუძლებელია.
5. **Production-ში ჩართული legacy ოჯახი არ აჩერებს გაშვებას** — ის წერს **startup WARNING**-ს,
   რომელიც ასახელებს ზედაპირს და მის property-ს. მფლობელს შეიძლება ეს მარშრუტები migration-ის
   ფანჯარაში დასჭირდეს; გაჩუმება კი დაუშვებელია.

## 3. ზედაპირის ოჯახები (switch-ები)

| Family | Switch (property) | Environment key | Controllers |
|---|---|---|---|
| `ACCESS_UPLOAD` | `platform.legacy.access-upload.enabled` | `PLATFORM_LEGACY_ACCESS_UPLOAD_ENABLED` | `ManagedImportController`, `biznes_statistika/MobileController`, `prices/{CpiCalculator,Inflation,kaleidoskope}`, `sagareo_vachroba/{FdiController,SagareoController}`, `sazogadoebastan_urtiertoba/ShedarebaPortali`, `socialuri_statistika/Salarium` (9) |
| `SPREADSHEET_CONVERSION` | `platform.legacy.spreadsheet-conversion.enabled` | `PLATFORM_LEGACY_SPREADSHEET_CONVERSION_ENABLED` | `XlsxToCsvController` |
| `DATABASE_EXPORT` | `platform.legacy.database-export.enabled` | `PLATFORM_LEGACY_DATABASE_EXPORT_ENABLED` | `MSSQLToAccess` |
| `DEMO` | `platform.legacy.demo.enabled` | `PLATFORM_LEGACY_DEMO_ENABLED` | `ResponseController` |

დამატებითი egress property-ები:

| Property | Environment key | წესი |
|---|---|---|
| `platform.legacy.spreadsheet-conversion.allowed-hosts` | `PLATFORM_LEGACY_SPREADSHEET_CONVERSION_ALLOWED_HOSTS` | exact host ან `.suffix`, მძიმით; **ცარიელი = მხოლოდ remote fetch გამორთულია; ლოკალური upload/convert მუშაობს** |
| `platform.legacy.database-export.allowed-hosts` | `PLATFORM_LEGACY_DATABASE_EXPORT_ALLOWED_HOSTS` | იგივე ფორმატი; ამ ზედაპირის ერთადერთი ფუნქცია გარე კავშირია, ამიტომ **ცარიელი სია = export მიუწვდომელია** — პარიტეტისთვის უნდა შეივსოს |
| `platform.legacy.operator-authority` | `PLATFORM_LEGACY_OPERATOR_AUTHORITY` | default `PLATFORM_LEGACY_OPERATOR` |

`platform.legacy.dynamic-pages.enabled` უცვლელია — ის უკვე იყო ამ pattern-ის პირველი წევრი და
ამ ADR-მა მისი `LegacyFallbackPolicy` გამეორა, ახალი მექანიზმი არ გამოუგონებია.

## 4. Coverage model — declared, not a URL list

ADR-010 §5-ის იგივე პრინციპი: controller აცხადებს `@LegacySurfaceGate(LegacySurface.X)`-ს;
`LegacySurfaceInterceptor` მიბმულია **მთელ `/api/v1/**`-ზე** და **annotation** წყვეტს, არა URL.
Interceptor-ის order არის `HIGHEST_PRECEDENCE + 5` — ანუ tenancy interceptor-ზე **ადრე**:
ამოღებულ ზედაპირს data product არ აქვს, ამიტომ tenancy-ს შეფასება მასზე აზრს მოკლებულია.

**SOLID.** ერთი bean ერთ საკითხზე: `LegacySurfacePolicy` (ჩართულია თუ არა + ვის აქვს უფლება),
`HostAllowList` (სუფთა matcher), `RemoteFetchGuard` (HTTP egress + pinning), `DatabaseEndpointGuard`
(JDBC egress). controller-ები თხელი რჩებიან; `XlsxToCsvController`-ის `downloadFile` აღარ ხსნის
სახელს და აღარ წყვეტს არაფერს — მხოლოდ ზომისა და timeout-ის ზღვარს აწესებს.

**No site literal.** `LegacySurface` enum შეიცავს მხოლოდ ოჯახის ტექნიკურ სახელს; არც domain, არც
page და არც site literal არ არის engine-ში.

## 5. Telemetry

`geostat.legacy.surface.request`, tag-ები `surface` (4 მნიშვნელობა) და `outcome`
(`served` / `gone`) — low cardinality, `geostat.legacy.dynamic_pages.fallback`-ის სტილში.

## 6. Failure modes

| მდგომარეობა | ქცევა |
|---|---|
| ოჯახი გამორთულია | 410 problem+json, `code=legacy-surface-retired`, `surface`, `enableWith`, `Cache-Control: no-store` |
| ჩართულია, caller-ს არ აქვს operator authority | 403 (არსებული `ApiAccessDeniedHandler` / method security) |
| ჩართულია, allow-list ცარიელი | **მხოლოდ** remote fetch / export უარყოფილია (`IOException` / `IllegalArgumentException`); ლოკალური upload/convert უცვლელად მუშაობს |
| host allow-list-ის გარეთ | უარყოფილია **სახელის გახსნამდე** |
| DNS პასუხში თუნდაც ერთი private/loopback/link-local/multicast მისამართი | მთელი სახელი უარყოფილია |
| prod profile + ჩართული ოჯახი | გაშვება გრძელდება, **WARNING** ასახელებს ზედაპირს |

## 6a. Functional parity (owner constraint)

dev არის production-ის პროტოტიპი: ცვლილებამ **არცერთი დღეს მოქმედი ფუნქცია არ უნდა გატეხოს**.
ამიტომ:

1. **კოდის default რჩება „გამორთული"** (secure by default), მაგრამ **environment contract ცხადად
   რთავს** მათ: `ops/config/projects/geostat/shared/templates/common.env.example`-ში ოთხივე
   `*_ENABLED` არის `true`, და compose-ის pass-through-ს აქვს `:-false` fallback — ანუ ის გარემო,
   რომელიც გასაღებს საერთოდ არ აცხადებს, დახურული რჩება.
2. **არცერთი მოქმედი მომხმარებელი არ იკარგება.** ეს ზედაპირები გამოიყენება **LEGACY
   authentication რეჟიმშიც** (HMAC JWT `/sign/login`-იდან; authority-ები `dbo.roles` /
   `dbo.permissions` / `dbo.role_permissions`-იდან). ახალი authority, რომელსაც არავინ ფლობს,
   ჩუმად გამორთავდა ყველა ამჟამინდელ ოპერატორს. ამიტომ **migration 105**
   (`105_legacy_operator_permission_seed.sql`, control plane, `PlatformSchemaMigrationRunner`-ში
   104-ის შემდეგ) additive და idempotent-ად seed-ავს `PLATFORM_LEGACY_OPERATOR` permission-ს და
   ანიჭებს მას **ყველა იმ role-ს, რომელიც დღეს ფლობს** `WRITE_RESOURCE`, `READ_RESOURCE` ან
   `ADMIN`-ს. grant **გამოიყვანება permission ცხრილებიდან** — არცერთი role სახელი migration-ში
   არ წერია.
3. **OIDC რეჟიმში** იგივე პარიტეტი `platform.oidc.role-authority-map`-ის default-ით მიიღწევა:
   `contract.read`, `contract.write`, `ingest.execute`, `raw.read`, `admin` ახლა იძლევიან **ორ**
   authority-ს — თავის ძველ business authority-სა და `PLATFORM_LEGACY_OPERATOR`-ს.
   `quality.approve` / `publish.execute` არ იღებენ: ისინი ამ ზედაპირებს ისედაც ვერ წვდებოდნენ.
4. **Allow-list-ის სემანტიკა:** ცარიელი სია თიშავს **მხოლოდ იმ ერთ შესაძლებლობას**, არა მთელ
   ზედაპირს. `XlsxToCsvController`-ის ლოკალური upload/convert (`POST /upload`, `GET /convert`
   URL-ის გარეშე) ცარიელი egress სიითაც მუშაობს. `MSSQLToAccess`-ის ერთადერთი ფუნქცია კი
   **არის** გარე კავშირი, ამიტომ მისთვის ცარიელი სია = ფუნქცია მიუწვდომელია;
   `PLATFORM_LEGACY_DATABASE_EXPORT_ALLOWED_HOSTS` **უნდა შეივსოს** პარიტეტისთვის.
5. **Request/response shape უცვლელია** ყველა gated endpoint-ზე — მხოლოდ authorization და egress
   შეიცვალა. არსებული frontend-ები byte-compatible პასუხს იღებენ.

**Production hosts:** `metaDatabaseUrl` მოდის profile plane-ის `MetaDatabaseItem` row-ებიდან
(control-plane UI-ის upload ეკრანი), **არა repository-დან**. ამიტომ ამჟამინდელი production
export host-ები **ამ repo-ში არ არის აღწერილი და არ არის გამოსაცნობი**; ისინი ოპერატორმა უნდა
ჩაწეროს allow-list-ში. host არ არის გამოგონილი.

## 7. Migration / compatibility / rollback

- **Migration 105** (control plane) — additive, idempotent, literal-free permission seed + derived
  grant. არაფერს შლის და არაფერს ართმევს.
- დანარჩენი — **additive configuration only**.
- **არცერთი მოქმედი მომხმარებელი არ იკარგება** (§6a): კოდი default-ად დახურულია, მაგრამ
  environment contract აცხადებს ჩართვას, ხოლო migration 105 / role-authority map აძლევს ყველა
  ამჟამინდელ ოპერატორს ახალ authority-ს. ერთადერთი მოქმედება, რაც ოპერატორს მოეთხოვება, არის
  `PLATFORM_LEGACY_DATABASE_EXPORT_ALLOWED_HOSTS`-ის შევსება.
- **Retirement** ხდება მაშინ, როცა შესაბამისი `*_ENABLED` გადაირთვება `false`-ზე;
  `docs/legacy-retirement-governance.md`-ის deprecation window / rollback წესი მაშინ ვრცელდება.
- **Rollback:** configuration-only — შესაბამისი `*_ENABLED=true` + allow-list + IdP-ში
  `platform.legacy` role. იდემპოტენტური, restart-ით.
- `ResponseController` **არ წაიშალა**: `platform/apps/geostat/frontend/geostat-system-app`-ის
  dashboard დღემდე იძახებს `/api/v1/test/{dashboardStats,revenueChart,usersChart}`-ს
  (`src/api/hooks.ts`). ამიტომ ის `DEMO` ოჯახში მოექცა და default-ად ამოღებულია; წაშლა ხდება
  მაშინ, როცა ის dashboard governed metric-ებზე გადავა.

## 8. Tests

- `LegacySurfacePolicyTest` — ყველა ოჯახი default-ად დახურული; operator authority არა-business;
  prod profile → ზუსტად ერთი WARNING ზედაპირის სახელით; არა-prod → WARNING არ არის.
- `LegacySurfaceGateTest` — გამორთული → 410 problem+json `surface`/`enableWith`-ით და counter
  `outcome=gone`; ჩართული → handler მიიღწევა, counter `outcome=served`; ungated controller ხელუხლებელია.
- `LegacySurfaceAuthorityTest` — **რეალური `@PreAuthorize` გამოსახულებით** (`@EnableMethodSecurity`),
  **ოთხივე ოჯახზე**: მხოლოდ ძველი business authority → `AccessDeniedException`;
  ძველი authority **+** `PLATFORM_LEGACY_OPERATOR` (ზუსტად ის, რასაც migration 105 / role map
  აწარმოებს) → service მიიღწევა და demo payload byte-compatible რჩება; caller-ის გარეშე → უარი.
- `LegacyOperatorAuthorityParityTest` — `application.yml`-ის **shipped** default map-ს კითხულობს:
  ყოველი role, რომელიც `WRITE_RESOURCE`/`READ_RESOURCE`/`ADMIN`-ს იძლევა, იძლევა
  `PLATFORM_LEGACY_OPERATOR`-საც; `publish.execute` — არა; migration 105 იმავე სამ authority-ს ფარავს.
- `LegacyOperatorPermissionMigrationTest` — 105 additive, idempotent, role-literal-ის გარეშე,
  runner-ში numeric order-ში რეგისტრირებული.
- `RemoteFetchGuardTest` — ცარიელი allow-list → უარი (სახელიც არ იხსნება); host mismatch → უარი
  lookup-მდე; match → pinned public მისამართი; non-HTTPS / userinfo / null → უარი; ერთი private
  პასუხი → მთელი სახელი უარყოფილია; **rebinding**: ზუსტად ერთი lookup და pinned socket.
- `DatabaseEndpointGuardTest` — ცარიელი სია = გამორთული; `host` და `host:port` match; უცხო host,
  ცარიელი და `null` → უარი.
- `HostAllowListTest` — ცარიელი სია არაფერს უშვებს; exact vs look-alike; `.suffix` და
  sub-domain-ები; `example.org.attacker.tld` არ ემთხვევა.
- `TenantScopeCoverageTest` უცვლელი და მწვანე: ყველა ეს ზედაპირი კვლავ `@TenantNeutral(legacy=true)`-ია
  და თავის reason-ში წერს `LEGACY`-ს.

## 9. ღია საკითხები

- ამ 12 ზედაპირის **migration governed ingestion line-ზე** არ შესრულებულა; ისინი კვლავ
  product identity-ს გარეშე არიან — უბრალოდ აღარ არიან ხელმისაწვდომი default-ად.
- `geostat-system-app`-ის dashboard კვლავ demo endpoint-ებზეა მიბმული (`DEMO` ოჯახი).
- `MSSQLToAccess`-ის caller-supplied credentials რჩება: allow-list ზღუდავს **სად** უკავშირდება,
  არა **რა credential-ით**. სრული გადაწყვეტა არის managed connection registry, რაც ცალკე
  გადაწყვეტილებაა.
- `XlsxToCsvController#downloadAllSheets` კვლავ აბრუნებს `X-Frame-Options: ALLOWALL`-ს;
  ეს ზედაპირი ახლა default-ად ამოღებულია, მაგრამ ჩართვისას ეს header უნდა გადაიხედოს.
