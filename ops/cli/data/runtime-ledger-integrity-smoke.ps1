[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$good = Join-Path $root 'build/runtime-ledger-smoke.json'
$tampered = Join-Path $root 'build/runtime-ledger-smoke-tampered.json'
& pwsh -NoProfile -File (Join-Path $PSScriptRoot 'generate-runtime-ledger.ps1') -Output 'build/runtime-ledger-smoke.json' *> $null
if ($LASTEXITCODE -ne 0) { throw 'Unable to generate smoke ledger' }
$doc = Get-Content -Raw -LiteralPath $good | ConvertFrom-Json
if (@($doc.migrations).Count -eq 0) { throw 'Smoke ledger has no migrations' }
$doc.migrations[0].sha256 = ('0' * 64)
$doc | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $tampered -Encoding UTF8
& pwsh -NoProfile -File (Join-Path $PSScriptRoot 'verify-runtime-ledger.ps1') -Ledger 'build/runtime-ledger-smoke-tampered.json' *> $null
$exit = $LASTEXITCODE
Remove-Item -LiteralPath $good, $tampered -Force -ErrorAction SilentlyContinue
if ($exit -eq 0) { throw 'Tampered runtime ledger was accepted' }
[pscustomobject]@{ status = 'PASS'; tamperDetected = $true; schema = 'geostat.runtime-ledger.v1' } | ConvertTo-Json -Compress
