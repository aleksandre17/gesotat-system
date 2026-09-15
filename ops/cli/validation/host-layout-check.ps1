[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$required = @(
  'agent-framework', 'agent-framework/kit', '.agents/kit', '.agents/project', '.agents/project/project.json',
  '.agents/project/profile.json', '.agents/project/kit.lock.json', '.agents/project/roster.json',
  'platform/apps/geostat/backend', 'platform/apps/geostat/frontend', 'platform/apps/geostat/frontend/geostat-system-app', 'platform/apps/geostat/backend/api/Dockerfile', 'platform/apps/geostat/backend/mobile/Dockerfile', 'platform/packages', 'platform/apps', 'platform/apps/REGISTRY.json', 'platform/kits', 'platform/tools',
  'ops/compose/projects/geostat/services/control-plane-ui',
  'ops/config/projects', 'ops/config/projects/REGISTRY.json',
  'platform/data', 'platform/e2e/journey', 'ops/infra', 'ops/compose', 'ops/compose/projects/geostat', 'ops/compose/projects/geostat/services/api', 'ops/compose/projects/geostat/services/mobile', 'ops/compose/projects/geostat/services/web', 'ops/compose/projects/geostat/services/infra', 'ops/compose/projects/geostat/environments', 'ops/compose/projects/geostat/docker-compose.dev.yml', 'ops/compose/projects/geostat/docker-compose.prod.yml', 'ops/config', 'ops/config/schema', 'ops/config/projects/geostat/shared/templates', 'ops/config/projects/geostat/shared/secrets', 'ops/config/projects/geostat/services/api', 'ops/config/projects/geostat/services/mobile', 'ops/config/projects/geostat/services/web', 'ops/config/projects/geostat/services/infra', 'ops/runtime/projects/geostat', 'ops/runtime/projects/geostat/services/api', 'ops/runtime/projects/geostat/services/mobile', 'ops/runtime/projects/geostat/services/web', 'ops/runtime/projects/geostat/services/infra', 'ops/runtime/projects/geostat/receipts', 'ops/runtime/projects/geostat/telemetry', 'ops/runtime/projects/geostat/ground', 'ops/runtime/projects/geostat/guards', 'ops/runbook', 'ops/cli', 'ops/tests',
  'ops/config/projects/geostat/services/control-plane-ui', 'ops/runtime/projects/geostat/services/control-plane-ui',
  'ops/scripts/shell', 'ops/scripts/powershell', 'ops/scripts/python', 'ops/scripts/java', 'ops/scripts/shared',
  'docs/intent', 'docs/decisions', 'docs/reference', 'docs/guides', 'docs/work/cards', 'docs/work/evidence', 'docs/archive'
  ,'docs/reference/canonical-directory-blueprint.md', 'docs/reference/CANONICAL-FULL-TREE.md', 'docs/reference/CANONICAL-FULL-TREE-DESIGN.md', 'docs/reference/ENGINEERING-QUALITY-DOCTRINE.md'
)
foreach ($path in $required) {
  if (-not (Test-Path -LiteralPath (Join-Path -Path $root -ChildPath ([string]$path)))) { throw "Host layout missing: $path" }
}
$upstreamKit = Join-Path $root 'agent-framework/kit'
if (@(Get-ChildItem -Force -LiteralPath $upstreamKit -ErrorAction SilentlyContinue).Count -ne 0) { throw 'agent-framework/kit must remain empty' }
$envTemplate = Join-Path $root 'ops/config/projects/geostat/shared/templates/common.env.example'
if (-not (Test-Path -LiteralPath $envTemplate -PathType Leaf)) { throw 'Canonical environment template missing' }
foreach ($legacyEnv in @('.env.dev','.env.prod','.env.example')) {
  if (Test-Path -LiteralPath (Join-Path $root $legacyEnv)) { throw "Root environment file is forbidden: $legacyEnv" }
}
foreach ($forbiddenRoot in @('db','tmp','BOOT-INF')) {
  if (Test-Path -LiteralPath (Join-Path $root $forbiddenRoot)) { throw "Root build/source boundary is forbidden: $forbiddenRoot" }
}
$cliRoot = Join-Path $root 'ops/cli'
$unexpectedCliFiles = @(Get-ChildItem -Force -LiteralPath $cliRoot -File | Where-Object Name -ne '.gitkeep')
if ($unexpectedCliFiles.Count -gt 0) { throw "ops/cli root must contain categories only: $($unexpectedCliFiles.Name -join ', ')" }
$project = Get-Content -Raw -LiteralPath (Join-Path $root '.agents/project/project.json') | ConvertFrom-Json
if ($project.status -ne 'PHYSICAL_RELOCATED') { throw 'Host project is not marked PHYSICAL_RELOCATED' }
$registry = Get-Content -Raw -LiteralPath (Join-Path $root 'ops/config/projects/REGISTRY.json') | ConvertFrom-Json
$productRegistryPath = Join-Path $root 'platform/apps/REGISTRY.json'
if (-not (Test-Path -LiteralPath $productRegistryPath -PathType Leaf)) { throw 'Product project registry missing' }
$productRegistry = Get-Content -Raw -LiteralPath $productRegistryPath | ConvertFrom-Json
$registered = @($registry.projects | Where-Object projectId -eq $project.projectId)
$productRegistered = @($productRegistry.projects | Where-Object projectId -eq $project.projectId)
if ($registered.Count -ne 1) { throw "Project manifest is not uniquely registered: $($project.projectId)" }
if ($productRegistered.Count -ne 1) { throw "Project is not uniquely registered in product layer: $($project.projectId)" }
if ($productRegistered[0].productRoot -ne "platform/apps/$($project.projectId)") { throw 'Product registry productRoot mismatch' }
if ($registered[0].configRoot -ne "ops/config/projects/$($project.projectId)") { throw 'Registry configRoot mismatch' }
if ($registered[0].runtimeRoot -ne "ops/runtime/projects/$($project.projectId)") { throw 'Registry runtimeRoot mismatch' }
if (-not (Test-Path -LiteralPath (Join-Path $root 'platform/apps/geostat/backend/settings.gradle'))) { throw 'Backend Gradle source boundary missing' }
if (-not (Test-Path -LiteralPath (Join-Path $root 'platform/apps/geostat/backend/build.gradle'))) { throw 'Backend build boundary missing' }
foreach ($path in @('core','api','mobile')) {
  $item = Get-Item -LiteralPath (Join-Path $root "platform/apps/geostat/backend/$path") -Force
  if ($item.LinkType) { throw "Backend source must be a physical directory: $path" }
  if (-not (Test-Path -LiteralPath $item.FullName -PathType Container)) { throw "Backend source missing: $path" }
}
$frontend = Get-Item -LiteralPath (Join-Path $root 'platform/apps/geostat/frontend/web') -Force
if ($frontend.LinkType -or -not (Test-Path -LiteralPath $frontend.FullName -PathType Container)) { throw 'Frontend source must be a physical directory' }
$controlUi = Get-Item -LiteralPath (Join-Path $root 'platform/apps/geostat/frontend/geostat-system-app') -Force
if ($controlUi.LinkType -or -not (Test-Path -LiteralPath $controlUi.FullName -PathType Container)) { throw 'Control-plane UI must be a physical directory' }
foreach ($legacy in @('core','api','mobile','web','settings.gradle','build.gradle','gradlew','gradlew.bat','infra','scripts')) {
  if (Test-Path -LiteralPath (Join-Path $root $legacy)) { throw "Legacy root source remains: $legacy" }
}
$scanRoots = @(
  (Join-Path -Path $root -ChildPath 'platform')
  (Join-Path -Path $root -ChildPath '.agents')
)
$links = @(Get-ChildItem -Recurse -Force -Path $scanRoots -ErrorAction SilentlyContinue | Where-Object LinkType)
if ($links.Count -gt 0) { throw "Unexpected linked folders: $($links.FullName -join ', ')" }
[pscustomobject]@{
  status = 'PASS'
  host = $project.host
  upstream = 'EMPTY_PLACEHOLDER'
  backendSources = @('core','api','mobile')
  frontendSources = @('geostat-system-app','web[LEGACY]')
  relocation = $project.status
  deletion = 'LEGACY_ROOT_BOUNDARIES_REMOVED'
  mounts = 'core/api/mobile/web physically relocated; no junctions'
} | ConvertTo-Json -Compress
