#!/usr/bin/env bash
set -euo pipefail

SQLCMD="${SQLCMD:-/opt/mssql-tools18/bin/sqlcmd}"
SERVER="${SQLSERVER_HOST:-localhost}"
DATABASE="${DATA_PLANE_DATABASE:-geostat-data}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
MIGRATION="${ARTIFACT_AUDIT_MIGRATION:-$ROOT/platform/apps/geostat/backend/core/src/main/resources/db/platform/094_artifact_integrity_audit.sql}"

if [[ -z "${MSSQL_SA_PASSWORD:-}" ]]; then
  echo "MSSQL_SA_PASSWORD must be supplied through the environment" >&2
  exit 2
fi
if [[ ! -x "$SQLCMD" || ! -f "$MIGRATION" ]]; then
  echo "sqlcmd or artifact audit migration is unavailable" >&2
  exit 2
fi

scratch="$(mktemp)"
trap 'rm -f "$scratch"' EXIT
{
  cat <<'SQL'
SET XACT_ABORT ON;
SET NOCOUNT ON;
IF OBJECT_ID(N'ingest.artifact_object_audit_run',N'U') IS NOT NULL
  THROW 51094, 'Audit run table already exists; use a disposable pre-migration database.', 1;
IF OBJECT_ID(N'ingest.artifact_object_audit_issue',N'U') IS NOT NULL
  THROW 51094, 'Audit issue table already exists; use a disposable pre-migration database.', 1;
IF COL_LENGTH(N'ingest.artifact_object',N'last_audit_attempt_at') IS NOT NULL
  THROW 51094, 'Audit cursor column already exists; use a disposable pre-migration database.', 1;
BEGIN TRANSACTION;
SQL
  cat "$MIGRATION"
  cat <<'SQL'
IF OBJECT_ID(N'ingest.artifact_object_audit_run',N'U') IS NULL
  THROW 51094, 'Audit run table was not created.', 1;
IF OBJECT_ID(N'ingest.artifact_object_audit_issue',N'U') IS NULL
  THROW 51094, 'Audit issue table was not created.', 1;
IF COL_LENGTH(N'ingest.artifact_object',N'last_audit_attempt_at') IS NULL
  THROW 51094, 'Audit cursor column was not created.', 1;
ROLLBACK TRANSACTION;
IF OBJECT_ID(N'ingest.artifact_object_audit_run',N'U') IS NOT NULL
  THROW 51094, 'Rollback retained audit run table.', 1;
IF OBJECT_ID(N'ingest.artifact_object_audit_issue',N'U') IS NOT NULL
  THROW 51094, 'Rollback retained audit issue table.', 1;
IF COL_LENGTH(N'ingest.artifact_object',N'last_audit_attempt_at') IS NOT NULL
  THROW 51094, 'Rollback retained audit cursor column.', 1;
PRINT 'ARTIFACT_INTEGRITY_MIGRATION_ROLLBACK_PASS';
SQL
} >"$scratch"

"$SQLCMD" -S "$SERVER" -U sa -P "$MSSQL_SA_PASSWORD" -C -d "$DATABASE" -b -i "$scratch"
