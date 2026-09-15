param(
  [string]$EnvFile = $(if ($env:GEOSTAT_ENV_FILE) { $env:GEOSTAT_ENV_FILE } else { 'ops/config/projects/geostat/shared/runtime/production.env' }),
  [string]$ComposeFile = 'ops/compose/projects/geostat/docker-compose.prod.yml'
)
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $EnvFile)) { throw "Environment file not found: $EnvFile" }
if (-not (Test-Path -LiteralPath $ComposeFile)) { throw "Compose file not found: $ComposeFile" }
$values = @{}
foreach ($line in Get-Content -LiteralPath $EnvFile) {
  if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$' -and $line -notmatch '^\s*#') {
    $values[$Matches[1]] = $Matches[2].Trim().Trim('"').Trim("'")
  }
}
$required = @('DB_PRIMARY_URL','DB_PRIMARY_USER','DB_PRIMARY_PASS','JWT_SECRET','JWT_ISSUER','JWT_AUDIENCE','PLATFORM_CURSOR_SIGNING_SECRET','PLATFORM_CORS_ALLOWED_ORIGINS','PLATFORM_MAX_ACCESS_ARTIFACT_BYTES','PLATFORM_QUERY_COST_MAX','PLATFORM_QUERY_MAX_EXPORT_BYTES','PLATFORM_QUERY_MAX_REQUEST_BYTES','STORAGE_ENDPOINT','STORAGE_ACCESS_KEY','STORAGE_SECRET_KEY','STORAGE_BUCKET_INGEST','STORAGE_BUCKET_QUARANTINE','STORAGE_BUCKET_ARCHIVE','STORAGE_BUCKET_EXPORT','PLATFORM_RATE_LIMIT_REQUESTS','PLATFORM_RATE_LIMIT_WINDOW_MS','PLATFORM_RATE_LIMIT_MAX_CLIENT_KEYS')
$missing = @($required | Where-Object { -not $values.ContainsKey($_) -or [string]::IsNullOrWhiteSpace($values[$_]) })
if ($missing.Count) { throw "Missing required production keys: $($missing -join ', ')" }
$placeholder = @($required | Where-Object { $values[$_] -match '(?i)CHANGE_ME|YOUR_HOST|replace-with|example\.com' })
if ($placeholder.Count) { throw "Placeholder values remain in production keys: $($placeholder -join ', ')" }
if ($values['JWT_SECRET'].Length -lt 64 -or $values['PLATFORM_CURSOR_SIGNING_SECRET'].Length -lt 32) { throw 'Production signing secrets do not meet minimum length policy' }
if ([long]$values['PLATFORM_MAX_ACCESS_ARTIFACT_BYTES'] -lt 1) { throw 'Production artifact size limit must be positive' }
if ([long]$values['PLATFORM_QUERY_MAX_EXPORT_BYTES'] -lt 1) { throw 'Production export size limit must be positive' }
if ([long]$values['PLATFORM_QUERY_MAX_REQUEST_BYTES'] -lt 1) { throw 'Production query request size limit must be positive' }
if ([long]$values['PLATFORM_QUERY_COST_MAX'] -lt 1) { throw 'Production query cost budget must be positive' }
if ($values['PLATFORM_CORS_ALLOWED_ORIGINS'] -match '\*' -or $values['PLATFORM_CORS_ALLOWED_ORIGIN_PATTERNS'] -match '\*') { throw 'Wildcard CORS origins/patterns are forbidden in production' }
if ($values.ContainsKey('PLATFORM_OIDC_ENABLED') -and $values['PLATFORM_OIDC_ENABLED'].ToLowerInvariant() -eq 'true') {
  if (-not $values.ContainsKey('OIDC_ISSUER_URL') -or [string]::IsNullOrWhiteSpace($values['OIDC_ISSUER_URL'])) { throw 'OIDC is enabled but OIDC_ISSUER_URL is missing' }
  if ($values['OIDC_ISSUER_URL'] -notmatch '^https://') { throw 'OIDC_ISSUER_URL must use HTTPS in production' }
  if (-not $values.ContainsKey('OIDC_AUDIENCE') -or [string]::IsNullOrWhiteSpace($values['OIDC_AUDIENCE'])) { throw 'OIDC is enabled but OIDC_AUDIENCE is missing' }
  if (-not $values.ContainsKey('OIDC_ROLES_CLAIM') -or [string]::IsNullOrWhiteSpace($values['OIDC_ROLES_CLAIM'])) { throw 'OIDC is enabled but OIDC_ROLES_CLAIM is missing' }
  if (-not $values.ContainsKey('OIDC_TENANT_CLAIM') -or [string]::IsNullOrWhiteSpace($values['OIDC_TENANT_CLAIM'])) { throw 'OIDC is enabled but OIDC_TENANT_CLAIM is missing' }
  if (-not $values.ContainsKey('OIDC_ROLE_AUTHORITY_MAP') -or [string]::IsNullOrWhiteSpace($values['OIDC_ROLE_AUTHORITY_MAP'])) { throw 'OIDC is enabled but OIDC_ROLE_AUTHORITY_MAP is missing' }
  $seenRoles = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
  foreach ($entry in $values['OIDC_ROLE_AUTHORITY_MAP'].Split(';')) {
    if ($entry -notmatch '^\s*([^=;,\s]+)\s*=\s*([^=;\s,]+(?:\s*,\s*[^=;\s,]+)*)\s*$') { throw 'OIDC_ROLE_AUTHORITY_MAP contains an invalid entry' }
    if (-not $seenRoles.Add($Matches[1])) { throw "OIDC_ROLE_AUTHORITY_MAP contains duplicate role: $($Matches[1])" }
  }
}
if ($values.ContainsKey('PLATFORM_RATE_LIMIT_REDIS_ENABLED') -and $values['PLATFORM_RATE_LIMIT_REDIS_ENABLED'].ToLowerInvariant() -eq 'true') {
  if (-not $values.ContainsKey('PLATFORM_RATE_LIMIT_REDIS_URL') -or [string]::IsNullOrWhiteSpace($values['PLATFORM_RATE_LIMIT_REDIS_URL'])) { throw 'Redis quota is enabled but PLATFORM_RATE_LIMIT_REDIS_URL is missing' }
  if ($values['PLATFORM_RATE_LIMIT_REDIS_URL'] -match '(?i)localhost|127\.0\.0\.1|CHANGE_ME|replace-with') { throw 'Redis quota endpoint is not production-safe' }
  if ($values['PLATFORM_RATE_LIMIT_REDIS_URL'] -notmatch '^rediss?://') { throw 'Redis quota endpoint must use redis:// or rediss:// URI' }
}
if ($values.ContainsKey('OTEL_METRICS_ENABLED') -and $values['OTEL_METRICS_ENABLED'].ToLowerInvariant() -eq 'true') {
  if (-not $values.ContainsKey('OTEL_METRICS_ENDPOINT') -or [string]::IsNullOrWhiteSpace($values['OTEL_METRICS_ENDPOINT'])) { throw 'OTel metrics are enabled but OTEL_METRICS_ENDPOINT is missing' }
  if ($values['OTEL_METRICS_ENDPOINT'] -match '(?i)localhost|127\.0\.0\.1|CHANGE_ME|replace-with') { throw 'OTel metrics endpoint is not production-safe' }
  if ($values['OTEL_METRICS_ENDPOINT'] -notmatch '^https?://') { throw 'OTel metrics endpoint must be an HTTP(S) URI' }
}
$compose = Get-Content -Raw -LiteralPath $ComposeFile
foreach ($key in $required) { if ($compose -notmatch [regex]::Escape($key)) { throw "Compose does not declare required runtime key: $key" } }
foreach ($key in @('PLATFORM_OIDC_ENABLED','OIDC_ISSUER_URL','OIDC_AUDIENCE','OIDC_ROLES_CLAIM','OIDC_TENANT_CLAIM','OIDC_ROLE_AUTHORITY_MAP')) { if ($compose -notmatch [regex]::Escape($key)) { throw "Compose does not declare OIDC runtime key: $key" } }
if ($compose -notmatch 'SPRING_PROFILES_ACTIVE:\s*prod') { throw 'Compose must activate the prod Spring profile' }
[pscustomobject]@{ status='PASS'; envFile=(Resolve-Path $EnvFile).Path; composeFile=(Resolve-Path $ComposeFile).Path; checkedKeys=$required.Count; secrets='validated-without-output' } | ConvertTo-Json -Compress
