[CmdletBinding()]
param([string]$Output='build/secure-overlay-acceptance.json')
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$relative='ops/compose/projects/geostat/docker-compose.secure.yml'
$path=Join-Path $root $relative
if(-not(Test-Path -LiteralPath $path -PathType Leaf)){throw "secure compose overlay missing: $relative"}
$text=[IO.File]::ReadAllText($path)
$keys=@('PLATFORM_OIDC_ENABLED','OIDC_ISSUER_URL','OIDC_AUDIENCE','PLATFORM_RATE_LIMIT_REDIS_ENABLED','PLATFORM_RATE_LIMIT_REDIS_URL','OTEL_METRICS_ENABLED','OTEL_METRICS_ENDPOINT')
$missing=@($keys|Where-Object{ -not $text.Contains('${' + $_ + ':?') })
$status=if($missing.Count -eq 0){'PASS'}else{'FAIL'}
$result=[ordered]@{schema='geostat.secure-overlay-preflight.v1';generatedAt=[DateTime]::UtcNow.ToString('o');status=$status;overlay=$relative;requiredKeys=$keys;missing=@($missing);rule='secure overlay requires explicit non-empty OIDC, Redis quota and OTel bindings'}
$resolved=Join-Path (Get-Location) $Output;$parent=Split-Path -Parent $resolved;if(-not(Test-Path $parent)){New-Item -ItemType Directory -Path $parent -Force|Out-Null};[IO.File]::WriteAllText($resolved,($result|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false));$result|ConvertTo-Json -Depth 6
if($missing.Count -gt 0){exit 1}
