param([string]$EnvironmentFile = $(if ($env:GEOSTAT_ENV_FILE) { $env:GEOSTAT_ENV_FILE } else { "ops/config/projects/geostat/shared/runtime/production.env" }))

$ErrorActionPreference = 'Stop'
$projectRoot=(Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
if (-not [IO.Path]::IsPathRooted($EnvironmentFile)) { $EnvironmentFile=Join-Path $projectRoot $EnvironmentFile }
if (-not (Test-Path -LiteralPath $EnvironmentFile)) { throw "Environment file not found: $EnvironmentFile" }
$cfg = @{}
Get-Content -LiteralPath $EnvironmentFile | ForEach-Object { if ($_ -match '^\s*([^#=\s]+)\s*=\s*(.*)$') { $cfg[$matches[1]]=$matches[2].Trim() } }
foreach($key in 'DB_PRIMARY_URL','DB_PRIMARY_USER','DB_PRIMARY_PASS','DB_DATA_URL','DB_DATA_USER','DB_DATA_PASS','DB_ARCHIVE_URL','DB_ARCHIVE_USER','DB_ARCHIVE_PASS') { if([string]::IsNullOrWhiteSpace($cfg[$key])) { throw "Missing $key" } }
Add-Type -AssemblyName System.Data
function Open-Connection([string]$jdbc,[string]$user,[string]$pass,[string]$database) {
  $server=$jdbc -replace '^jdbc:sqlserver://','' -replace ';.*$',''
  $connection=[System.Data.SqlClient.SqlConnection]::new("Server=$server;Database=$database;User ID=$user;Password=$pass;TrustServerCertificate=True;Encrypt=True")
  $connection.Open(); return $connection
}
function Run-Script([System.Data.SqlClient.SqlConnection]$connection,[string]$path) {
  $resolved=(Resolve-Path $path).Path
  $checksum=(Get-FileHash -LiteralPath $resolved -Algorithm SHA256).Hash.ToLowerInvariant()
  $recorded=$null
  try { $probe=$connection.CreateCommand(); $probe.CommandText='SELECT checksum FROM platform.schema_migration WHERE migration_id=@id'; [void]$probe.Parameters.AddWithValue('@id',$path); $recorded=$probe.ExecuteScalar() } catch { $recorded=$null }
  if($null -ne $recorded) {
    if([string]$recorded -ne $checksum) { throw "Schema migration checksum changed: $path. Create a new numbered migration." }
    Write-Output "SKIPPED=$path"; return
  }
  $command=$connection.CreateCommand(); $command.CommandText=[IO.File]::ReadAllText($resolved); [void]$command.ExecuteNonQuery()
  try { $record=$connection.CreateCommand(); $record.CommandText='INSERT INTO platform.schema_migration(migration_id,checksum) VALUES(@id,@checksum)'; [void]$record.Parameters.AddWithValue('@id',$path); [void]$record.Parameters.AddWithValue('@checksum',$checksum); [void]$record.ExecuteNonQuery() } catch { if($path -ne 'core/src/main/resources/db/platform/000_create_platform_databases.sql') { throw } }
  Write-Output "APPLIED=$path"
}
$primaryDb=if($cfg.DB_PRIMARY_URL -match 'databaseName=([^;]+)'){$matches[1]}else{throw 'DB_PRIMARY_URL lacks databaseName'}
$backendRoot=Join-Path $projectRoot 'platform/apps/geostat/backend'
Set-Location $backendRoot
$master=Open-Connection $cfg.DB_PRIMARY_URL $cfg.DB_PRIMARY_USER $cfg.DB_PRIMARY_PASS 'master'
try { Run-Script $master 'core/src/main/resources/db/platform/000_create_platform_databases.sql' } finally { $master.Dispose() }
$control=Open-Connection $cfg.DB_PRIMARY_URL $cfg.DB_PRIMARY_USER $cfg.DB_PRIMARY_PASS $primaryDb
$dataDb=if($cfg.DB_DATA_URL -match 'databaseName=([^;]+)'){$matches[1]}else{throw 'DB_DATA_URL lacks databaseName'}
$archiveDb=if($cfg.DB_ARCHIVE_URL -match 'databaseName=([^;]+)'){$matches[1]}else{throw 'DB_ARCHIVE_URL lacks databaseName'}
$data=Open-Connection $cfg.DB_DATA_URL $cfg.DB_DATA_USER $cfg.DB_DATA_PASS $dataDb
$archive=Open-Connection $cfg.DB_ARCHIVE_URL $cfg.DB_ARCHIVE_USER $cfg.DB_ARCHIVE_PASS $archiveDb
try { Run-Script $control 'core/src/main/resources/db/platform/001_control_plane.sql'; Run-Script $data 'core/src/main/resources/db/platform/002_data_plane.sql'; Run-Script $archive 'core/src/main/resources/db/platform/003_archive_plane.sql'; Run-Script $control 'core/src/main/resources/db/platform/004_kids_pilot_seed.sql'; Run-Script $data 'core/src/main/resources/db/platform/005_rejected_artifact_quarantine.sql'; Run-Script $data 'core/src/main/resources/db/platform/006_entity_localization_and_locator.sql'; Run-Script $control 'core/src/main/resources/db/platform/007_metric_alias.sql'; Run-Script $control 'core/src/main/resources/db/platform/008_kids_semantic_gate.sql'; Run-Script $control 'core/src/main/resources/db/platform/009_sdg_goal_draft_classification.sql'; Run-Script $control 'core/src/main/resources/db/platform/010_kids_logical_dataset_drafts.sql'; Run-Script $control 'core/src/main/resources/db/platform/011_contract_stable_identity.sql'; Run-Script $control 'core/src/main/resources/db/platform/012_kids_site_level_contract.sql'; Run-Script $control 'core/src/main/resources/db/platform/013_kids_statistical_provisional_contract.sql'; Run-Script $control 'core/src/main/resources/db/platform/014_kids_provisional_approval.sql'; Run-Script $data 'core/src/main/resources/db/platform/015_resumable_access_ingest.sql'; Run-Script $control 'core/src/main/resources/db/platform/016_contract_raw_ingest_scope.sql'; Run-Script $control 'core/src/main/resources/db/platform/017_kids_canonical_access_contract.sql'; Run-Script $data 'core/src/main/resources/db/platform/018_package_ingest_idempotency.sql'; Run-Script $control 'core/src/main/resources/db/platform/019_classifier_proposal_registry.sql'; Run-Script $control 'core/src/main/resources/db/platform/020_kids_r4_row_transport_contract.sql'; Run-Script $control 'core/src/main/resources/db/platform/021_kids_r5_artifact_raw_contract.sql'; Run-Script $control 'core/src/main/resources/db/platform/022_kids_complete_site_contract.sql'; Run-Script $control 'core/src/main/resources/db/platform/023_kids_inferred_statistical_semantics.sql'; Write-Output 'PLATFORM_BOOTSTRAP=READY' }
finally { $control.Dispose(); $data.Dispose(); $archive.Dispose() }
