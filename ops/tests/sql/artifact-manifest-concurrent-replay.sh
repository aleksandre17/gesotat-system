#!/usr/bin/env bash
set -euo pipefail

# Run inside an isolated SQL Server test database (or explicitly accept one retained
# immutable fixture row with ALLOW_IMMUTABLE_FIXTURE=1). Artifact versions are append-only.
# This exercises the same unique-index key-range lookup used by ArtifactRegistry.manifestId.
SQLCMD="${SQLCMD:-/opt/mssql-tools18/bin/sqlcmd}"
SERVER="${SQLSERVER_HOST:-localhost}"
DATABASE="${DATA_PLANE_DATABASE:-geostat-data}"
if [[ "${ALLOW_IMMUTABLE_FIXTURE:-0}" != "1" ]]; then
  echo "This acceptance creates an append-only manifest/version row; set ALLOW_IMMUTABLE_FIXTURE=1 only for a disposable test DB or a reviewed retained fixture" >&2
  exit 2
fi
if [[ -z "${MSSQL_SA_PASSWORD:-}" ]]; then
  echo "MSSQL_SA_PASSWORD must be supplied through the environment" >&2
  exit 2
fi
if [[ ! -x "$SQLCMD" ]]; then
  echo "sqlcmd executable is unavailable" >&2
  exit 2
fi

scratch="$(mktemp -d)"
suffix="$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')"
checksum="$(printf '%s' "$suffix" | sha256sum | cut -d' ' -f1)"
object_sha="$(printf 'object-%s' "$suffix" | sha256sum | cut -d' ' -f1)"
package="AIR_018_CONCURRENT_${suffix}"
cleanup() {
  local rc=$?
  rm -rf "$scratch"
  exit "$rc"
}
trap cleanup EXIT

run_attempt() {
  local output="$1" delay="$2"
  "$SQLCMD" -S "$SERVER" -U sa -P "$MSSQL_SA_PASSWORD" -C -d "$DATABASE" -b -h -1 -W -Q "
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @manifest_id BIGINT;
SELECT @manifest_id=artifact_manifest_id
FROM ingest.artifact_manifest WITH (UPDLOCK,HOLDLOCK,INDEX(uq_artifact_manifest_checksum))
WHERE package_checksum='$checksum';
IF @manifest_id IS NULL
BEGIN
  WAITFOR DELAY '00:00:$delay';
  INSERT ingest.artifact_manifest(manifest_schema,package_code,package_checksum,generator_version,entry_count,source_reference)
  VALUES('geostat.artifact-manifest.v1',N'$package','$checksum',N'artifact-manifest-generator/1',1,N'AIR-2026-018');
  SET @manifest_id=CONVERT(BIGINT,SCOPE_IDENTITY());
  DECLARE @object_id BIGINT;
  SELECT @object_id=artifact_object_id FROM ingest.artifact_object WITH (UPDLOCK,HOLDLOCK) WHERE sha256='$object_sha';
  IF @object_id IS NULL
  BEGIN
    INSERT ingest.artifact_object(sha256,byte_size,media_type,bucket,object_key)
    VALUES('$object_sha',13,'application/octet-stream','geostat-ingest',N'air-018/concurrency/$object_sha.bin');
    SET @object_id=CONVERT(BIGINT,SCOPE_IDENTITY());
  END;
  INSERT ingest.artifact_version(artifact_manifest_id,original_path,original_name,artifact_object_id)
  VALUES(@manifest_id,N'evidence.bin',N'evidence.bin',@object_id);
END;
SELECT CONVERT(VARCHAR(20),@manifest_id);
COMMIT TRANSACTION;
" | tr -d '\r' | tail -n 1 > "$output"
}

run_attempt "$scratch/first" 3 & first_pid=$!
sleep 0.25
run_attempt "$scratch/second" 0 & second_pid=$!
wait "$first_pid"
wait "$second_pid"

first="$(cat "$scratch/first")"
second="$(cat "$scratch/second")"
[[ "$first" =~ ^[0-9]+$ && "$first" == "$second" ]] || {
  echo "Concurrent replays did not converge: first=$first second=$second" >&2
  exit 1
}
counts="$($SQLCMD -S "$SERVER" -U sa -P "$MSSQL_SA_PASSWORD" -C -d "$DATABASE" -b -h -1 -W -s '|' -Q \
  "SET NOCOUNT ON; SELECT (SELECT COUNT_BIG(*) FROM ingest.artifact_manifest WHERE package_checksum='$checksum' AND package_code='$package'),(SELECT COUNT_BIG(*) FROM ingest.artifact_version v JOIN ingest.artifact_manifest m ON m.artifact_manifest_id=v.artifact_manifest_id WHERE m.package_checksum='$checksum' AND m.package_code='$package'),(SELECT COUNT_BIG(*) FROM ingest.artifact_object WHERE sha256='$object_sha');" | tr -d '\r' | tail -n 1)"
[[ "$counts" == "1|1|1" ]] || { echo "Expected one manifest/version/object, found $counts" >&2; exit 1; }

echo "ARTIFACT_MANIFEST_CONCURRENT_REPLAY_PASS|manifestId=$first|package=$package|checksum=$checksum|manifest/version/object=$counts|fixture=retained-immutable"
