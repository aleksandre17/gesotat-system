param(
  [string]$HostName = '192.168.1.199',
  [string]$User = 'administrator',
  [int]$ApiPort = 8083,
  [string]$ContractCode = '',
  [string]$Output = ''
)
$ErrorActionPreference = 'Stop'

function result([string]$name, [string]$status, [string]$detail) {
  [ordered]@{ name=$name; status=$status; detail=$detail }
}

$checks = [System.Collections.Generic.List[object]]::new()
$remoteCommand = "docker ps --format '{{.Names}}|{{.Status}}'"
$inventory = ''
try {
  $inventory = (& ssh '-o' 'BatchMode=yes' '-o' 'ConnectTimeout=5' "$User@$HostName" $remoteCommand) 2>&1 | Out-String
  if ($LASTEXITCODE -ne 0) { throw "ssh exit $LASTEXITCODE" }
  $required = @('geostat-api','geostat-redis','geostat-keycloak','geostat-otel-collector','geostat-minio')
  foreach ($name in $required) {
    $line = @($inventory -split "`r?`n" | Where-Object { $_ -match "^$([regex]::Escape($name))\|" }) | Select-Object -First 1
    if ($line) { $checks.Add((result "container:$name" 'PASS' $line.Trim())) }
    else { $checks.Add((result "container:$name" 'FAIL' 'Required GEOSOTAT container is not running')) }
  }
} catch {
  $checks.Add((result 'ssh-inventory' 'NOT_RUN' $_.Exception.Message))
}

$base = "http://$HostName`:$ApiPort"
$probes = [System.Collections.Generic.List[object]]::new()
$probes.Add(@{ name='api-health'; uri="$base/health"; expected=200 })
if ($ContractCode) {
  $protectedUri="$base/api/v1/platform/contracts/$([uri]::EscapeDataString($ContractCode))/pages/11/query-capabilities"
  $probes.Add(@{ name='protected-contract-without-token'; uri=$protectedUri; expected=401 })
  $probes.Add(@{ name='protected-contract-with-malformed-token'; uri=$protectedUri; expected=401; headers=@{ Authorization='Bearer malformed.invalid.token' } })
}
foreach ($probe in $probes) {
  try {
    $requestParams=@{UseBasicParsing=$true;Uri=$probe.uri;Method='Get';TimeoutSec=8}
    if($probe.headers){$requestParams.Headers=$probe.headers}
    $response = Invoke-WebRequest @requestParams
    $code = [int]$response.StatusCode
  } catch {
    $code = 0
    if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode.value__ }
  }
  if ($code -eq $probe.expected) { $checks.Add((result $probe.name 'PASS' "HTTP $code")) }
  elseif ($code -eq 0) { $checks.Add((result $probe.name 'NOT_RUN' 'Endpoint unavailable')) }
  else { $checks.Add((result $probe.name 'FAIL' "Expected HTTP $($probe.expected), got $code")) }
}

$failed = @($checks | Where-Object status -eq 'FAIL').Count
$notRun = @($checks | Where-Object status -eq 'NOT_RUN').Count
$report = [ordered]@{
  schema='geostat.remote-staging-smoke.v1'
  host=$HostName
  apiPort=$ApiPort
  readOnly=$true
  scope='GEOSOTAT containers and API probes only; no mutation'
  checks=$checks
  passed=@($checks | Where-Object status -eq 'PASS').Count
  failed=$failed
  notRun=$notRun
  status=if ($failed -gt 0) { 'FAIL' } elseif ($notRun -gt 0) { 'NOT_RUN' } else { 'PASS' }
}
$json = $report | ConvertTo-Json -Depth 6
if ($Output) {
  $parent = Split-Path -Parent $Output
  if ($parent -and -not (Test-Path -LiteralPath $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
  Set-Content -LiteralPath $Output -Value $json -Encoding UTF8
}
$json
if ($failed -gt 0) { exit 1 }
