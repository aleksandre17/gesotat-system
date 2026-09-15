[CmdletBinding()]
param(
  [string]$BaseUrl = $(if ($env:KIDS_PLATFORM_API_URL) { $env:KIDS_PLATFORM_API_URL } else { 'http://192.168.1.199:8083/api/v1' }),
  [string]$AccessToken = $env:KIDS_APPROVED_TEST_TOKEN,
  [string]$EvidencePath = $(Join-Path $env:TEMP 'kids-page11-runtime-replay.json')
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($AccessToken)) {
  $result = [ordered]@{ status = 'CREDENTIAL_REQUIRED'; baseUrl = $BaseUrl; pageId = 11; anonymousBypass = $false; reason = 'KIDS_APPROVED_TEST_TOKEN is required' }
  $result | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $EvidencePath -Encoding utf8
  Write-Output ($result | ConvertTo-Json -Compress)
  exit 2
}

$headers = @{ Authorization = "Bearer $AccessToken"; Accept = 'application/json' }
$checks = @()
foreach ($path in @(
  '/platform/contracts/KIDS_PORTAL_V1/revisions/8/introspection',
  '/platform/contracts/KIDS_PORTAL_V1/pages?revision=8',
  '/platform/contracts/KIDS_PORTAL_V1/pages/11/query-capabilities?revision=8'
)) {
  $response = Invoke-WebRequest -Uri ($BaseUrl + $path) -Headers $headers -Method Get -TimeoutSec 30 -UseBasicParsing
  $checks += [ordered]@{ path=$path; status=[int]$response.StatusCode; passed=($response.StatusCode -eq 200) }
}

$body = @{
  filters=@{}; sort='input_key'; descending=$false; groupBy=@(); aggregation=$null
  page=1; limit=100; select=@('input_key','carrier_code','cell_ordinal','period_raw','period_normalized','dimension_key_raw','age_group_item_ref','value_lexical','value_decimal','json_path','source_encoding','source_row_key','operation')
  include=@('lineage','dimensions'); cursor=$null; where=@{}; orderBy=@(); distinct=$false; includeLimits=@{}
} | ConvertTo-Json -Depth 12
$query = Invoke-WebRequest -Uri ($BaseUrl + '/platform/contracts/KIDS_PORTAL_V1/pages/11/query') -Headers ($headers + @{ 'Content-Type'='application/json' }) -Method Post -Body $body -TimeoutSec 30 -UseBasicParsing
$payload = $query.Content | ConvertFrom-Json
$shapePass = ([int]$query.StatusCode -eq 200 -and $payload.pageId -eq 11 -and $payload.contractRevision -eq 8 -and $payload.datasetCode -eq 'KIDS_STATISTICAL_INPUT' -and [int]$payload.pagination.returned -le [int]$payload.pagination.total)
$checks += [ordered]@{ path='/platform/contracts/KIDS_PORTAL_V1/pages/11/query'; status=[int]$query.StatusCode; returned=[int]$payload.pagination.returned; total=[int]$payload.pagination.total; shapePass=$shapePass; passed=($shapePass -and [int]$payload.pagination.total -eq 880) }
$passed = (($checks | Where-Object { -not $_.passed }).Count -eq 0)
$result = [ordered]@{ status=($(if ($passed) { 'RUNTIME_REPLAY_PASS' } else { 'RUNTIME_REPLAY_FAIL' })); contractCode='KIDS_PORTAL_V1'; revision=8; pageId=11; checks=$checks; anonymousBypass=$false }
$result | ConvertTo-Json -Depth 16 | Set-Content -LiteralPath $EvidencePath -Encoding utf8
Write-Output ($result | ConvertTo-Json -Compress)
if (-not $passed) { exit 1 }
