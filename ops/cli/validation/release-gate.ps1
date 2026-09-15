<##
  Release provenance gate (read-only).
  Fails closed when a release cannot be reproduced from a clean, non-secret tree.
  Usage: pwsh ./scripts/release-gate.ps1 [-AllowDirty] [-ImageName <name> -ExpectedImageRevision <sha>]
##>
[CmdletBinding()]
param(
  [switch]$AllowDirty,
  [string]$ImageName,
  [string]$ExpectedImageRevision
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
Set-Location $root
$fail = @()

function Require-Path([string]$p) {
  if (-not (Test-Path -LiteralPath $p)) { $script:fail += "missing: $p" }
}

Require-Path 'platform/apps/geostat/backend/settings.gradle'
Require-Path 'platform/apps/geostat/backend/gradlew.bat'
Require-Path 'ops/compose/projects/geostat/docker-compose.prod.yml'
if (Test-Path -LiteralPath 'ops/compose/projects/geostat/docker-compose.prod.yml') {
  $composeText = Get-Content -LiteralPath 'ops/compose/projects/geostat/docker-compose.prod.yml' -Raw
  if ($composeText -notmatch 'IMAGE_REVISION:\s*\$\{IMAGE_REVISION:\?') { $fail += 'production Compose does not require IMAGE_REVISION for image builds' }
}
Require-Path 'ops/cli/validation/check.sh'
Require-Path 'ops/cli/lifecycle/production-preflight.ps1'
Require-Path 'ops/cli/validation/audit-checklist-count.ps1'
Require-Path 'ops/cli/validation/verify-image-provenance.ps1'
Require-Path 'ops/cli/data/generate-evidence-bundle.ps1'
Require-Path 'ops/cli/data/verify-evidence-bundle.ps1'
Require-Path 'ops/cli/data/generate-runtime-ledger.ps1'
Require-Path 'ops/cli/data/verify-runtime-ledger.ps1'
Require-Path 'ops/cli/validation/supply-chain-preflight.ps1'
Require-Path 'ops/cli/validation/documentation-consistency.ps1'
Require-Path 'ops/cli/validation/legacy-retirement-preflight.ps1'
Require-Path 'ops/cli/validation/authorization-surface-preflight.ps1'
Require-Path 'ops/cli/validation/oidc-policy-preflight.ps1'
Require-Path 'ops/cli/validation/production-evidence-readiness.ps1'
Require-Path 'ops/cli/validation/secret-boundary-preflight.ps1'
Require-Path 'ops/cli/lifecycle/deploy.sh'
if (Test-Path -LiteralPath 'ops/cli/lifecycle/deploy.sh') {
  $deployText = Get-Content -LiteralPath 'ops/cli/lifecycle/deploy.sh' -Raw
  if ($deployText -notmatch "IMAGE_REVISION=.*docker-compose") { $fail += 'deploy script does not forward IMAGE_REVISION to remote Compose build' }
  if ($deployText -notmatch 'clean checkout') { $fail += 'deploy script lacks clean-checkout provenance guard' }
}
if (Test-Path -LiteralPath 'ops/cli/validation/audit-checklist-count.ps1') {
  & pwsh -NoProfile -File '.\ops\cli\validation\audit-checklist-count.ps1' *> $null
  if ($LASTEXITCODE -ne 0) { $fail += 'audit checklist declared count does not match checkbox state' }
}
if ((Test-Path -LiteralPath 'ops/cli/data/generate-runtime-ledger.ps1') -and (Test-Path -LiteralPath 'ops/cli/data/verify-runtime-ledger.ps1')) {
  & pwsh -NoProfile -File '.\ops\cli\data\generate-runtime-ledger.ps1' -Output 'build/release-runtime-ledger.json' *> $null
  if ($LASTEXITCODE -ne 0) { $fail += 'runtime ledger generation failed' }
  & pwsh -NoProfile -File '.\ops\cli\data\verify-runtime-ledger.ps1' -Ledger 'build/release-runtime-ledger.json' *> $null
  if ($LASTEXITCODE -ne 0) { $fail += 'runtime ledger verification failed' }
}
if (Test-Path -LiteralPath 'ops/cli/validation/supply-chain-preflight.ps1') {
  & pwsh -NoProfile -File '.\ops\cli\validation\supply-chain-preflight.ps1' -Output 'build/release-supply-chain.json' *> $null
  if ($LASTEXITCODE -ne 0) { $fail += 'supply-chain preflight failed' }
}
if (Test-Path -LiteralPath 'ops/cli/validation/documentation-consistency.ps1') {
  & pwsh -NoProfile -File '.\ops\cli\validation\documentation-consistency.ps1' -Output 'build/release-documentation-consistency.json' *> $null
  if ($LASTEXITCODE -ne 0) { $fail += 'documentation consistency report failed' }
}
if (Test-Path -LiteralPath 'ops/cli/validation/legacy-retirement-preflight.ps1') {
  & pwsh -NoProfile -File '.\ops\cli\validation\legacy-retirement-preflight.ps1' -Output 'build/release-legacy-retirement.json' *> $null
  if ($LASTEXITCODE -ne 0) { $fail += 'legacy retirement preflight failed' }
}
if (Test-Path -LiteralPath 'ops/cli/validation/authorization-surface-preflight.ps1') {
  & pwsh -NoProfile -File '.\ops\cli\validation\authorization-surface-preflight.ps1' *> 'build/release-authorization-surface.json'
  if ($LASTEXITCODE -ne 0) { $fail += 'authorization surface preflight failed' }
  else {
    try { $authEvidence = Get-Content -Raw 'build/release-authorization-surface.json' | ConvertFrom-Json; if ($authEvidence.schema -ne 'geostat.authorization-surface-preflight.v1' -or $authEvidence.status -ne 'PASS') { $fail += 'authorization surface evidence is invalid' } } catch { $fail += 'authorization surface evidence is not valid JSON' }
  }
}
if (Test-Path -LiteralPath 'ops/cli/validation/oidc-policy-preflight.ps1') {
  & pwsh -NoProfile -File '.\ops\cli\validation\oidc-policy-preflight.ps1' -Output 'build/release-oidc-policy.json' *> $null
  if ($LASTEXITCODE -ne 0) { $fail += 'OIDC policy preflight failed' }
  elseif (Test-Path -LiteralPath 'build/release-oidc-policy.json') {
    try { $oidcEvidence = Get-Content -Raw 'build/release-oidc-policy.json' | ConvertFrom-Json; if ($oidcEvidence.schema -ne 'geostat.oidc-policy-preflight.v1' -or $oidcEvidence.status -ne 'PASS' -or $oidcEvidence.runtimeBinding -ne 'NOT_ASSERTED') { $fail += 'OIDC policy evidence is invalid or overclaims runtime binding' } } catch { $fail += 'OIDC policy evidence is not valid JSON' }
  } else { $fail += 'OIDC policy evidence artifact missing' }
}
if (Test-Path -LiteralPath 'ops/cli/validation/production-evidence-readiness.ps1') {
  & pwsh -NoProfile -File '.\ops\cli\validation\production-evidence-readiness.ps1' -Output 'build/release-production-evidence-readiness.json' *> $null
  if ($LASTEXITCODE -ne 0) { $fail += 'production evidence readiness runner failed' }
  elseif (Test-Path -LiteralPath 'build/release-production-evidence-readiness.json') {
    try { $readiness = Get-Content -Raw 'build/release-production-evidence-readiness.json' | ConvertFrom-Json; if ($readiness.schema -ne 'geostat.production-evidence-readiness.v1' -or $readiness.releaseDecision -ne 'NOT_READY_FOR_PRODUCTION') { $fail += 'production evidence readiness report is invalid' }; if ($readiness.checks.secretBoundary.status -ne 'PASS') { $fail += 'secret boundary gate is not PASS' } } catch { $fail += 'production evidence readiness report is not valid JSON' }
  } else { $fail += 'production evidence readiness report missing' }
}
Require-Path 'docs/KIDS-R8-current-status-and-acceptance.md'
Require-Path 'docs/platform-capability-and-architecture-audit-2026-09-13.md'
Require-Path 'documentation/complete-package/PACKAGE-INDEX.html'

if ($ImageName -or $ExpectedImageRevision) {
  if ([string]::IsNullOrWhiteSpace($ImageName) -or [string]::IsNullOrWhiteSpace($ExpectedImageRevision)) {
    $fail += 'image provenance check requires both -ImageName and -ExpectedImageRevision'
  } elseif (Test-Path -LiteralPath 'ops/cli/validation/verify-image-provenance.ps1') {
    & pwsh -NoProfile -File '.\ops\cli\validation\verify-image-provenance.ps1' -Image $ImageName -ExpectedRevision $ExpectedImageRevision *> $null
    if ($LASTEXITCODE -ne 0) { $fail += "image provenance mismatch or unavailable: $ImageName" }
  }
}

$status = @(git status --porcelain=v1)
if ($status.Count -gt 0 -and -not $AllowDirty) {
  $fail += "working tree is not clean ($($status.Count) entries); commit source/migrations/tests or pass -AllowDirty only for local diagnostics"
}
$previousErrorAction = $ErrorActionPreference
try {
  # Git may emit harmless line-ending advice on stderr; the native exit code
  # remains authoritative for the actual whitespace check.
  $ErrorActionPreference = 'Continue'
  git diff --check 2>$null | Out-Null
  if ($LASTEXITCODE -ne 0) { $fail += 'git diff --check failed (whitespace/hygiene defect)' }
} finally { $ErrorActionPreference = $previousErrorAction }

$secretPatterns = @('(?i)password\s*[:=]\s*["''][^"'']{8,}["'']','(?i)secret[_-]?key\s*[:=]\s*["''][^"'']{8,}["'']','(?i)AKIA[0-9A-Z]{16}')
$scanRoots = @('platform/apps/geostat/backend','platform/apps/geostat/frontend','scripts','db','docs') | Where-Object { Test-Path -LiteralPath $_ }
foreach ($r in $scanRoots) {
  $hits = Get-ChildItem -LiteralPath $r -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch '\\build\\|\\.gradle\\|\\documentation\\complete-package\\' -and $_.Extension -in '.java','.gradle','.yml','.yaml','.properties','.ps1','.sh','.md','.sql' } |
    Select-String -Pattern $secretPatterns -ErrorAction SilentlyContinue |
    Where-Object { $_.Path -notmatch '\.env|\.example|README|docs\\' -and $_.Line -notmatch '\$\{[^}]+\}' }
  if ($hits) { $fail += "possible secret material in source tree: $($hits[0].Path)" }
}

$sha = (git rev-parse --verify HEAD 2>$null)
$describe = (git describe --always --dirty 2>$null)
$result = [ordered]@{
  generatedAt = [DateTime]::UtcNow.ToString('o')
  repository = $root
  head = if ($sha) { $sha.Trim() } else { $null }
  describe = if ($describe) { $describe.Trim() } else { $null }
  statusEntries = $status.Count
  allowDirty = [bool]$AllowDirty
  passed = ($fail.Count -eq 0)
  failures = $fail
}
$result | ConvertTo-Json -Depth 4
if ($fail.Count -gt 0) { exit 1 }
