[CmdletBinding()]
param(
  [string]$Output = 'build/technical-acceptance-report.json',
  [switch]$RunTests = $true
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$started = [DateTime]::UtcNow
$results = [System.Collections.Generic.List[object]]::new()

function Check([string]$name, [scriptblock]$action) {
  try { & $action; $results.Add([ordered]@{ name=$name; status='PASS' }) }
  catch { $results.Add([ordered]@{ name=$name; status='FAIL'; error=$_.Exception.Message }) }
}
function Required([string]$relative) {
  $path = Join-Path $root $relative
  if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Required artefact missing: $relative" }
}

Check 'engineering governance' {
  & python (Join-Path $root 'ops/cli/validation/engineering-governance.py') --root $root | Out-Null
  if ($LASTEXITCODE -ne 0) { throw 'Engineering governance failed' }
}

Check 'release automation artefacts' {
  Required 'ops/cli/validation/release-gate.ps1'; Required 'ops/cli/data/generate-evidence-bundle.ps1'; Required 'ops/cli/data/verify-evidence-bundle.ps1'; Required 'ops/cli/validation/production-evidence-readiness.ps1'; Required 'ops/cli/validation/remote-staging-smoke.ps1'; Required 'ops/cli/validation/kids-r8-api-delivery-preflight.ps1'; Required 'ops/cli/validation/schema-agnostic-runtime-preflight.ps1'; Required 'ops/cli/validation/secret-boundary-preflight.ps1'; Required 'ops/cli/validation/deployment-secret-injection-preflight.ps1'; Required 'ops/cli/validation/secure-overlay-preflight.ps1'; Required 'docs/production-evidence-handoff.md'
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/production-evidence-readiness.ps1 -Output build/production-evidence-readiness.json | Out-Null; if ($LASTEXITCODE -ne 0) { throw "production evidence readiness exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'host layout boundary' {
  Required 'ops/cli/validation/host-layout-check.ps1'
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/host-layout-check.ps1 | Out-Null; if ($LASTEXITCODE -ne 0) { throw "host layout checker exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'OIDC policy artefact' {
  Required 'ops/infra/geostat-platform/keycloak/geostat-realm.json'
  Required 'ops/cli/validation/oidc-policy-preflight.ps1'
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/oidc-policy-preflight.ps1 -Output build/oidc-policy-acceptance.json | Out-Null; if ($LASTEXITCODE -ne 0) { throw "OIDC policy preflight exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'OIDC runtime endpoint preflight' {
  Required 'ops/cli/validation/oidc-runtime-preflight.ps1'
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/oidc-runtime-preflight.ps1 -Output build/oidc-runtime-acceptance.json | Out-Null; if ($LASTEXITCODE -ne 0) { throw "OIDC runtime preflight exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'authorization surface preflight' {
  Required 'ops/cli/validation/authorization-surface-preflight.ps1'
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/authorization-surface-preflight.ps1 | Out-Null; if ($LASTEXITCODE -ne 0) { throw "authorization preflight exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'schema-agnostic runtime preflight' {
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/schema-agnostic-runtime-preflight.ps1 | Out-Null; if ($LASTEXITCODE -ne 0) { throw "schema-agnostic runtime preflight exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'secret boundary preflight' {
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/secret-boundary-preflight.ps1 -Output build/secret-boundary-acceptance.json | Out-Null; if ($LASTEXITCODE -ne 0) { throw "secret boundary preflight exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'deployment secret injection boundary' {
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/deployment-secret-injection-preflight.ps1 -Output build/deployment-secret-injection-acceptance.json | Out-Null; if ($LASTEXITCODE -ne 0) { throw "deployment secret injection preflight exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'secure production overlay contract' {
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/secure-overlay-preflight.ps1 -Output build/secure-overlay-acceptance.json | Out-Null; if ($LASTEXITCODE -ne 0) { throw "secure overlay preflight exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'remote staging read-only replay' {
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/remote-staging-smoke.ps1 -ContractCode KIDS_PORTAL_V1 -Output build/remote-staging-smoke-latest.json | Out-Null; if ($LASTEXITCODE -ne 0) { throw "remote staging smoke exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'KIDS R8 API delivery handoff' {
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/kids-r8-api-delivery-preflight.ps1 -Output build/kids-r8-api-delivery-preflight.json -SkipTechnicalEvidence | Out-Null; if ($LASTEXITCODE -ne 0) { throw "KIDS R8 delivery preflight exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'remote infrastructure readiness' {
  Required 'ops/cli/validation/remote-infra-readiness.ps1'
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/remote-infra-readiness.ps1 -Output build/remote-infra-readiness.json | Out-Null; if ($LASTEXITCODE -ne 0) { throw "remote infrastructure readiness exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'remote OIDC discovery and JWKS readiness' {
  Required 'ops/cli/validation/remote-oidc-readiness.ps1'
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/remote-oidc-readiness.ps1 -Output build/remote-oidc-readiness.json | Out-Null; if ($LASTEXITCODE -ne 0) { throw "remote OIDC readiness exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'isolated provider acceptance tests' {
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/ContractOnlyOnboardingTest.java'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/ContractOnlyDatabaseReplayTest.java'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/CrossFamilyRelationProjectionAcceptanceTest.java'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/IndependentProviderCompositeAcceptanceTest.java'
}
Check 'provider capability negotiation' {
  Required 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ProviderCapabilityNegotiator.java'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/ProviderCapabilityNegotiatorTest.java'
}
Check 'provider lifecycle governance' {
  Required 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ProviderLifecycle.java'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/ProviderLifecycleTest.java'
  Required 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ProviderHealthSupervisor.java'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/ProviderHealthSupervisorTest.java'
}
Check 'contract lifecycle persistence' {
  Required 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ContractLifecycleOrchestrator.java'
  Required 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/JdbcContractLifecycleStateStore.java'
  Required 'platform/apps/geostat/backend/core/src/main/resources/db/platform/084_contract_revision_lifecycle.sql'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/ContractLifecycleOrchestratorTest.java'
}
Check 'data-family lifecycle conformance' {
  Required 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/DataFamilyLifecycle.java'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/DataFamilyLifecycleConformanceTest.java'
  Required 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/JdbcLifecycleStateStore.java'
  Required 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/LifecyclePersistenceConfiguration.java'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/DataFamilyLifecycleOrchestratorTest.java'
}
Check 'semantic compatibility surface analysis' {
  Required 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/SemanticCompatibilityAnalyzer.java'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/SemanticCompatibilityAnalyzerTest.java'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/SemanticGoldenSurfaceAcceptanceTest.java'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/PersistedProjectionIntegrationTest.java'
  Required 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ContractCompatibilityService.java'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/ContractCompatibilityServiceTest.java'
}
Check 'provider-neutral keyset tuple semantics' {
  Required 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/KeysetTupleComparator.java'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/KeysetTupleComparatorTest.java'
}
Check 'OpenAPI contract surface validation' {
  Required 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ContractOpenApiValidator.java'
  Required 'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/ContractOpenApiValidatorTest.java'
}
Check 'Redis quota wiring' { Required 'platform/apps/geostat/backend/api/src/main/java/org/base/api/config/RedisRateLimitStore.java'; Required 'ops/infra/geostat-platform/docker-compose.prod.yml' }
Check 'OTel collector wiring' { Required 'ops/infra/geostat-platform/otel-collector-config.yml'; Required 'platform/apps/geostat/backend/api/build.gradle' }
Check 'runtime ledger generation' {
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/data/generate-runtime-ledger.ps1 -Output build/runtime-ledger-acceptance.json; if ($LASTEXITCODE -ne 0) { throw "runtime ledger exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'runtime ledger verification' {
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/data/verify-runtime-ledger.ps1 -Ledger build/runtime-ledger-acceptance.json; if ($LASTEXITCODE -ne 0) { throw "runtime ledger verification exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'evidence bundle root and verifier regression' {
  Required 'ops/cli/data/generate-evidence-bundle.ps1'; Required 'ops/cli/data/verify-evidence-bundle.ps1'
  $probe = Join-Path ([IO.Path]::GetTempPath()) ('geostat-evidence-' + [Guid]::NewGuid().ToString('N'))
  New-Item -ItemType Directory -Path $probe -Force | Out-Null
  try {
    $bundle = Join-Path $probe 'bundle.json'
    & pwsh -NoProfile -File ./ops/cli/data/generate-evidence-bundle.ps1 -Output $bundle -EvidencePath 'build/runtime-ledger-acceptance.json' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "evidence bundle generator exit $LASTEXITCODE" }
    & pwsh -NoProfile -File ./ops/cli/data/verify-evidence-bundle.ps1 -Bundle $bundle | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "evidence bundle verifier exit $LASTEXITCODE" }
  } finally { if (Test-Path -LiteralPath $probe) { Remove-Item -LiteralPath $probe -Recurse -Force } }
}
Check 'runtime ledger tamper resistance' {
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/data/runtime-ledger-integrity-smoke.ps1; if ($LASTEXITCODE -ne 0) { throw "runtime ledger integrity smoke exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'portability matrix planner' {
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/portability/portability-matrix.ps1 -Output build/portability-matrix-acceptance.json; if ($LASTEXITCODE -ne 0) { throw "portability matrix exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'supply-chain preflight' {
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/supply-chain-preflight.ps1 -Output build/supply-chain-acceptance.json; if ($LASTEXITCODE -ne 0) { throw "supply-chain preflight exit $LASTEXITCODE" } }
  finally { Pop-Location }
}
Check 'production readiness local evidence' {
  $path = Join-Path $root 'build/production-evidence-readiness.json'
  if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw 'production readiness report is missing' }
  $report = Get-Content -Raw -LiteralPath $path | ConvertFrom-Json
  if ($report.schema -ne 'geostat.production-evidence-readiness.v1') { throw 'production readiness schema mismatch' }
  if (-not $report.localEvidence -or @($report.localEvidence | Where-Object { -not $_.present }).Count -gt 0) { throw 'local implementation evidence is incomplete' }
}
if ($RunTests) {
  Check 'contract-only synthetic acceptance' {
    Push-Location $root
    try { Push-Location (Join-Path $root 'platform/apps/geostat/backend'); & ./gradlew :api:test --no-daemon --rerun-tasks --tests '*ContractOnlyOnboardingTest' --tests '*ContractOnlyDatabaseReplayTest' --tests '*CrossFamilyRelationProjectionAcceptanceTest' --tests '*IndependentProviderCompositeAcceptanceTest' --tests '*PersistedProjectionIntegrationTest'; if ($LASTEXITCODE -ne 0) { throw "Gradle exit $LASTEXITCODE" } }
    finally { Pop-Location }
  }
  Check 'full API regression' {
    Push-Location $root
    try { Push-Location (Join-Path $root 'platform/apps/geostat/backend'); & ./gradlew :api:test --no-daemon; if ($LASTEXITCODE -ne 0) { throw "Gradle exit $LASTEXITCODE" } }
    finally { Pop-Location }
  }
}
Check 'audit checklist consistency' {
  Push-Location $root
  try { & pwsh -NoProfile -File ./ops/cli/validation/audit-checklist-count.ps1; if ($LASTEXITCODE -ne 0) { throw "audit checker exit $LASTEXITCODE" } }
  finally { Pop-Location }
}

$passed = @($results | Where-Object status -eq 'PASS').Count
$failed = @($results | Where-Object status -eq 'FAIL').Count
$report = [ordered]@{ schema='geostat.technical-acceptance.v1'; startedAt=$started.ToString('o'); completedAt=[DateTime]::UtcNow.ToString('o'); passed=$passed; failed=$failed; results=@($results); productionApproval='NOT_ASSERTED'; note='Technical checks do not substitute for production authority, signed approvals, or measured DR/load evidence.' }
$resolved = Join-Path (Get-Location) $Output
$parent = Split-Path -Parent $resolved
if (-not (Test-Path -LiteralPath $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
[IO.File]::WriteAllText($resolved, ($report | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
$report | ConvertTo-Json -Depth 8
if ($failed -gt 0) { exit 1 }
