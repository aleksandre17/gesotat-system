[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)][string]$Output,
  [string]$ReleaseReference,
  [string]$ContractChecksum,
  [string]$SnapshotChecksum,
  [string]$GateReport,
  [string]$EvidencePath = ''
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$resolvedOutput = if ([IO.Path]::IsPathRooted($Output)) { [IO.Path]::GetFullPath($Output) } else { [IO.Path]::GetFullPath((Join-Path (Get-Location) $Output)) }
$parent = Split-Path -Parent $resolvedOutput
if (-not (Test-Path -LiteralPath $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }

function Hash-File([string]$path) {
  $resolved = (Resolve-Path -LiteralPath $path).Path
  $item = Get-Item -LiteralPath $resolved
  $hash = (Get-FileHash -LiteralPath $resolved -Algorithm SHA256).Hash.ToLowerInvariant()
  [ordered]@{ path = $resolved.Substring($root.Length).TrimStart('\','/'); size = $item.Length; sha256 = $hash }
}

$files = @()
$evidencePaths = @($EvidencePath -split '[,;]' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
foreach ($path in $evidencePaths) {
  if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Evidence file not found: $path" }
  $files += Hash-File $path
}
$gate = $null
if ($GateReport) {
  if (-not (Test-Path -LiteralPath $GateReport -PathType Leaf)) { throw "Gate report not found: $GateReport" }
  $gate = Hash-File $GateReport
}

$bundle = [ordered]@{
  schema = 'geostat.evidence-bundle.v1'
  generatedAt = [DateTime]::UtcNow.ToString('o')
  repositoryRoot = $root
  releaseReference = if ($ReleaseReference) { $ReleaseReference } else { (git -C $root rev-parse HEAD 2>$null).Trim() }
  contractChecksum = $ContractChecksum
  snapshotChecksum = $SnapshotChecksum
  files = @($files)
  gateReport = $gate
  secretPolicy = 'No secret values are read or serialized; only hashes and non-sensitive metadata are recorded.'
}
$json = $bundle | ConvertTo-Json -Depth 8
[IO.File]::WriteAllText($resolvedOutput, $json, [Text.UTF8Encoding]::new($false))
$bundleHash = (Get-FileHash -LiteralPath $resolvedOutput -Algorithm SHA256).Hash.ToLowerInvariant()
[IO.File]::WriteAllText("$resolvedOutput.sha256", "$bundleHash  $([IO.Path]::GetFileName($resolvedOutput))`n", [Text.UTF8Encoding]::new($false))
[IO.File]::SetAttributes($resolvedOutput, [IO.FileAttributes]::ReadOnly)
[pscustomobject]@{ status='PASS'; bundle=$resolvedOutput; sha256=$bundleHash; fileCount=$files.Count; gateIncluded=[bool]$gate } | ConvertTo-Json -Compress
