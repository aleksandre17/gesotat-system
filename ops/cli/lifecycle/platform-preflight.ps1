param(
    [string]$EnvironmentFile = $(if ($env:GEOSTAT_ENV_FILE) { $env:GEOSTAT_ENV_FILE } else { "ops/config/projects/geostat/shared/runtime/production.env" })
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $EnvironmentFile)) { throw "Environment file not found: $EnvironmentFile" }
$cfg = @{}
Get-Content -LiteralPath $EnvironmentFile | ForEach-Object {
    if ($_ -match '^\s*([^#=\s]+)\s*=\s*(.*)$') { $cfg[$matches[1]] = $matches[2].Trim() }
}
$required = 'DB_PRIMARY_URL','DB_PRIMARY_USER','DB_PRIMARY_PASS','DB_DATA_URL','DB_DATA_USER','DB_DATA_PASS','DB_ARCHIVE_URL','DB_ARCHIVE_USER','DB_ARCHIVE_PASS','STORAGE_ENDPOINT','STORAGE_ACCESS_KEY','STORAGE_SECRET_KEY','KIDS_SQL_PASSWORD'
$missing = $required | Where-Object {
    $value = if ($cfg.ContainsKey($_)) { $cfg[$_] } else { [Environment]::GetEnvironmentVariable($_) }
    [string]::IsNullOrWhiteSpace($value)
}
if ($missing.Count -gt 0) {
    Write-Output "PREFLIGHT=NOT_READY"
    $missing | ForEach-Object { Write-Output "MISSING=$_" }
    exit 2
}
Add-Type -AssemblyName System.Data
function Test-Sql([string]$name, [string]$jdbc, [string]$user, [string]$pass) {
    $server = $jdbc -replace '^jdbc:sqlserver://','' -replace ';.*$',''
    $database = if ($jdbc -match 'databaseName=([^;]+)') { $matches[1] } else { throw "$name JDBC URL lacks databaseName" }
    $cs = "Server=$server;Database=$database;User ID=$user;Password=$pass;TrustServerCertificate=True;Encrypt=True"
    $connection = [System.Data.SqlClient.SqlConnection]::new($cs)
    try { $connection.Open(); $command=$connection.CreateCommand(); $command.CommandText='SELECT 1'; [void]$command.ExecuteScalar(); Write-Output "SQL_$name=UP" }
    finally { $connection.Dispose() }
}
Test-Sql 'CONTROL' $cfg.DB_PRIMARY_URL $cfg.DB_PRIMARY_USER $cfg.DB_PRIMARY_PASS
Test-Sql 'DATA' $cfg.DB_DATA_URL $cfg.DB_DATA_USER $cfg.DB_DATA_PASS
Test-Sql 'ARCHIVE' $cfg.DB_ARCHIVE_URL $cfg.DB_ARCHIVE_USER $cfg.DB_ARCHIVE_PASS
try { $response = Invoke-WebRequest -Uri ($cfg.STORAGE_ENDPOINT.TrimEnd('/') + '/minio/health/live') -UseBasicParsing -TimeoutSec 10; Write-Output "STORAGE=UP ($($response.StatusCode))" }
catch { Write-Output 'STORAGE=DOWN'; exit 3 }
Write-Output 'PREFLIGHT=READY'
