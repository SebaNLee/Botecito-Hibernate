#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ACTION="${1:-}"
PAMPERO_USERNAME="${2:-}"
PAMPERO_HOST="pampero.itba.edu.ar"
PAW_DB_HOST="10.16.1.110"
PAW_DB_USER="paw-2026a-11"
PAW_DB_NAME="paw-2026a-11"

BACKUPS_DIR="$ROOT_DIR/backups"
PROD_PROPS="$ROOT_DIR/webapp/src/main/resources/config/credentials-production.properties"

usage() {
  echo "Usage:" >&2
  echo "  ./db.sh backup <username>" >&2
}

[[ "$ACTION" == "backup" ]] || { usage; exit 1; }
[[ -n "$PAMPERO_USERNAME" ]] || { usage; exit 1; }

[[ -f "$PROD_PROPS" ]] || {
  echo "error: missing $PROD_PROPS, create credentials-production.properties with production credentials" >&2
  exit 1
}

JDBC_PASSWORD="$(grep -m1 -E '^[[:space:]]*jdbc\.password[[:space:]]*=' "$PROD_PROPS" | cut -d'=' -f2- | sed 's/^[[:space:]]*//')"
[[ -n "$JDBC_PASSWORD" ]] || {
  echo "error: missing jdbc.password in $PROD_PROPS" >&2
  exit 1
}

CONTROL_PATH="/tmp/paw-db-backup-%C"
SSH_MASTER_OPTS=(
  -o ControlMaster=auto
  -o ControlPersist=300
  -o ControlPath="$CONTROL_PATH"
  -o LogLevel=ERROR
)
SSH_REUSE_OPTS=(
  -o ControlMaster=no
  -o ControlPath="$CONTROL_PATH"
  -o LogLevel=ERROR
)

cleanup_ssh_session() {
  ssh -o BatchMode=yes -o ControlPath="$CONTROL_PATH" -O exit "$PAMPERO_USERNAME@$PAMPERO_HOST" >/dev/null 2>&1 || true
}

trap cleanup_ssh_session EXIT

echo "Opening SSH session to Pampero..."
ssh "${SSH_MASTER_OPTS[@]}" "$PAMPERO_USERNAME@$PAMPERO_HOST" true

echo ""
echo "Reading latest Flyway migration from deploy DB..."
REMOTE_MIGRATION_CMD="read -r PGPASSWORD; export PGPASSWORD; psql -h '$PAW_DB_HOST' -U '$PAW_DB_USER' -d '$PAW_DB_NAME' -v ON_ERROR_STOP=1 -Atqc \"SELECT version FROM flyway_schema_history WHERE success = true AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1\""
LATEST_MIGRATION="$(ssh "${SSH_REUSE_OPTS[@]}" "$PAMPERO_USERNAME@$PAMPERO_HOST" "$REMOTE_MIGRATION_CMD" <<< "$JDBC_PASSWORD" | tr -d '[:space:]')"
[[ -n "$LATEST_MIGRATION" ]] || {
  echo "error: could not read latest migration version from deploy flyway_schema_history" >&2
  exit 1
}

mkdir -p "$BACKUPS_DIR"

TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE_NAME="backup-V${LATEST_MIGRATION}-${TIMESTAMP}.sql"
REMOTE_BACKUP_PATH="/home/$PAMPERO_USERNAME/$BACKUP_FILE_NAME"
LOCAL_BACKUP_PATH="$BACKUPS_DIR/$BACKUP_FILE_NAME"

echo ""
echo "Creating SQL backup on Pampero..."
REMOTE_BACKUP_CMD="read -r PGPASSWORD; export PGPASSWORD; pg_dump -h '$PAW_DB_HOST' -U '$PAW_DB_USER' -d '$PAW_DB_NAME' --clean --if-exists -f '$REMOTE_BACKUP_PATH'"
ssh "${SSH_REUSE_OPTS[@]}" "$PAMPERO_USERNAME@$PAMPERO_HOST" "$REMOTE_BACKUP_CMD" <<< "$JDBC_PASSWORD"

echo ""
echo "Downloading SQL backup to local backups directory using scp..."
scp "${SSH_REUSE_OPTS[@]}" "$PAMPERO_USERNAME@$PAMPERO_HOST:$REMOTE_BACKUP_PATH" "$LOCAL_BACKUP_PATH"

echo ""
echo "Cleaning up remote backup file..."
ssh "${SSH_REUSE_OPTS[@]}" "$PAMPERO_USERNAME@$PAMPERO_HOST" "rm -f '$REMOTE_BACKUP_PATH'"

echo ""
echo "Closing SSH session..."
cleanup_ssh_session
trap - EXIT

echo ""
echo "Backup done! Path: $LOCAL_BACKUP_PATH"
