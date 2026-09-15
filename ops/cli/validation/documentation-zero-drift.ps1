[CmdletBinding()]
param([string]$AuditFile='docs/platform-capability-and-architecture-audit-2026-09-13.md')
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$required=@('docs/api-modernization-capability-gap.md','docs/system-complete-learning-guide.md','docs/production-authority-input-contract.md','docs/production-blockers-and-required-authority.md','ops/cli/validation/release-gate.ps1','ops/cli/data/generate-evidence-bundle.ps1','ops/cli/data/verify-evidence-bundle.ps1','ops/cli/data/generate-runtime-ledger.ps1','ops/cli/validation/host-layout-check.ps1','ops/cli/validation/documentation-consistency.ps1')
foreach($rel in $required){if(-not(Test-Path -LiteralPath (Join-Path $root $rel) -PathType Leaf)){throw "Required documentation/automation missing: $rel"}}
$audit=Get-Content -Raw -LiteralPath (Join-Path $root $AuditFile)
$checked=([regex]::Matches($audit,'(?m)^- \[x\]')).Count; $open=([regex]::Matches($audit,'(?m)^- \[ \]')).Count
$declared=[regex]::Match($audit,'\*\*(\d+) verified items\*\*, \*\*(\d+) open items\*\*')
if(-not $declared.Success){throw 'Audit declared count missing'}
if($checked -ne [int]$declared.Groups[1].Value -or $open -ne [int]$declared.Groups[2].Value){throw "Audit count drift: actual $checked/$open"}
if($audit -notmatch 'W-01.*W-07'){throw 'Canonical W-01–W-07 scope missing'}
$plan=Get-Content -Raw -LiteralPath (Join-Path $root 'docs/contract-driven-metadata-schema-agnostic-completion-plan.md')
if($plan -notmatch 'C-01.*C-14'){throw 'Canonical C-01–C-14 completion plan missing'}
$source=Join-Path $root 'platform/apps/geostat/backend'
$files=@('core/src/main/resources/db/platform/081_provider_capability_registry.sql','api/src/main/java/org/base/api/service/platform/ProviderCapabilityDiscoveryService.java','api/src/main/java/org/base/api/service/platform/ContractLifecycle.java','api/src/main/java/org/base/api/service/platform/StableSortSpec.java','api/src/main/java/org/base/api/service/platform/ContractChecksumBinding.java','api/src/main/java/org/base/api/service/platform/LegacyFallbackPolicy.java','api/src/main/java/org/base/api/service/platform/StatisticalQueryAdapter.java','api/src/main/java/org/base/api/service/platform/CanonicalStatisticalQueryAdapter.java','api/src/main/java/org/base/api/service/platform/ProviderCapabilityNegotiator.java','api/src/main/java/org/base/api/service/platform/ProviderLifecycle.java','api/src/main/java/org/base/api/service/platform/DataFamilyLifecycle.java','api/src/main/java/org/base/api/service/platform/SemanticCompatibilityAnalyzer.java','api/src/main/java/org/base/api/service/platform/ContractCompatibilityService.java','api/src/main/java/org/base/api/service/platform/KeysetTupleComparator.java','api/src/main/java/org/base/api/service/platform/ContractOpenApiValidator.java')
foreach($rel in $files){if(-not(Test-Path -LiteralPath (Join-Path $source $rel) -PathType Leaf)){throw "Required source missing: platform/apps/geostat/backend/$rel"}}
[pscustomobject]@{status='PASS';verified=$checked;open=$open;requiredFiles=$required.Count;sourceBoundary='platform/apps/geostat/backend';audit=(Resolve-Path (Join-Path $root $AuditFile)).Path} | ConvertTo-Json -Compress
