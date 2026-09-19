#!/usr/bin/env bash
# Assembles a deterministic contract-bound artifact package for any approved contract dataset.
# The package structure (Access table, key fields, relation rules, policies) is read from the Control
# Plane package descriptor; nothing contract-specific lives in this script or in the assembler.
#
# Usage: ops/scripts/shell/artifact-package-assemble.sh CONTRACT_CODE REVISION DATASET_CODE \
#          DATASET.accdb RESOURCE_ROOT OUTPUT.zip EVIDENCE.json
set -euo pipefail

[ $# -eq 7 ] || { echo "usage: $0 CONTRACT_CODE REVISION DATASET_CODE DATASET.accdb RESOURCE_ROOT OUTPUT.zip EVIDENCE.json" >&2; exit 2; }
CONTRACT_CODE=$1; REVISION=$2; DATASET_CODE=$3; DATASET=$4; RESOURCES=$5; OUTPUT=$6; EVIDENCE=$7
[[ $CONTRACT_CODE =~ ^[A-Za-z0-9._-]{1,120}$ && $DATASET_CODE =~ ^[A-Za-z0-9._-]{1,120}$ && $REVISION =~ ^[1-9][0-9]{0,8}$ ]] || {
  echo "invalid contract dataset identity" >&2; exit 2;
}
[ -f "$DATASET" ] || { echo "dataset file not found: $DATASET" >&2; exit 2; }
[ -d "$RESOURCES" ] || { echo "resource root not found: $RESOURCES" >&2; exit 2; }

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
DESCRIPTOR="$(mktemp -t artifact-package-descriptor.XXXXXX.json)"
trap 'rm -f "$DESCRIPTOR"' EXIT

RESPONSE="$("$ROOT/ops/scripts/shell/artifact-operator-api.sh" GET \
  "/api/v1/platform/artifacts/contracts/$CONTRACT_CODE/revisions/$REVISION/datasets/$DATASET_CODE/package-descriptor")"
[ "$(printf '%s\n' "$RESPONSE" | tail -1)" = "HTTP=200" ] || { printf '%s\n' "$RESPONSE" >&2; echo "package descriptor request failed" >&2; exit 4; }
printf '%s\n' "$RESPONSE" | sed '$d' > "$DESCRIPTOR"

# Absolute, JVM-readable paths (Git Bash paths are converted on Windows).
abs() {
  local path; path="$(cd "$(dirname "$1")" && printf '%s/%s' "$(pwd)" "$(basename "$1")")"
  if command -v cygpath >/dev/null 2>&1; then cygpath -m "$path"; else printf '%s' "$path"; fi
}
mkdir -p "$(dirname "$OUTPUT")" "$(dirname "$EVIDENCE")"
ARGS=("-PpackageDescriptor=$(abs "$DESCRIPTOR")" "-PpackageDataset=$(abs "$DATASET")" "-PpackageResources=$(abs "$RESOURCES")"
      "-PpackageOutput=$(abs "$OUTPUT")" "-PpackageEvidence=$(abs "$EVIDENCE")")
cd "$ROOT/platform/apps/geostat/backend"
./gradlew -q :api:assembleArtifactPackage "${ARGS[@]}"
