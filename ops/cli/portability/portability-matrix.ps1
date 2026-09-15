[CmdletBinding()]
param([string]$Output='build/portability-matrix-report.json')
$ErrorActionPreference='Stop'
$profiles=@(
  [ordered]@{ id='sqlserver-default'; provider='SQL_SERVER'; schema='dbo'; key='single'; workload='baseline' },
  [ordered]@{ id='sqlserver-nondefault-composite'; provider='SQL_SERVER'; schema='geostat_alt'; key='composite'; workload='large-table' },
  [ordered]@{ id='mysql-default'; provider='MYSQL'; schema='geostat'; key='single'; workload='baseline' },
  [ordered]@{ id='mysql-nondefault-composite'; provider='MYSQL'; schema='geostat_alt'; key='composite'; workload='large-table' }
)
$results=@()
foreach($profile in $profiles){
  $providerEnv=if($profile.provider -eq 'MYSQL'){'PLATFORM_PORTABILITY_MYSQL_URL'}else{'PLATFORM_PORTABILITY_SQLSERVER_URL'}
  $configured=[bool](Get-Item -Path "Env:$providerEnv" -ErrorAction SilentlyContinue)
  $status=if($configured){'READY_FOR_EXECUTION'}else{'NOT_RUN'}
  $results += [ordered]@{profile=$profile.id;provider=$profile.provider;schema=$profile.schema;key=$profile.key;workload=$profile.workload;status=$status;reason=if($configured){'Endpoint configured; invoke provider-specific integration runner'}else{"$providerEnv is not configured"}}
}
$canonicalProfiles = ($profiles | ConvertTo-Json -Compress -Depth 4)
$matrixHash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes($canonicalProfiles))).ToLowerInvariant()
$report=[ordered]@{schema='geostat.portability-matrix.v1';matrixFingerprint=$matrixHash;generatedAt=[DateTime]::UtcNow.ToString('o');results=$results;passed=0;failed=0;notRun=@($results|Where-Object status -eq 'NOT_RUN').Count;executionContract=[ordered]@{requiredEvidence=@('row-count','checksum','foreign-keys','relation-cardinality','schema-evolution','large-table-capacity');runner='provider-specific integration runner';statusRule='PASS only with endpoint and evidence; otherwise NOT_RUN'};note='This planner is fail-closed: it never claims a provider PASS without a configured endpoint and integration runner evidence.'}
$resolved=Join-Path (Get-Location) $Output; $parent=Split-Path -Parent $resolved;if(-not(Test-Path $parent)){New-Item -ItemType Directory -Path $parent -Force|Out-Null};[IO.File]::WriteAllText($resolved,($report|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false));$report|ConvertTo-Json -Depth 8
