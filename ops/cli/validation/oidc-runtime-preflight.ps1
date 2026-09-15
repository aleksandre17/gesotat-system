[CmdletBinding()]
param([string]$Output='build/oidc-runtime-acceptance.json')
$ErrorActionPreference='Stop'
$issuer=$env:OIDC_ISSUER_URL
$jwks=$env:OIDC_JWKS_URL
$checks=[System.Collections.Generic.List[object]]::new()
if([string]::IsNullOrWhiteSpace($issuer) -or [string]::IsNullOrWhiteSpace($jwks)) {
  $result=[ordered]@{schema='geostat.oidc-runtime-preflight.v1';status='NOT_RUN';reason='OIDC_ISSUER_URL and OIDC_JWKS_URL are operator-supplied; no runtime values were provided';checks=@()}
} else {
  foreach($item in @(@{name='issuer';url=$issuer},@{name='jwks';url=$jwks})) {
    if($item.url -notmatch '^https://'){throw "$($item.name) must use HTTPS"}
    try { $response=Invoke-WebRequest -UseBasicParsing -Uri $item.url -Method Get -TimeoutSec 10; if([int]$response.StatusCode -ne 200){throw "HTTP $($response.StatusCode)"}; $checks.Add([ordered]@{name=$item.name;status='PASS';http=[int]$response.StatusCode}) }
    catch { $checks.Add([ordered]@{name=$item.name;status='FAIL';error=$_.Exception.Message}) }
  }
  $jwksResponse=$checks|Where-Object name -eq 'jwks'|Select-Object -First 1
  if($jwksResponse.status -eq 'PASS') {
    try { $body=Invoke-RestMethod -Uri $jwks -Method Get -TimeoutSec 10; if($null -eq $body.keys -or @($body.keys).Count -lt 1){throw 'JWKS response has no keys'}; $checks.Add([ordered]@{name='jwks-schema';status='PASS';keyCount=@($body.keys).Count}) }
    catch { $checks.Add([ordered]@{name='jwks-schema';status='FAIL';error=$_.Exception.Message}) }
  }
  $result=[ordered]@{schema='geostat.oidc-runtime-preflight.v1';status=if(@($checks|Where-Object status -eq 'FAIL').Count){'FAIL'}else{'PASS'};issuer=$issuer;jwks=$jwks;checks=@($checks);security='URLs only; no tokens or secret values are persisted'}
}
$resolved=Join-Path (Get-Location) $Output; $parent=Split-Path -Parent $resolved
if(-not (Test-Path -LiteralPath $parent)){New-Item -ItemType Directory -Path $parent -Force|Out-Null}
[IO.File]::WriteAllText($resolved,($result|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
$result|ConvertTo-Json -Depth 8
if($result.status -eq 'FAIL'){exit 1}
