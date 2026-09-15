[CmdletBinding()]
param(
  [string]$Server = 'administrator@192.168.1.199',
  [int]$LocalPort = 18080,
  [int]$RemotePort = 8080
)
$ErrorActionPreference = 'Stop'
$containerIp = (ssh -o BatchMode=yes $Server "docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' geostat-keycloak").Trim()
if ($containerIp -notmatch '^\d{1,3}(\.\d{1,3}){3}$') { throw 'Could not resolve the isolated geostat-keycloak container address' }
Write-Output "OIDC tunnel ready at http://127.0.0.1:$LocalPort (target is isolated geostat-keycloak:$RemotePort; no public port is opened)"
Write-Output 'Keep this window open during the PKCE login.'
ssh -o ExitOnForwardFailure=yes -N -L "${LocalPort}:${containerIp}:${RemotePort}" $Server
