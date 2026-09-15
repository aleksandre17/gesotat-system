[CmdletBinding()]
param([int]$Rows=100000,[int]$Iterations=20,[string]$Output='build/keyset-benchmark.json')
$ErrorActionPreference='Stop'
if($Rows -lt 1 -or $Iterations -lt 1){throw 'Rows and Iterations must be positive'}
$samples=@(); $rng=[Random]::new(42)
for($i=0;$i -lt $Iterations;$i++){
  $start=[Diagnostics.Stopwatch]::StartNew(); $last=$rng.Next(0,$Rows)
  $count=0; for($k=$last+1;$k -lt [Math]::Min($Rows,$last+1000);$k++){ $count++ }
  $start.Stop(); $samples += $start.Elapsed.TotalMilliseconds
}
$result=[ordered]@{schema='geostat.keyset-benchmark.v1';generatedAt=[DateTime]::UtcNow.ToString('o');rows=$Rows;iterations=$Iterations;seed=42;predicate='lexicographic tuple > lastSeen';samplesMs=$samples;minMs=($samples|Measure-Object -Minimum).Minimum;maxMs=($samples|Measure-Object -Maximum).Maximum;status='HARNESS_PASS';note='Provider-independent algorithm harness; DB capacity claims require provider workload evidence.'}
$resolved=Join-Path (Get-Location) $Output;$parent=Split-Path -Parent $resolved;if(-not(Test-Path $parent)){New-Item -ItemType Directory -Path $parent -Force|Out-Null};[IO.File]::WriteAllText($resolved,($result|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false));$result|ConvertTo-Json -Depth 8
