[CmdletBinding()]
param([string]$Output='build/legacy-retirement-preflight.json')
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path; Set-Location $root
$prod='platform/apps/geostat/backend/api/src/main/resources/application-prod.yml'
$policy='platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/LegacyFallbackPolicy.java'
$controller='platform/apps/geostat/backend/api/src/main/java/org/base/api/controller/DynamicDataController.java'
$notice='docs/api-modernization-capability-gap.md'
$governance='docs/legacy-retirement-governance.md'
$governanceText=Get-Content -Raw $governance
$approvalBlock=([regex]::Match($governanceText,'(?s)```json\s*(\{.*?\})\s*```')).Groups[1].Value
$approval=$null
try { if($approvalBlock){$approval=$approvalBlock|ConvertFrom-Json} } catch { $approval=$null }
$checks=[ordered]@{
 productionDefaultDisabled = ((Get-Content -Raw $prod) -match 'dynamic-pages:\s*\r?\n\s*enabled:\s*\$\{PLATFORM_LEGACY_DYNAMIC_PAGES_ENABLED:false\}')
 policyBoundaryPresent = (Test-Path -LiteralPath $policy -PathType Leaf)
 fallbackTelemetryPresent = ((Get-Content -Raw $controller) -match 'geostat\.legacy\.dynamic_pages\.fallback')
 migrationNoticePresent = ((Get-Content -Raw $notice) -match 'Legacy fallback retirement|controlled deprecation|migration notice')
 governanceRecordPresent = (Test-Path -LiteralPath $governance -PathType Leaf)
 consumerImpactSection = ($governanceText -match '## 1\. Consumer-impact review')
 deprecationWindowSection = ($governanceText -match '## 2\. Deprecation window' -and $governanceText -match 'T\+60 days')
 rollbackSection = ($governanceText -match '## 3\. Rollback plan' -and $governanceText -match 'configuration-only, idempotent')
 approvalRecordSchema = ($null -ne $approval -and $approval.recordType -eq 'LEGACY_RETIREMENT_APPROVAL' -and $approval.status -eq 'PENDING' -and $approval.rollbackPlan)
}
$result=[ordered]@{schema='geostat.legacy-retirement-preflight.v1';generatedAt=[DateTime]::UtcNow.ToString('o');checks=$checks;status=if(@($checks.Values|Where-Object { -not $_ }).Count -eq 0){'READY_FOR_IMPACT_REVIEW'}else{'FAIL'};decision='NO_AUTOMATIC_REMOVAL';requiredApproval=@('consumer-impact review','steward approval','deprecation window','rollback plan')}
$resolved=Join-Path (Get-Location) $Output;$parent=Split-Path -Parent $resolved;if(-not(Test-Path $parent)){New-Item -ItemType Directory -Path $parent -Force|Out-Null};[IO.File]::WriteAllText($resolved,($result|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false));$result|ConvertTo-Json -Depth 8
if($result.status -eq 'FAIL'){exit 1}
