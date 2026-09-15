[CmdletBinding()]
param([string]$Output='build/secret-boundary-acceptance.json')

$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$scope=@(
  (Join-Path $root 'platform/apps/geostat'),
  (Join-Path $root 'ops/infra/geostat-platform'),
  (Join-Path $root 'ops/config/projects/geostat')
)
$extensions=@('.yml','.yaml','.properties','.json','.env','.gradle','.java','.xml')
$allowPattern='(\$\{|<PASSWORD>|<SECRET>|CHANGE_ME|example|placeholder)'
$forbidden=@('Ozzy112358','5A7C8E1F2A3C5E7A8C1E2F3A5C7E8A1F2C3F5A7C8E1F2A3C5E7A8C1E2F3A5C7E')
$findings=[System.Collections.Generic.List[object]]::new()
$secretDirectory=Join-Path $root 'ops/config/projects/geostat/shared/secrets'
if(Test-Path -LiteralPath $secretDirectory){
  Get-ChildItem -LiteralPath $secretDirectory -File -Filter '*.env' -ErrorAction SilentlyContinue | ForEach-Object {
    $findings.Add([ordered]@{file=$_.FullName.Substring($root.Length+1);line=0;reason='repository-secret-file-forbidden'})
  }
}
foreach($base in $scope){
  if(-not(Test-Path -LiteralPath $base)){continue}
  Get-ChildItem -LiteralPath $base -Recurse -File | Where-Object {
    $extensions -contains $_.Extension.ToLowerInvariant() -and
    $_.FullName -notmatch '\\(build|\.gradle|node_modules|templates)\\' -and
    $_.Name -notmatch '\.example$'
  } | ForEach-Object {
    $file=$_
    $lines=@(Get-Content -LiteralPath $file.FullName -ErrorAction Stop)
    for($i=0;$i -lt $lines.Count;$i++){
      $line=$lines[$i]
      foreach($needle in $forbidden){if($line.Contains($needle)){$findings.Add([ordered]@{file=$file.FullName.Substring($root.Length+1);line=$i+1;reason='known-secret-or-private-endpoint'})}}
      if($line -match '(?i)\b(password|secret|access-key|secret-key)\s*:\s*([^$<{\s][^\s#]*)' -and $line -notmatch $allowPattern){
        $findings.Add([ordered]@{file=$file.FullName.Substring($root.Length+1);line=$i+1;reason='literal-secret-bearing-config'})
      }
    }
  }
}
$result=[ordered]@{schema='geostat.secret-boundary-preflight.v1';generatedAt=[DateTime]::UtcNow.ToString('o');status=if($findings.Count -eq 0){'PASS'}else{'FAIL'};findings=@($findings);rule='Runtime/config source may contain only environment references or explicit placeholders; credentials never ship in resources.'}
$resolved=Join-Path $root $Output;$parent=Split-Path -Parent $resolved;if($parent -and -not(Test-Path $parent)){New-Item -ItemType Directory -Path $parent -Force|Out-Null};[IO.File]::WriteAllText($resolved,($result|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false));$result|ConvertTo-Json -Depth 8
if($findings.Count -gt 0){exit 1}
