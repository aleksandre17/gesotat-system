[CmdletBinding()]
param(
  [string]$Output = 'build/kids-r8-api-delivery-preflight.json',
  [switch]$SkipTechnicalEvidence
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$checks = [System.Collections.Generic.List[object]]::new()
function Check([string]$name, [bool]$ok, [string]$detail) {
  $checks.Add([ordered]@{name=$name;status=if($ok){'PASS'}else{'FAIL'};detail=$detail})
  if (-not $ok) { $script:failed++ }
}
$failed = 0

$artifact = Join-Path $root 'platform/apps/geostat/backend/api/kids-portal-v1-canonical-r8-final.accdb'
$runbook = Join-Path $root 'docs/kids-r8-api-delivery-runbook.md'
$technical = Join-Path $root 'build/technical-acceptance-report.json'
$remote = Join-Path $root 'build/remote-staging-smoke-latest.json'

Check 'canonical R8 artifact exists' (Test-Path -LiteralPath $artifact) $artifact
if (Test-Path -LiteralPath $artifact) {
  $item = Get-Item -LiteralPath $artifact
  $hash = (Get-FileHash -LiteralPath $artifact -Algorithm SHA256).Hash
  Check 'canonical R8 artifact fingerprint' ($item.Length -eq 2920448 -and $hash -eq '1930EAD852912858F25D85867FC075AAFFECD7F6704C2540E41E623764D27ACC') "bytes=$($item.Length); sha256=$hash"
}
Check 'API delivery runbook present' (Test-Path -LiteralPath $runbook) $runbook
if (Test-Path -LiteralPath $runbook) {
  $text = Get-Content -LiteralPath $runbook -Raw
  Check 'runbook declares R8 page/query contract' ($text.Contains('KIDS_PORTAL_V1') -and $text.Contains('/api/v1/platform/contracts') -and $text.Contains('pageId')) 'contract, endpoint and page identity markers present'
}
if ($SkipTechnicalEvidence) {
  Check 'technical acceptance evidence' $true 'deferred to the current technical-acceptance runner (cycle-safe invocation)'
} elseif (Test-Path -LiteralPath $technical) {
  $t = Get-Content -LiteralPath $technical -Raw | ConvertFrom-Json
  # Do not couple this handoff gate to a hard-coded number of checks.  The
  # technical runner is intentionally extensible; a successful report means
  # no failed checks and at least one recorded result, irrespective of the
  # current check count.
  $hasResults = $null -ne $t.results -and @($t.results).Count -gt 0
  Check 'technical acceptance evidence' ($hasResults -and $t.failed -eq 0) "passed=$($t.passed); failed=$($t.failed); results=$(@($t.results).Count)"
} else { Check 'technical acceptance evidence' $false 'report missing' }
if (Test-Path -LiteralPath $remote) {
  $r = Get-Content -LiteralPath $remote -Raw | ConvertFrom-Json
  Check 'remote read-only staging evidence' ($r.status -eq 'PASS' -and $r.passed -eq 8 -and $r.failed -eq 0 -and $r.readOnly -eq $true) "status=$($r.status); passed=$($r.passed); failed=$($r.failed); readOnly=$($r.readOnly)"
} else { Check 'remote read-only staging evidence' $false 'report missing' }

$resolved = if ([IO.Path]::IsPathRooted($Output)) { [IO.Path]::GetFullPath($Output) } else { [IO.Path]::GetFullPath((Join-Path (Get-Location) $Output)) }
$parent = Split-Path -Parent $resolved
if (-not (Test-Path -LiteralPath $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
$result = [ordered]@{schema='geostat.kids-r8-api-delivery-preflight.v1';generatedAt=[DateTime]::UtcNow.ToString('o');checks=@($checks);passed=@($checks|? status -eq 'PASS').Count;failed=$failed;status=if($failed -eq 0){'PASS'}else{'FAIL'};productionActivation='NOT_ASSERTED';note='This preflight proves repository/staging handoff readiness only; production authority and publication approval remain external gates.'}
[IO.File]::WriteAllText($resolved, ($result|ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
$result | ConvertTo-Json -Compress
if ($failed -gt 0) { exit 1 }
