param(
  [string]$Image = 'geostat-api',
  [Parameter(Mandatory=$true)][string]$ExpectedRevision
)
$ErrorActionPreference = 'Stop'
if ($ExpectedRevision -notmatch '^[0-9a-fA-F]{7,64}$|^v?[0-9A-Za-z._-]{1,64}$') { throw 'Expected revision must be a commit/tag identifier' }
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { throw 'docker CLI is required' }
$actual = (& docker inspect $Image --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' 2>$null).Trim()
if ([string]::IsNullOrWhiteSpace($actual)) { throw "Image '$Image' has no org.opencontainers.image.revision label" }
if ($actual -ne $ExpectedRevision) { throw "Image provenance mismatch: expected '$ExpectedRevision', actual '$actual'" }
[pscustomobject]@{ status='PASS'; image=$Image; revision=$actual } | ConvertTo-Json -Compress
