[CmdletBinding()]
param(
  [int]$Port = 18765,
  [string]$IssuerBase = 'https://keycloak:8443',
  [string]$VerifierPath = "$env:TEMP\kids-pkce-verifier.txt",
  [string]$TokenPath = "$env:TEMP\kids-approved-test-token.txt"
)
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $VerifierPath)) { throw "Missing PKCE verifier: $VerifierPath" }
$verifier=(Get-Content -LiteralPath $VerifierPath -Raw).Trim(); $redirect="http://localhost:$Port/callback"
$bytes=New-Object byte[] 32; $rng=[Security.Cryptography.RandomNumberGenerator]::Create(); $rng.GetBytes($bytes); $rng.Dispose(); $newVerifier=[Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+','-').Replace('/','_'); $hash=[Security.Cryptography.SHA256]::Create().ComputeHash([Text.Encoding]::ASCII.GetBytes($newVerifier)); $challenge=[Convert]::ToBase64String($hash).TrimEnd('=').Replace('+','-').Replace('/','_'); Set-Content -LiteralPath $VerifierPath -Value $newVerifier -NoNewline -Encoding ascii
$url="$IssuerBase/realms/geostat/protocol/openid-connect/auth?client_id=kids-runtime-test&response_type=code&redirect_uri=$([uri]::EscapeDataString($redirect))&code_challenge=$challenge&code_challenge_method=S256"
$listener=[Net.HttpListener]::new(); $listener.Prefixes.Add("http://localhost:$Port/"); $listener.Start(); Write-Output "Open this URL and complete login:`n$url"; Write-Output 'Waiting for callback...'
$context=$listener.GetContext(); $query=$context.Request.QueryString; $code=$query['code']; if ([string]::IsNullOrWhiteSpace($code)) { throw "OIDC callback error: $($query['error']) $($query['error_description'])" }
$form="grant_type=authorization_code&client_id=kids-runtime-test&code=$([uri]::EscapeDataString($code))&redirect_uri=$([uri]::EscapeDataString($redirect))&code_verifier=$([uri]::EscapeDataString($newVerifier))"; $token=Invoke-RestMethod -Uri "$IssuerBase/realms/geostat/protocol/openid-connect/token" -Method Post -ContentType 'application/x-www-form-urlencoded' -Body $form
if ([string]::IsNullOrWhiteSpace($token.access_token)) { throw 'OIDC token response did not contain access_token' }
Set-Content -LiteralPath $TokenPath -Value $token.access_token -NoNewline -Encoding ascii; $reply=[Text.Encoding]::UTF8.GetBytes('Login complete. You may close this tab.'); $context.Response.OutputStream.Write($reply,0,$reply.Length); $context.Response.Close(); $listener.Stop(); Write-Output "TOKEN_READY=$TokenPath"
