[CmdletBinding()]
param([string]$Output='build/documentation-consistency.json')
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
Set-Location $root
$required=@(
 'docs/contract-driven-metadata-schema-agnostic-completion-plan.md',
 'docs/platform-capability-and-architecture-audit-2026-09-13.md',
 'docs/api-complete-input-output-contract.md',
 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ContractIntrospectionService.java',
 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ContractResponseSerializer.java',
 'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ContractOpenApiService.java',
 'ops/cli/data/generate-runtime-ledger.ps1',
 'docs/legacy-retirement-governance.md',
 'documentation/complete-package/PACKAGE-INDEX.html')
$missing=@($required | Where-Object { -not (Test-Path -LiteralPath $_ -PathType Leaf) })
$plan=Get-Content -Raw -LiteralPath 'docs/contract-driven-metadata-schema-agnostic-completion-plan.md'
$audit=Get-Content -Raw -LiteralPath 'docs/platform-capability-and-architecture-audit-2026-09-13.md'
$markers=[ordered]@{
 contractPlan=($plan -match 'C-01.*C-14')
 apiContract=($plan -match 'Contract persistence/API orchestration')
 runtimeLedger=($plan -match 'runtime ledger')
 auditScope=($audit -match 'W-01.*W-07')
}
$result=[ordered]@{schema='geostat.documentation-consistency.v1';generatedAt=[DateTime]::UtcNow.ToString('o');requiredCount=$required.Count;missing=$missing;markers=$markers;status=if($missing.Count -eq 0 -and @($markers.Values|Where-Object { -not $_ }).Count -eq 0){'PASS'}else{'FAIL'}}
$resolved=Join-Path (Get-Location) $Output; $parent=Split-Path -Parent $resolved;if(-not(Test-Path $parent)){New-Item -ItemType Directory -Path $parent -Force|Out-Null};[IO.File]::WriteAllText($resolved,($result|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false));$result|ConvertTo-Json -Depth 8
if($result.status -ne 'PASS'){exit 1}
