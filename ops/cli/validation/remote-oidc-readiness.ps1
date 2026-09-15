[CmdletBinding()]
param([string]$HostName='192.168.1.199',[string]$User='administrator',[string]$Output='build/remote-oidc-readiness.json')
$ErrorActionPreference='Stop'
$checks=[System.Collections.Generic.List[object]]::new()
function Add-Check([string]$name,[string]$status,[string]$detail){$checks.Add([ordered]@{name=$name;status=$status;detail=$detail})}
try {
  $discoveryText=((& ssh -o BatchMode=yes -o ConnectTimeout=5 "$User@$HostName" "docker exec geostat-api sh -c 'wget -qO- --timeout=8 http://geostat-keycloak:8080/realms/geostat/.well-known/openid-configuration'") 2>&1 | Out-String).Trim()
  if($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($discoveryText)){throw 'OIDC discovery request failed'}
  $discovery=$discoveryText|ConvertFrom-Json
  if($discovery.issuer -notmatch '/realms/geostat$'){throw "Unexpected issuer realm: $($discovery.issuer)"}
  if($discovery.jwks_uri -notmatch '/realms/geostat/protocol/openid-connect/certs$'){throw 'Unexpected JWKS URI'}
  Add-Check 'oidc-discovery' 'PASS' "issuer=$($discovery.issuer); jwks_uri=$($discovery.jwks_uri)"
  $jwksText=((& ssh -o BatchMode=yes -o ConnectTimeout=5 "$User@$HostName" "docker exec geostat-api sh -c 'wget -qO- --timeout=8 $($discovery.jwks_uri)'") 2>&1 | Out-String).Trim()
  if($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($jwksText)){throw 'JWKS request failed'}
  $jwks=$jwksText|ConvertFrom-Json; $keys=@($jwks.keys)
  $signing=@($keys|Where-Object { $_.use -eq 'sig' -and $_.kty -eq 'RSA' -and $_.alg -eq 'RS256' })
  if($signing.Count -lt 1){throw 'No RSA RS256 signing key in JWKS'}
  Add-Check 'jwks-signing-key' 'PASS' "rsa-rs256-keys=$($signing.Count)"
} catch { Add-Check 'oidc-runtime' 'FAIL' $_.Exception.Message }
$failed=@($checks|Where-Object status -eq 'FAIL').Count
$result=[ordered]@{schema='geostat.remote-oidc-readiness.v1';generatedAt=[DateTime]::UtcNow.ToString('o');host=$HostName;readOnly=$true;checks=@($checks);passed=@($checks|Where-Object status -eq 'PASS').Count;failed=$failed;status=if($failed){'FAIL'}else{'PASS'};scope='In-network OIDC discovery/JWKS metadata only; public key material and tokens are never persisted'}
$resolved=Join-Path (Get-Location) $Output;$parent=Split-Path -Parent $resolved;if($parent -and -not(Test-Path $parent)){New-Item -ItemType Directory -Path $parent -Force|Out-Null};[IO.File]::WriteAllText($resolved,($result|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false));$result|ConvertTo-Json -Depth 8
if($failed){exit 1}
