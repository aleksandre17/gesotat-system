param([string]$AuditFile = 'docs/platform-capability-and-architecture-audit-2026-09-13.md')
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $AuditFile)) { throw "Audit file not found: $AuditFile" }
$text = Get-Content -Raw -LiteralPath $AuditFile
$checked = ([regex]::Matches($text, '(?m)^- \[x\]')).Count
$open = ([regex]::Matches($text, '(?m)^- \[ \]')).Count
$m = @([regex]::Matches($text, '\*\*(\d+) verified items\*\*, \*\*(\d+) open items\*\*')) | Select-Object -First 1
if (-not $m.Success) { throw 'Declared checklist count is missing' }
$declaredChecked = [int]$m.Groups[1].Value; $declaredOpen = [int]$m.Groups[2].Value
if ($checked -ne $declaredChecked -or $open -ne $declaredOpen) { throw "Checklist count mismatch: declared $declaredChecked/$declaredOpen, actual $checked/$open" }
[pscustomobject]@{ status='PASS'; checked=$checked; open=$open; auditFile=(Resolve-Path $AuditFile).Path } | ConvertTo-Json -Compress
