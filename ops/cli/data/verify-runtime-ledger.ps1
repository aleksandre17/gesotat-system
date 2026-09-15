[CmdletBinding()]
param([string]$Ledger = 'build/runtime-ledger-acceptance.json')

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$path = Join-Path $root $Ledger
if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Runtime ledger missing: $Ledger" }
$doc = Get-Content -Raw -LiteralPath $path | ConvertFrom-Json
if ($doc.schema -ne 'geostat.runtime-ledger.v1') { throw "Unexpected ledger schema: $($doc.schema)" }
$migrationRoot = Join-Path $root 'platform/apps/geostat/backend/core/src/main/resources/db/platform'
$ids = @()
foreach ($entry in @($doc.migrations)) {
    if ($entry.file -notmatch '^(\d+)_') { throw "Non-canonical migration in ledger: $($entry.file)" }
    $id = [int]$Matches[1]
    if ($ids -contains $id) { throw "Duplicate migration id in ledger: $id" }
    $ids += $id
    $file = Join-Path $migrationRoot $entry.file
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { throw "Ledger migration missing: $($entry.file)" }
    $actual = (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne ([string]$entry.sha256).ToLowerInvariant()) { throw "Migration checksum mismatch: $($entry.file)" }
}
$audit = Get-Content -Raw -LiteralPath (Join-Path $root 'docs/platform-capability-and-architecture-audit-2026-09-13.md')
$checked = ([regex]::Matches($audit, '(?m)^- \[x\]')).Count
$open = ([regex]::Matches($audit, '(?m)^- \[ \]')).Count
if ($checked -ne [int]$doc.audit.verified -or $open -ne [int]$doc.audit.open) { throw "Audit count differs from ledger: actual $checked/$open" }
if (@($doc.implementationMarkers).Count -lt 4) { throw 'Ledger implementation markers are incomplete' }
[pscustomobject]@{ status = 'PASS'; schema = $doc.schema; migrations = @($doc.migrations).Count; contractMarkers = @($doc.contractMarkers).Count; verified = $checked; open = $open; ledger = $path } | ConvertTo-Json -Compress
