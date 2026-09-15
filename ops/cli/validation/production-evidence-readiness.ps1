[CmdletBinding()]
param([string]$Output='build/production-evidence-readiness.json')

$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path; Set-Location $root
$checks = [ordered]@{
  secretBoundary = [ordered]@{ status='NOT_RUN'; reason='secret-boundary preflight has not been executed yet' }
  releaseAuthority = [ordered]@{ status='NOT_ASSERTED'; reason='signed release tag/commit and deploy authority are external approvals' }
  oidcRuntime = [ordered]@{ status=if($env:OIDC_ISSUER_URL -and $env:OIDC_JWKS_URL){'NOT_RUN'}else{'NOT_ASSERTED'}; reason='protected-token replay and issuer/JWKS validation require approved runtime configuration' }
  tenantPolicy = [ordered]@{ status='NOT_ASSERTED'; reason='cross-tenant ABAC evidence requires approved tenant fixtures and token claims' }
  independentProvider = [ordered]@{ status=if($env:PLATFORM_PORTABILITY_SQLSERVER_URL -or $env:PLATFORM_PORTABILITY_MYSQL_URL){'NOT_RUN'}else{'NOT_RUN'}; reason='real provider acceptance requires configured SQL Server/MySQL endpoint' }
  redisHa = [ordered]@{ status='NOT_ASSERTED'; reason='production Redis HA/failover and fairness workload evidence not measured' }
  durableTelemetry = [ordered]@{ status='NOT_ASSERTED'; reason='durable trace/log backend and alert firing evidence not available' }
  backupDr = [ordered]@{ status='NOT_ASSERTED'; reason='approved RPO/RTO and production DR authority are external' }
  resilienceWindow = [ordered]@{ status='NOT_ASSERTED'; reason='approved load/chaos/security test window and signed results are external' }
}
$localEvidence = @(
  'build/technical-acceptance-report.json',
  'build/runtime-ledger-acceptance.json',
  'build/remote-infra-readiness.json',
  'build/deployment-secret-injection-acceptance.json',
  'build/secret-boundary-readiness.json',
  'build/secure-overlay-acceptance.json',
  'ops/infra/geostat-platform/docker-compose.prod.yml',
  'ops/infra/geostat-platform/otel-collector-config.yml',
  'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/CrossFamilyRelationProjectionAcceptanceTest.java',
  'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/IndependentProviderCompositeAcceptanceTest.java',
  'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/PersistedProjectionIntegrationTest.java',
  'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/SemanticGoldenSurfaceAcceptanceTest.java'
) | ForEach-Object {
  [ordered]@{ path = $_; present = (Test-Path -LiteralPath (Join-Path $root $_) -PathType Leaf) }
}
$secretPreflight=Join-Path $root 'ops/cli/validation/secret-boundary-preflight.ps1'
if(Test-Path -LiteralPath $secretPreflight){
  $secretOutput=Join-Path $root 'build/secret-boundary-readiness.json'
  try {
    & pwsh -NoProfile -File $secretPreflight -Output 'build/secret-boundary-readiness.json' | Out-Null
    $checks.secretBoundary=[ordered]@{status=if($LASTEXITCODE -eq 0){'PASS'}else{'FAIL'};reason='repository runtime/config secret-boundary scan'}
  } catch { $checks.secretBoundary=[ordered]@{status='FAIL';reason=$_.Exception.Message} }
}
$oidcPreflight=Join-Path $root 'ops/cli/validation/oidc-runtime-preflight.ps1'
if((Test-Path -LiteralPath $oidcPreflight) -and $env:OIDC_ISSUER_URL -and $env:OIDC_JWKS_URL){
  try {
    & pwsh -NoProfile -File $oidcPreflight -Output 'build/oidc-runtime-readiness.json' | Out-Null
    $checks.oidcRuntime=[ordered]@{status=if($LASTEXITCODE -eq 0){'PASS'}else{'FAIL'};reason='issuer/JWKS runtime endpoint and JWKS structure preflight'}
  } catch { $checks.oidcRuntime=[ordered]@{status='FAIL';reason=$_.Exception.Message} }
}
$summary=[ordered]@{ PASS=0; FAIL=0; NOT_RUN=0; NOT_ASSERTED=0 }
foreach($v in $checks.Values){$summary[$v.status]=[int]$summary[$v.status]+1}
$result=[ordered]@{schema='geostat.production-evidence-readiness.v1';generatedAt=[DateTime]::UtcNow.ToString('o');checks=$checks;localEvidence=$localEvidence;summary=$summary;releaseDecision='NOT_READY_FOR_PRODUCTION';rule='No external approval or runtime evidence is inferred from local implementation tests.'}
$resolved=Join-Path (Get-Location) $Output;$parent=Split-Path -Parent $resolved;if($parent -and -not(Test-Path $parent)){New-Item -ItemType Directory -Path $parent -Force|Out-Null};[IO.File]::WriteAllText($resolved,($result|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false));$result|ConvertTo-Json -Depth 8
