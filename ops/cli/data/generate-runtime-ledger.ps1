[CmdletBinding()]
param([string]$Output = 'build/runtime-ledger.json')

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$migrationRoot = Join-Path $root 'platform/apps/geostat/backend/core/src/main/resources/db/platform'
if (-not (Test-Path -LiteralPath $migrationRoot -PathType Container)) { throw "Migration directory missing: $migrationRoot" }

$migrations = @(Get-ChildItem -LiteralPath $migrationRoot -Filter '*.sql' -File | ForEach-Object {
    if ($_.Name -notmatch '^(\d+)_([^.]*)\.sql$') { throw "Migration filename is not canonical: $($_.Name)" }
    [pscustomobject]@{ id = [int]$Matches[1]; file = $_.Name; sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() }
} | Sort-Object id, file)
$duplicateIds = @($migrations | Group-Object id | Where-Object Count -gt 1 | Select-Object -ExpandProperty Name)
if ($duplicateIds.Count -gt 0) { throw "Duplicate migration ids: $($duplicateIds -join ', ')" }
$contracts = @()
foreach ($migration in $migrations) {
    $content = Get-Content -Raw -LiteralPath (Join-Path $migrationRoot $migration.file)
    foreach ($match in [regex]::Matches($content, "contract_code=N'([^']+)'[^\r\n]{0,500}?revision(?:=|,revision=)(\d+)", [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)) {
        $contracts += [pscustomobject]@{ code = $match.Groups[1].Value; revision = [int]$match.Groups[2].Value; migration = $migration.id; file = $migration.file }
    }
}
$contracts = @($contracts | Sort-Object code, revision, migration -Unique)

$auditPath = Join-Path $root 'docs/platform-capability-and-architecture-audit-2026-09-13.md'
$audit = Get-Content -Raw -LiteralPath $auditPath
$checked = ([regex]::Matches($audit, '(?m)^- \[x\]')).Count
$open = ([regex]::Matches($audit, '(?m)^- \[ \]')).Count
$declared = @([regex]::Matches($audit, '\*\*(\d+) verified items\*\*, \*\*(\d+) open items\*\*')) | Select-Object -First 1
if (-not $declared.Success -or $checked -ne [int]$declared.Groups[1].Value -or $open -ne [int]$declared.Groups[2].Value) { throw "Audit count drift: actual $checked/$open" }

$required = @(
    'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ContractLifecycle.java',
    'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ContractChecksumBinding.java',
    'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/StableSortSpec.java',
    'ops/cli/validation/documentation-zero-drift.ps1',
    'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/JdbcLifecycleStateStore.java',
    'platform/apps/geostat/backend/core/src/main/resources/db/platform/083_data_family_lifecycle_state.sql'
    ,'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/IndependentProviderCompositeAcceptanceTest.java'
    ,'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/SemanticGoldenSurfaceAcceptanceTest.java'
    ,'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/PersistedProjectionIntegrationTest.java'
    ,'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/LifecyclePersistenceConfiguration.java'
    ,'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ProviderHealthSupervisor.java'
    ,'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/ProviderHealthSupervisorTest.java'
    ,'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/ContractLifecycleOrchestrator.java'
    ,'platform/apps/geostat/backend/api/src/main/java/org/base/api/service/platform/JdbcContractLifecycleStateStore.java'
    ,'platform/apps/geostat/backend/core/src/main/resources/db/platform/084_contract_revision_lifecycle.sql'
    ,'platform/apps/geostat/backend/api/src/test/java/org/base/api/service/platform/ContractLifecycleOrchestratorTest.java'
    ,'ops/compose/projects/geostat/docker-compose.secure.yml'
    ,'ops/cli/validation/secure-overlay-preflight.ps1'
    ,'ops/infra/geostat-platform/docker-compose.prod.yml'
    ,'ops/infra/geostat-platform/otel-collector-config.yml'
)
$missing = @($required | Where-Object { -not (Test-Path -LiteralPath (Join-Path $root $_) -PathType Leaf) })
if ($missing.Count -gt 0) { throw "Runtime ledger required artifact missing: $($missing -join ', ')" }

$outPath = Join-Path $root $Output
$parent = Split-Path -Parent $outPath
if ($parent -and -not (Test-Path -LiteralPath $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
$composeRoot = Join-Path $root 'ops/compose/projects/geostat'
$deployments = @()
if (Test-Path -LiteralPath $composeRoot -PathType Container) {
    $deployments = @(Get-ChildItem -LiteralPath $composeRoot -Filter 'docker-compose*.yml' -Recurse -File | ForEach-Object {
        $text = Get-Content -Raw -LiteralPath $_.FullName
        foreach ($m in [regex]::Matches($text, '(?m)^\s*container_name:\s*([^\s#]+)')) {
            [pscustomobject]@{ compose = $_.FullName.Substring($root.Length + 1); container = $m.Groups[1].Value }
        }
    } | Sort-Object compose, container -Unique)
}
$ledger = [ordered]@{
    schema = 'geostat.runtime-ledger.v1'
    generatedAt = [DateTime]::UtcNow.ToString('o')
    audit = [ordered]@{ file = $AuditPath; verified = $checked; open = $open }
    migrations = $migrations
    contractMarkers = $contracts
    deploymentBindings = $deployments
    implementationMarkers = $required
    productionAuthority = 'NOT_ASSERTED'
    note = 'Generated inventory and consistency evidence; it does not assert production approval, deployment, or external acceptance.'
}
$ledger | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $outPath -Encoding UTF8
[pscustomobject]@{ status = 'PASS'; schema = $ledger.schema; migrations = $migrations.Count; contractMarkers = $contracts.Count; verified = $checked; open = $open; output = $outPath } | ConvertTo-Json -Compress
