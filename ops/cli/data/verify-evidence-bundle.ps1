[CmdletBinding()]
param(
  [Parameter(Mandatory=$true)][string]$Bundle
)

$ErrorActionPreference = 'Stop'
$resolved = (Resolve-Path -LiteralPath $Bundle).Path
$sidecar = "$resolved.sha256"
if (-not (Test-Path -LiteralPath $sidecar -PathType Leaf)) { throw "Evidence sidecar not found: $sidecar" }

$actual = (Get-FileHash -LiteralPath $resolved -Algorithm SHA256).Hash.ToLowerInvariant()
$line = (Get-Content -LiteralPath $sidecar -Raw).Trim()
if ($line -notmatch "^([0-9a-fA-F]{64})\s+(.+)$") { throw 'Invalid SHA-256 sidecar format' }
if ($Matches[1].ToLowerInvariant() -ne $actual) { throw 'Evidence bundle checksum mismatch' }

$doc = Get-Content -LiteralPath $resolved -Raw | ConvertFrom-Json
if ($doc.schema -ne 'geostat.evidence-bundle.v1') { throw "Unsupported evidence schema: $($doc.schema)" }
if ([string]::IsNullOrWhiteSpace([string]$doc.releaseReference)) { throw 'releaseReference is required' }
if ($null -eq $doc.files) { throw 'files collection is required' }

foreach ($entry in @($doc.files)) {
  if ([string]::IsNullOrWhiteSpace([string]$doc.repositoryRoot)) { throw 'repositoryRoot is required' }
  $repoRoot = [IO.Path]::GetFullPath([string]$doc.repositoryRoot)
  $candidate = [IO.Path]::GetFullPath((Join-Path $repoRoot $entry.path))
  if (-not $candidate.StartsWith($repoRoot.TrimEnd('\','/') + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw "Referenced evidence path escapes repository root: $($entry.path)" }
  if (-not (Test-Path -LiteralPath $candidate -PathType Leaf)) { throw "Referenced evidence file not found: $($entry.path)" }
  $hash = (Get-FileHash -LiteralPath $candidate -Algorithm SHA256).Hash.ToLowerInvariant()
  if ($hash -ne ([string]$entry.sha256).ToLowerInvariant()) { throw "Referenced evidence hash mismatch: $($entry.path)" }
  $size = (Get-Item -LiteralPath $candidate).Length
  if ([int64]$entry.size -ne $size) { throw "Referenced evidence size mismatch: $($entry.path)" }
}

$attrs = (Get-Item -LiteralPath $resolved).Attributes
if (($attrs -band [IO.FileAttributes]::ReadOnly) -eq 0) { throw 'Evidence bundle must be read-only after generation' }
[pscustomobject]@{ status='PASS'; bundle=$resolved; sha256=$actual; fileCount=@($doc.files).Count; schema=$doc.schema } | ConvertTo-Json -Compress
