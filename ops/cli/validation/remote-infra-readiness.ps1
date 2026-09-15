[CmdletBinding()]
param(
  [string]$HostName='192.168.1.199',
  [string]$User='administrator',
  [string]$Output='build/remote-infra-readiness.json'
)
$ErrorActionPreference='Stop'
$checks=[System.Collections.Generic.List[object]]::new()
function Add-Check([string]$name,[string]$status,[string]$detail){$checks.Add([ordered]@{name=$name;status=$status;detail=$detail})}
try {
  $redis = (& ssh -o BatchMode=yes -o ConnectTimeout=5 "$User@$HostName" 'docker exec geostat-redis sh -c ''redis-cli -a "$GEOSTAT_REDIS_PASSWORD" ping 2>/dev/null''') 2>&1 | Out-String
  if($LASTEXITCODE -ne 0 -or $redis.Trim() -notmatch 'PONG'){throw "Redis probe failed: $($redis.Trim())"}
  Add-Check 'redis-authenticated-ping' 'PASS' 'PONG (secret omitted)'
} catch { Add-Check 'redis-authenticated-ping' 'FAIL' $_.Exception.Message }
try {
  $tcp = (& ssh -o BatchMode=yes -o ConnectTimeout=5 "$User@$HostName" "docker exec geostat-keycloak bash -lc 'echo > /dev/tcp/127.0.0.1/9000'") 2>&1 | Out-String
  if($LASTEXITCODE -ne 0){throw "Keycloak readiness probe failed: $($tcp.Trim())"}
  Add-Check 'keycloak-internal-readiness' 'PASS' 'TCP 9000 open inside isolated container'
} catch { Add-Check 'keycloak-internal-readiness' 'FAIL' $_.Exception.Message }
try {
  $oidc = (& ssh -o BatchMode=yes -o ConnectTimeout=5 "$User@$HostName" "docker exec geostat-api sh -c 'wget -qO- --timeout=5 http://geostat-keycloak:8080/realms/geostat/.well-known/openid-configuration'" ) 2>&1 | Out-String
  if($LASTEXITCODE -ne 0 -or $oidc.Trim() -notmatch 'jwks_uri'){throw "OIDC discovery failed or jwks_uri is absent"}
  Add-Check 'oidc-discovery-from-api-network' 'PASS' 'Keycloak realm discovery reachable; response contains jwks_uri (payload omitted)'
} catch { Add-Check 'oidc-discovery-from-api-network' 'FAIL' $_.Exception.Message }
try {
  $otel = (& ssh -o BatchMode=yes -o ConnectTimeout=5 "$User@$HostName" "docker inspect geostat-otel-collector --format '{{.State.Status}}'") 2>&1 | Out-String
  if($LASTEXITCODE -ne 0 -or $otel.Trim() -ne 'running'){throw "OTel Collector is not running: $($otel.Trim())"}
  Add-Check 'otel-collector-running' 'PASS' 'isolated geostat-otel-collector container is running'
} catch { Add-Check 'otel-collector-running' 'FAIL' $_.Exception.Message }
$failed=@($checks|Where-Object status -eq 'FAIL').Count
$result=[ordered]@{schema='geostat.remote-infra-readiness.v1';generatedAt=[DateTime]::UtcNow.ToString('o');host=$HostName;readOnly=$true;checks=@($checks);passed=@($checks|Where-Object status -eq 'PASS').Count;failed=$failed;status=if($failed){'FAIL'}else{'PASS'};scope='GEOSOTAT Redis and Keycloak containers only; no credentials or mutations'}
$resolved=Join-Path (Get-Location) $Output;$parent=Split-Path -Parent $resolved;if($parent -and -not(Test-Path $parent)){New-Item -ItemType Directory -Path $parent -Force|Out-Null};[IO.File]::WriteAllText($resolved,($result|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false));$result|ConvertTo-Json -Depth 8
if($failed){exit 1}
