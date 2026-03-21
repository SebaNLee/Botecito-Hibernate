#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEBAPP_DIR="$ROOT_DIR/webapp"
JETTY_URL="http://localhost:8080"

cd "$ROOT_DIR"

if [[ ! -f "$ROOT_DIR/pom.xml" || ! -f "$WEBAPP_DIR/pom.xml" ]]; then
  echo "This script must be run from the project root."
  exit 1
fi

start_process() {
  local name="$1"
  shift

  nohup "$@" >/dev/null 2>&1 &
  STARTED_PID=$!
  disown "$STARTED_PID" 2>/dev/null || true
  echo "$name started with PID $STARTED_PID."
}

is_jetty_running() {
  curl --silent --head --max-time 2 "$JETTY_URL" >/dev/null 2>&1
}

is_pid_running() {
  local pid="$1"
  kill -0 "$pid" 2>/dev/null
}

wait_for_jetty() {
  local attempts=30

  for ((i = 1; i <= attempts; i++)); do
    if is_jetty_running; then
      return 0
    fi
    sleep 1
  done

  return 1
}

wait_for_pid() {
  local pid="$1"
  local attempts=5

  for ((i = 1; i <= attempts; i++)); do
    if is_pid_running "$pid"; then
      return 0
    fi
    sleep 1
  done

  return 1
}

echo "Compiling base modules from the root..."
mvn install
mvn compile

cd "$WEBAPP_DIR"

echo "Compiling webapp..."
mvn install
mvn compile

echo "Starting background processes..."
start_process "Tailwind watch" mvn tailwind:watch
TAILWIND_PID="$STARTED_PID"

if is_jetty_running; then
  echo "Jetty is already running at $JETTY_URL."
else
  start_process "Jetty" mvn jetty:run
fi

echo
if wait_for_pid "$TAILWIND_PID"; then
  echo "Tailwind watch is running."
else
  echo "Tailwind watch did not stay running."
fi

if wait_for_jetty; then
  echo "Jetty is up."
else
  echo "Jetty did not respond at $JETTY_URL within 30 seconds."
fi

echo "Processes started."
printf 'Open: \033]8;;%s\033\\%s\033]8;;\033\\\n' "$JETTY_URL" "$JETTY_URL"
