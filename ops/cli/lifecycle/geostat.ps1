# Project entrypoint for the pinned delivery kit (geostat.ops.json -> package).
# Resolves the kit from the manifest so the kit location is declared once.
param([Parameter(ValueFromRemainingArguments = $true)][string[]]$CliArgs = @())

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
$manifest = Get-Content (Join-Path $root "geostat.ops.json") -Raw | ConvertFrom-Json
$kitCli = Join-Path (Join-Path $root $manifest.package) "cli\geostat.ps1"
if (-not (Test-Path $kitCli)) {
    Write-Host "  [ERROR] kit CLI not found: $kitCli (git submodule update --init)" -ForegroundColor Red
    exit 1
}
$env:GEOSTAT_PROJECT_ROOT = $root
& $kitCli @CliArgs
exit $LASTEXITCODE
