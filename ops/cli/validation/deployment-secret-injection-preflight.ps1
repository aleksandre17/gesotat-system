[CmdletBinding()]
param([string]$Output='build/deployment-secret-injection-acceptance.json')
$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$entrypoints=@(
  'ops/cli/validation/check.sh',
  'ops/cli/lifecycle/deploy.sh',
  'ops/cli/lifecycle/deploy-infra.sh',
  'ops/cli/lifecycle/provision-infra-remote.sh',
  'ops/cli/lifecycle/platform-preflight.ps1',
  'ops/cli/lifecycle/platform-bootstrap.ps1',
  'ops/cli/lifecycle/production-preflight.ps1',
  'ops/compose/projects/geostat/docker-compose.dev.yml'
  ,'ops/compose/projects/geostat/docker-compose.secure.yml'
)
$forbidden=@('ops/config/projects/geostat/shared/secrets/local.env','ops/config/projects/geostat/shared/secrets/production.env')
$requiredInfraKeys=@('GEOSTAT_REDIS_PASSWORD','GRAFANA_ADMIN_PASSWORD','KEYCLOAK_DB_NAME','KEYCLOAK_DB_USER','KEYCLOAK_DB_PASSWORD','KEYCLOAK_HOSTNAME','KEYCLOAK_ADMIN_USERNAME','KEYCLOAK_ADMIN_PASSWORD')
$findings=[System.Collections.Generic.List[string]]::new()
foreach($relative in $entrypoints) {
  $path=Join-Path $root $relative
  if(-not (Test-Path -LiteralPath $path -PathType Leaf)) { $findings.Add("missing:$relative"); continue }
  $text=[IO.File]::ReadAllText($path)
  foreach($needle in $forbidden) { if($text.Contains($needle)) { $findings.Add("forbidden-reference:${relative}:$needle") } }
  if($relative -ne 'ops/cli/lifecycle/provision-infra-remote.sh' -and $relative -ne 'ops/compose/projects/geostat/docker-compose.secure.yml' -and -not $text.Contains('GEOSTAT_ENV_FILE')) { $findings.Add("missing-injection-contract:${relative}") }
}
$infraScript=Join-Path $root 'ops/cli/lifecycle/deploy-infra.sh'
if(Test-Path -LiteralPath $infraScript -PathType Leaf){
  $infraText=[IO.File]::ReadAllText($infraScript)
  foreach($key in $requiredInfraKeys){if(-not $infraText.Contains($key)){$findings.Add("missing-infrastructure-secret-contract:$key")}}
}
$provisionScript=Join-Path $root 'ops/cli/lifecycle/provision-infra-remote.sh'
if(Test-Path -LiteralPath $provisionScript -PathType Leaf){
  $provisionText=[IO.File]::ReadAllText($provisionScript)
  foreach($key in $requiredInfraKeys){
    if(-not $provisionText.Contains($key)){ $findings.Add("missing-provisioning-secret-contract:$key") }
  }
}
$template=Join-Path $root 'ops/config/projects/geostat/shared/templates/common.env.example'
if(-not (Test-Path -LiteralPath $template -PathType Leaf)) { $findings.Add('missing:ops/config/projects/geostat/shared/templates/common.env.example') }
$status=if($findings.Count -eq 0){'PASS'}else{'FAIL'}
$result=[ordered]@{schema='geostat.deployment-secret-injection-preflight.v1';generatedAt=[DateTime]::UtcNow.ToString('o');status=$status;checkedEntrypoints=$entrypoints;findings=@($findings);contract='GEOSTAT_ENV_FILE points to an operator-managed, uncommitted env file; repository secret paths are forbidden'}
$resolved=Join-Path (Get-Location) $Output; $parent=Split-Path -Parent $resolved
if(-not (Test-Path -LiteralPath $parent)){New-Item -ItemType Directory -Path $parent -Force|Out-Null}
[IO.File]::WriteAllText($resolved,($result|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
$result|ConvertTo-Json -Depth 8
if($findings.Count -gt 0){exit 1}
