#!/usr/bin/env bash
#
# Build a WAR for Pampero / production: embeds JDBC profile "production" and
# packages config/credentials-production.properties (must exist locally; gitignored).
#
# Usage: ./deploy.sh
# Output: webapp/target/webapp.war
#

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROD_PROPS="$ROOT_DIR/webapp/src/main/resources/config/credentials-production.properties"

if [[ ! -f "$PROD_PROPS" ]]; then
  echo "error: missing $PROD_PROPS" >&2
  echo "  Copy webapp/src/main/resources/config/credentials-production.properties.example to credentials-production.properties" >&2
  echo "  and set JDBC (and any other) credentials for the server." >&2
  exit 1
fi

cd "$ROOT_DIR"
echo "Building webapp.war with -Pproduction-war (skipping tests)..."
mvn clean package -DskipTests -pl webapp -am -Pproduction-war

WAR="$ROOT_DIR/webapp/target/webapp.war"
if [[ ! -f "$WAR" ]]; then
  echo "error: expected $WAR after package" >&2
  exit 1
fi

echo
echo "WAR ready: $WAR"
echo "Copy to the server (see docs/deployment.md), e.g. scp then sftp to web/app.war"
