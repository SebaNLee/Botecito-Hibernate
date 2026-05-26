#!/usr/bin/env bash
# Run from anywhere: builds the multi-module project, then starts Jetty from webapp.
set -eo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

run() {
  echo ">>> $*" >&2
  if ! "$@"; then
    local ec=$?
    echo "ERROR: exit $ec — stopping after: $*" >&2
    exit "$ec"
  fi
}

run mvn clean install
run mvn compile

cd "$ROOT/webapp"
run mvn clean install
run mvn compile
run mvn jetty:run
