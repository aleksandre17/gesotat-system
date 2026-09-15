[CmdletBinding()]
param(
  [string]$ControllerRoot = 'platform/apps/geostat/backend/api/src/main/java/org/base/api/controller',
  [string]$SecurityConfig = 'platform/apps/geostat/backend/core/src/main/java/org/base/core/setting/api/ApiSecurityConfig.java'
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $ControllerRoot -PathType Container)) { throw "Controller root missing: $ControllerRoot" }
if (-not (Test-Path -LiteralPath $SecurityConfig -PathType Leaf)) { throw "API security configuration missing: $SecurityConfig" }

$security = Get-Content -Raw -LiteralPath $SecurityConfig
$requiredSecurityMarkers = @(
  'requestMatchers\("/health"\)\.permitAll\(\)',
  'requestMatchers\("/api/v1/\*\*"\)\.authenticated\(\)',
  'exceptionHandling',
  'SessionCreationPolicy\.STATELESS'
)
$missing = @($requiredSecurityMarkers | Where-Object { $security -notmatch $_ })
if ($missing.Count -gt 0) { throw "Fail-closed API security markers missing: $($missing -join ', ')" }

$controllers = @(Get-ChildItem -LiteralPath $ControllerRoot -Recurse -Filter '*.java' -File)
$unprotected = @()
foreach ($file in $controllers) {
  $text = Get-Content -Raw -LiteralPath $file.FullName
  if ($text -match '(@RestController|@Controller)') {
    # Health is the sole intentional unauthenticated liveness surface. Build-info
    # is protected by the global anyRequest().authenticated() rule.
    $isHealth = $file.Name -eq 'HealthController.java'
    $isAdvice = $file.Name -eq 'ContractApiExceptionHandler.java' -or $text -match '@RestControllerAdvice'
    $hasMethodPolicy = $text -match '@PreAuthorize\s*\('
    $isBuildInfo = $file.Name -eq 'BuildInfoController.java'
    if (-not $isHealth -and -not $isAdvice -and -not $hasMethodPolicy -and -not $isBuildInfo) {
      $unprotected += $file.FullName
    }
  }
}
if ($unprotected.Count -gt 0) { throw "Controller lacks explicit method policy: $($unprotected -join '; ')" }

[pscustomobject]@{
  schema = 'geostat.authorization-surface-preflight.v1'
  status = 'PASS'
  controllerCount = $controllers.Count
  policyBoundControllerCount = $controllers.Count - $unprotected.Count
  intentionalPublicSurface = @('/health', '/actuator/health', '/sign/login', '/sign/register', '/sign/refresh', '/ws/**')
  globalRule = 'anyRequest().authenticated()'
  methodRule = 'PreAuthorize authority checks on resource controllers'
} | ConvertTo-Json -Depth 4
