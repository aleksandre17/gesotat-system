[CmdletBinding()]
param([string]$Output = 'build/supply-chain-preflight.json')

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$composeFiles = @('ops/compose/projects/geostat/docker-compose.prod.yml','ops/infra/geostat-platform/docker-compose.prod.yml') | ForEach-Object { Join-Path $root $_ } | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf }
$images = @()
foreach ($file in $composeFiles) {
    foreach ($line in Get-Content -LiteralPath $file) {
        if ($line -match '^\s*image:\s*([^\s#]+)') { $images += [pscustomobject]@{ image=$Matches[1]; file=(Resolve-Path $file).Path } }
    }
}
$floating = @($images | Where-Object { $_.image -match '(^|:)latest$' -or $_.image -notmatch ':' })
if ($floating.Count -gt 0) { throw "Floating/unpinned container image(s): $($floating.image -join ', ')" }
$syft = Get-Command syft -ErrorAction SilentlyContinue
$trivy = Get-Command trivy -ErrorAction SilentlyContinue
$tools = [ordered]@{ syft = [bool]$syft; trivy = [bool]$trivy }
$status = if ($syft -and $trivy) { 'READY_FOR_EXECUTION' } else { 'NOT_RUN' }
$doc = [ordered]@{ schema='geostat.supply-chain-preflight.v1'; generatedAt=[DateTime]::UtcNow.ToString('o'); images=$images; tools=$tools; status=$status; productionApproval='NOT_ASSERTED'; note='Static pinning is enforced. SBOM and vulnerability scans require approved registry access and syft/trivy credentials; absence is explicit NOT_RUN.' }
$out = Join-Path $root $Output; $parent=Split-Path -Parent $out; if($parent -and -not(Test-Path $parent)){New-Item -ItemType Directory -Path $parent -Force|Out-Null}; $doc|ConvertTo-Json -Depth 6|Set-Content -LiteralPath $out -Encoding UTF8
[pscustomobject]@{status='PASS'; imageCount=$images.Count; scanStatus=$status; output=$out}|ConvertTo-Json -Compress
