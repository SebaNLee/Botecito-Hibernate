#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PAMPERO_USERNAME="${1:-}"
PAMPERO_HOST="pampero.itba.edu.ar"
PAW_SFTP_HOST="10.16.1.110"
PAW_SFTP_USER="paw-2026a-11"

[[ -n "$PAMPERO_USERNAME" ]] || { echo "Usage: ./deploy.sh <username>" >&2; exit 1; }

BRANCH="$(git -C "$ROOT_DIR" rev-parse --abbrev-ref HEAD)"
if [[ "$BRANCH" != "main" ]]; then
  read -r -p "Branch is'$BRANCH' (not main). Continue with deploy? [y/n]: " confirm
  [[ "$confirm" =~ ^[Yy]$ ]] || { echo "Deploy cancelado." >&2; exit 1; }
fi

PROD_PROPS="$ROOT_DIR/webapp/src/main/resources/config/credentials-production.properties"
[[ -f "$PROD_PROPS" ]] || {
  echo "error: missing $PROD_PROPS, create credentials-production.properties with production credentials" >&2
  exit 1
}

WAR="$ROOT_DIR/webapp/target/webapp.war"
REMOTE_WAR="/home/$PAMPERO_USERNAME/webapp.war"
CONTROL_PATH="/tmp/paw-deploy-%C"
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

echo "Building WAR..."
mvn -f "$ROOT_DIR/pom.xml" clean package -DskipTests -pl webapp -am -Pproduction-war

echo ""
echo "Opening SSH session to Pampero..."
ssh "${SSH_MASTER_OPTS[@]}" "$PAMPERO_USERNAME@$PAMPERO_HOST" true

echo ""
echo "Uploading WAR to Pampero with scp..."
scp "${SSH_REUSE_OPTS[@]}" "$WAR" "$PAMPERO_USERNAME@$PAMPERO_HOST:$REMOTE_WAR"

echo ""
echo "Deploying to Pampero PAW server..."
ssh -tt "${SSH_REUSE_OPTS[@]}" "$PAMPERO_USERNAME@$PAMPERO_HOST" "printf '%s\\n' 'put \"$REMOTE_WAR\" \"web/app.war\"' 'exit' | sftp -q '$PAW_SFTP_USER@$PAW_SFTP_HOST'"

echo ""
echo "Closing SSH session..."
ssh -o BatchMode=yes -o ControlPath="$CONTROL_PATH" -O exit "$PAMPERO_USERNAME@$PAMPERO_HOST" >/dev/null 2>&1 || true

echo ""
echo "Deployment done!"
