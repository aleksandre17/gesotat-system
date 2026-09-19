#!/usr/bin/env bash
# Replays the registered migration chain, in runner order, into disposable empty databases and checks
# that an approved site contract revision exists afterwards. Proves (or refutes) that the platform can
# be rebuilt from source. The databases are created with a unique suffix and always dropped.
#
# Runs where sqlcmd and the SQL Server instance are (for dev: inside the database container).
# Inputs:  MSSQL_SA_PASSWORD (environment), MIGRATIONS_DIR (db/platform files), CHAIN_FILE ("<plane> <file>" per line,
#          plane = control|data|archive, produced from PlatformSchemaMigrationRunner by the caller).
set -euo pipefail

SQLCMD="${SQLCMD:-/opt/mssql-tools18/bin/sqlcmd}"
SERVER="${SQLSERVER_HOST:-localhost}"
: "${MSSQL_SA_PASSWORD:?MSSQL_SA_PASSWORD must be supplied through the environment}"
: "${MIGRATIONS_DIR:?}"; : "${CHAIN_FILE:?}"
SUFFIX="$(date +%s)$$"
declare -A DB=([control]="zz_replay_cp_$SUFFIX" [data]="zz_replay_dp_$SUFFIX" [archive]="zz_replay_ap_$SUFFIX")

run() { "$SQLCMD" -S "$SERVER" -U sa -P "$MSSQL_SA_PASSWORD" -C -b -I -h -1 -W "$@"; }
cleanup() {
  for plane in control data archive; do
    run -d master -Q "IF DB_ID(N'${DB[$plane]}') IS NOT NULL BEGIN ALTER DATABASE [${DB[$plane]}] SET SINGLE_USER WITH ROLLBACK IMMEDIATE; DROP DATABASE [${DB[$plane]}]; END" >/dev/null 2>&1 || true
  done
}
trap cleanup EXIT

for plane in control data archive; do run -d master -Q "CREATE DATABASE [${DB[$plane]}]" >/dev/null; done
# The Control Plane references identity and page tables that the application ORM owns; minimal stand-ins
# with the referenced columns and the bootstrap admin the seeds look up.
run -d "${DB[control]}" -Q "CREATE TABLE dbo.users(id BIGINT IDENTITY PRIMARY KEY, username NVARCHAR(255) NOT NULL UNIQUE);
CREATE TABLE dbo.page_nodes(id BIGINT IDENTITY PRIMARY KEY);
CREATE TABLE dbo.roles(id BIGINT IDENTITY PRIMARY KEY, name NVARCHAR(255) NOT NULL UNIQUE);
CREATE TABLE dbo.permissions(id BIGINT IDENTITY PRIMARY KEY, name NVARCHAR(255) NOT NULL UNIQUE);
CREATE TABLE dbo.role_permissions(role_id BIGINT NOT NULL, permission_id BIGINT NOT NULL, PRIMARY KEY(role_id,permission_id));
INSERT dbo.users(username) VALUES(N'admin'); INSERT dbo.roles(name) VALUES(N'ADMIN');" >/dev/null

applied=0
failed=0
mapfile -t CHAIN < "$CHAIN_FILE"
for line in "${CHAIN[@]}"; do
  plane="${line%% *}"; file="${line#* }"
  [ -n "$plane" ] && [ "$plane" != "$line" ] || continue
  # stdin is detached: sqlcmd must never read the chain or wait for input.
  if ! out="$(run -t 600 -d "${DB[$plane]}" -i "$MIGRATIONS_DIR/$file" </dev/null 2>&1)"; then
    failed=$((failed + 1))
    echo "FAILED=$file plane=$plane :: $(echo "$out" | grep -m1 -E '^Msg [0-9]+' || true) $(echo "$out" | grep -v -E '^Msg |^$|rows affected' | head -n 1)"
    # REPLAY_MODE=inventory lists every failing script in one pass; the default stops at the first one.
    [ "${REPLAY_MODE:-strict}" = "inventory" ] || exit 1
    continue
  fi
  applied=$((applied + 1))
done
echo "FAILED_COUNT=$failed"

echo "APPLIED=$applied"
run -d "${DB[control]}" -Q "SET NOCOUNT ON; SELECT CONCAT(contract_code,N' r',revision,N' ',status,N' datasets=',(SELECT COUNT(*) FROM platform.site_contract_dataset d WHERE d.site_contract_revision_id=r.site_contract_revision_id)) FROM platform.site_contract_revision r ORDER BY contract_code,revision"
