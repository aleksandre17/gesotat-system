[CmdletBinding()]
param(
  [string]$KeycloakBaseUrl = $(if ($env:KIDS_KEYCLOAK_URL) { $env:KIDS_KEYCLOAK_URL } else { 'http://localhost:8080' }),
  [string]$AdminToken = $env:KIDS_KEYCLOAK_ADMIN_TOKEN,
  [string]$TenantId = $(if ($env:KIDS_TEST_TENANT_ID) { $env:KIDS_TEST_TENANT_ID } else { 'kids-staging' }),
  [string]$Username = $(if ($env:KIDS_TEST_USERNAME) { $env:KIDS_TEST_USERNAME } else { 'kids-runtime-test' }),
  [string]$UserPassword = $env:KIDS_TEST_USER_PASSWORD,
  [string]$RedirectUri = $(if ($env:KIDS_TEST_REDIRECT_URI) { $env:KIDS_TEST_REDIRECT_URI } else { 'http://localhost:8765/callback' })
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($AdminToken) -or [string]::IsNullOrWhiteSpace($UserPassword)) { throw 'KIDS_KEYCLOAK_ADMIN_TOKEN and KIDS_TEST_USER_PASSWORD are required; no secret is generated or printed by this script.' }
$realm='geostat'; $adminHeaders=@{Authorization="Bearer $AdminToken"; 'Content-Type'='application/json'}; $admin="$KeycloakBaseUrl/admin/realms/$realm"
function Invoke-Admin($method,$uri,$body=$null) { try { Invoke-WebRequest -Uri $uri -Method $method -Headers $adminHeaders -Body ($body|ConvertTo-Json -Depth 12) -UseBasicParsing -TimeoutSec 30 } catch { if ($_.Exception.Response.StatusCode.value__ -ne 409) { throw } } }
$clientId='kids-runtime-test'; $client=@{clientId=$clientId;name='KIDS staging runtime test';enabled=$true;publicClient=$true;protocol='openid-connect';standardFlowEnabled=$true;directAccessGrantsEnabled=$false;serviceAccountsEnabled=$false;redirectUris=@($RedirectUri);webOrigins=@();defaultClientScopes=@('profile','email','roles','tenant')}
Invoke-Admin POST "$admin/clients" $client | Out-Null
$clientResult=Invoke-RestMethod "$admin/clients?clientId=$clientId" -Headers @{Authorization="Bearer $AdminToken"} -Method Get
if (-not $clientResult -or $clientResult.Count -eq 0) { throw 'OIDC client provisioning could not be verified' }
$role=Invoke-RestMethod "$admin/roles/contract.read" -Headers @{Authorization="Bearer $AdminToken"} -Method Get
$user=@{username=$Username;enabled=$true;emailVerified=$true;credentials=@(@{type='password';value=$UserPassword;temporary=$false});attributes=@{tenant_id=@($TenantId)}}
Invoke-Admin POST "$admin/users" $user | Out-Null
$users=Invoke-RestMethod "$admin/users?username=$Username&exact=true" -Headers @{Authorization="Bearer $AdminToken"} -Method Get
if (-not $users -or $users.Count -eq 0) { throw 'OIDC test user provisioning could not be verified' }
$uid=$users[0].id; Invoke-Admin POST "$admin/users/$uid/role-mappings/realm" @($role) | Out-Null
$verifier=[Convert]::ToBase64String((1..32|ForEach-Object{Get-Random -Maximum 256})); $verifier=$verifier.TrimEnd('=').Replace('+','-').Replace('/','_'); $hash=[Security.Cryptography.SHA256]::Create().ComputeHash([Text.Encoding]::ASCII.GetBytes($verifier)); $challenge=[Convert]::ToBase64String($hash).TrimEnd('=').Replace('+','-').Replace('/','_')
$auth="$KeycloakBaseUrl/realms/$realm/protocol/openid-connect/auth?client_id=$clientId&response_type=code&scope=openid%20profile&redirect_uri=$([uri]::EscapeDataString($RedirectUri))&code_challenge=$challenge&code_challenge_method=S256"
[ordered]@{status='OIDC_TEST_PRINCIPAL_READY';realm=$realm;clientId=$clientId;username=$Username;tenantId=$TenantId;authorizationUrl=$auth;pkceVerifier=$verifier;tokenEndpoint="$KeycloakBaseUrl/realms/$realm/protocol/openid-connect/token";secretOutput='The user password and admin token are never printed'} | ConvertTo-Json -Depth 8
