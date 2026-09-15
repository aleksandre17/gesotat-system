param([string]$SourceRoot='platform/apps/geostat/backend/api/src/main/java')
$ErrorActionPreference='Stop'
if (-not (Test-Path -LiteralPath $SourceRoot)) { throw "Source root not found: $SourceRoot" }
$files = Get-ChildItem -LiteralPath $SourceRoot -Recurse -Filter '*.java' | Where-Object { $_.FullName -notmatch '\\src\\test\\' }
$violations = @()
foreach ($file in $files) {
  $lineNo=0
  foreach ($line in Get-Content -LiteralPath $file.FullName) {
    $lineNo++
    if ($line -match 'KIDS_PORTAL_V1|defaultValue\s*=\s*"KIDS_|"KIDS_[A-Z0-9_]+"') {
      $violations += [ordered]@{ file=$file.FullName; line=$lineNo; rule='NO_SITE_LITERAL'; text=$line.Trim() }
    }
  }
}
$report=[ordered]@{ schema='geostat.schema-agnostic-runtime-preflight.v1'; status=if($violations.Count){'FAIL'}else{'PASS'}; sourceRoot=(Resolve-Path $SourceRoot).Path; filesScanned=$files.Count; violations=$violations; rule='Runtime API code must resolve site/contract identity from governance metadata, never KIDS literals' }
$report | ConvertTo-Json -Depth 6
if($violations.Count){exit 1}
