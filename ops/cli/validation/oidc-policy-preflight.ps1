[CmdletBinding()]
param(
  [string]$Policy = 'ops/infra/geostat-platform/keycloak/geostat-realm.json',
  [string]$Output = '',
  [string]$TenantClaim = 'tenant_id'
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $Policy -PathType Leaf)) { throw "OIDC policy missing: $Policy" }
$doc = Get-Content -Raw -LiteralPath $Policy | ConvertFrom-Json
if ($doc.realm -ne 'geostat' -or $doc.enabled -ne $true) { throw 'OIDC realm must be enabled and canonical' }
$client = @($doc.clients | Where-Object clientId -eq 'geostat-api') | Select-Object -First 1
if ($null -eq $client) { throw 'Canonical API client is missing' }
$requiredFalse = @('bearerOnly','standardFlowEnabled','directAccessGrantsEnabled','serviceAccountsEnabled')
foreach ($name in $requiredFalse) { if ($client.$name -ne $true -and $name -eq 'bearerOnly') { throw "OIDC client $name must be true" }; if ($name -ne 'bearerOnly' -and $client.$name -eq $true) { throw "OIDC client $name must be false" } }
$roles = @($doc.roles.realm | ForEach-Object name)
foreach ($role in @('contract.read','contract.write','ingest.execute','quality.approve','publish.execute','raw.read','admin')) { if ($roles -notcontains $role) { throw "Required realm role missing: $role" } }
$tenantScopes = @($doc.clientScopes | Where-Object name -eq 'tenant')
if ($tenantScopes.Count -ne 1) { throw 'Exactly one tenant client scope is required' }
$mapper = @($tenantScopes[0].protocolMappers | Where-Object { $_.config.'claim.name' -eq $TenantClaim }) | Select-Object -First 1
if ([string]::IsNullOrWhiteSpace($TenantClaim) -or $null -eq $mapper -or $mapper.config.'access.token.claim' -ne 'true') { throw "${TenantClaim} access-token mapper is incomplete" }
$result = [pscustomobject]@{
  schema='geostat.oidc-policy-preflight.v1'; status='PASS'; realm=$doc.realm; client=$client.clientId
  directAccessGrantsEnabled=[bool]$client.directAccessGrantsEnabled; serviceAccountsEnabled=[bool]$client.serviceAccountsEnabled
  tenantClaim=$TenantClaim; requiredRoles=$roles
  runtimeBinding='NOT_ASSERTED'; note='Static policy contract only; issuer/JWKS runtime and tenant enforcement require protected-token evidence.'
}
$json = $result | ConvertTo-Json -Depth 6
if (-not [string]::IsNullOrWhiteSpace($Output)) {
  $parent = Split-Path -Parent $Output
  if ($parent -and -not (Test-Path -LiteralPath $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
  [IO.File]::WriteAllText([IO.Path]::GetFullPath($Output), $json, [Text.UTF8Encoding]::new($false))
}
$json
