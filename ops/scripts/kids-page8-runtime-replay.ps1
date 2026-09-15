[CmdletBinding()]
param(
  [string]$BaseUrl = $(if ($env:KIDS_PLATFORM_API_URL) { $env:KIDS_PLATFORM_API_URL } else { 'http://192.168.1.199:8083/api/v1' }),
  [string]$AccessToken = $env:KIDS_APPROVED_TEST_TOKEN,
  [string]$EvidencePath = $(Join-Path $env:TEMP 'kids-page8-runtime-replay.json')
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($AccessToken)) {
  $result = [ordered]@{ status = 'CREDENTIAL_REQUIRED'; baseUrl = $BaseUrl; pageId = 8; anonymousBypass = $false; reason = 'KIDS_APPROVED_TEST_TOKEN is required' }
  $result | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $EvidencePath -Encoding utf8
  Write-Output ($result | ConvertTo-Json -Compress)
  exit 2
}

$headers = @{ Authorization = "Bearer $AccessToken"; Accept = 'application/json' }
$paths = @(
  '/platform/contracts/KIDS_PORTAL_V1/revisions/8/introspection',
  '/platform/contracts/KIDS_PORTAL_V1/pages?revision=8',
  '/platform/contracts/KIDS_PORTAL_V1/pages/8/query-capabilities?revision=8'
)
$body = @{ filters=@{}; sort='source_goal_id'; descending=$false; groupBy=@(); aggregation=$null; page=1; limit=5; select=@('source_goal_id','title_ka','title_en','path_ka','path_en','category_item_ref'); include=@(); cursor=$null; where=@{}; orderBy=@(); distinct=$false; includeLimits=@{} } | ConvertTo-Json -Depth 8
$checks = @()
foreach ($path in $paths) {
  $response = Invoke-WebRequest -Uri ($BaseUrl + $path) -Headers $headers -Method Get -TimeoutSec 30 -UseBasicParsing
  $checks += [ordered]@{ path=$path; status=[int]$response.StatusCode; passed=($response.StatusCode -eq 200) }
}
$query = Invoke-WebRequest -Uri ($BaseUrl + '/platform/contracts/KIDS_PORTAL_V1/pages/8/query') -Headers ($headers + @{ 'Content-Type'='application/json' }) -Method Post -Body $body -TimeoutSec 30 -UseBasicParsing
$checks += [ordered]@{ path='/platform/contracts/KIDS_PORTAL_V1/pages/8/query'; status=[int]$query.StatusCode; passed=($query.StatusCode -eq 200) }
$result = [ordered]@{ status='RUNTIME_REPLAY_PASS'; contractCode='KIDS_PORTAL_V1'; revision=8; pageId=8; checks=$checks; anonymousBypass=$false }
$result | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $EvidencePath -Encoding utf8
Write-Output ($result | ConvertTo-Json -Compress)
