#!/usr/bin/env bash
#
# Dev runner: build, start Jetty + Tailwind in the background with logs and PID files.
# Usage: ./run.sh start|stop|restart|status|logs [jetty|tailwind|both]
#
# Environment:
#   PAW_RUN_SKIP_TESTS=1  — mvn install with -DskipTests (faster iteration)
#

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WEBAPP_DIR="$ROOT_DIR/webapp"
STATE_DIR="$ROOT_DIR/.run"
JETTY_LOG="$STATE_DIR/jetty.log"
TAILWIND_LOG="$STATE_DIR/tailwind.log"
JETTY_PID_FILE="$STATE_DIR/jetty.pid"
TAILWIND_PID_FILE="$STATE_DIR/tailwind.pid"
JETTY_URL="${PAW_JETTY_URL:-http://127.0.0.1:8080}"
PORT="${PAW_JETTY_PORT:-8080}"
WAIT_SECS="${PAW_RUN_WAIT_SECS:-120}"

usage() {
  echo "Usage: $0 {start|stop|restart|status|logs} [jetty|tailwind|both]"
  echo
  echo "  start   — mvn install (webapp + deps), then Tailwind watch + Jetty in background"
  echo "  stop    — stop processes tracked by PID files; Jetty fallback: port $PORT"
  echo "  restart — stop then start"
  echo "  status  — port, HTTP code, PIDs, last log lines"
  echo "  logs    — tail -f logs (default: both; or jetty | tailwind)"
  exit 1
}

die() {
  echo "error: $*" >&2
  exit 1
}

ensure_layout() {
  if [[ ! -f "$ROOT_DIR/pom.xml" || ! -f "$WEBAPP_DIR/pom.xml" ]]; then
    die "this script must live in the repository root (missing pom.xml under $ROOT_DIR or webapp/)"
  fi
  mkdir -p "$STATE_DIR"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "missing command '$1' (install it and retry)"
}

check_prereqs() {
  require_cmd java
  require_cmd mvn
  if ! java -version 2>&1 | grep -qE 'version "21|version "2[2-9]'; then
    echo "warning: this project targets Java 21; 'java -version' does not look like 21+." >&2
  fi
  if command -v pg_isready >/dev/null 2>&1; then
    if ! pg_isready -q 2>/dev/null; then
      echo "warning: PostgreSQL does not appear to be accepting connections (pg_isready failed)." >&2
      echo "          Jetty may return HTTP 503 until Postgres is up and database 'paw' exists." >&2
    fi
  else
    echo "note: install postgresql-client for pg_isready checks (optional)." >&2
  fi
  if ! command -v curl >/dev/null 2>&1 && ! command -v python3 >/dev/null 2>&1; then
    echo "note: install curl or python3 for HTTP status in ./run.sh status." >&2
  fi
  if ! command -v lsof >/dev/null 2>&1; then
    echo "note: install lsof for ./run.sh stop when PID files are missing (sudo apt install lsof)." >&2
  fi
}

tcp_port_open() {
  local host="${1:-127.0.0.1}"
  local port="$2"
  bash -c "echo >/dev/tcp/$host/$port" 2>/dev/null
}

wait_for_tcp() {
  local port="$1"
  local max="$2"
  local i
  for ((i = 1; i <= max; i++)); do
    if tcp_port_open 127.0.0.1 "$port"; then
      return 0
    fi
    sleep 1
  done
  return 1
}

http_status_code() {
  local url="$1"
  if command -v curl >/dev/null 2>&1; then
    curl -sS -o /dev/null -w '%{http_code}' --max-time 3 "$url" 2>/dev/null || true
    return
  fi
  command -v python3 >/dev/null 2>&1 || {
    echo ""
    return
  }
  python3 -c "
import urllib.error
import urllib.request
import sys
url = sys.argv[1]
try:
    with urllib.request.urlopen(url, timeout=3) as r:
        print(r.status)
except urllib.error.HTTPError as e:
    print(e.code)
except Exception:
    print('')
" "$url" 2>/dev/null
}

pid_alive() {
  local pid="$1"
  [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null
}

read_pid_file() {
  local f="$1"
  [[ -f "$f" ]] || return 1
  local p
  p="$(tr -d ' \n' <"$f")"
  [[ -n "$p" ]] || return 1
  echo "$p"
}

jetty_listening_pids() {
  if command -v lsof >/dev/null 2>&1; then
    lsof -ti "tcp:$PORT" -sTCP:LISTEN 2>/dev/null || true
  fi
}

stop_tailwind() {
  local pid
  pid="$(read_pid_file "$TAILWIND_PID_FILE" 2>/dev/null || true)"
  if [[ -n "$pid" ]] && pid_alive "$pid"; then
    kill "$pid" 2>/dev/null || true
    echo "Tailwind (PID $pid) stopped."
  fi
  rm -f "$TAILWIND_PID_FILE"

  # Fallback: Maven tailwind goal for this webapp path only
  if pgrep -f "mvn.*tailwind:watch" >/dev/null 2>&1; then
    local pids
    pids="$(pgrep -f "mvn.*tailwind:watch" || true)"
    local p
    for p in $pids; do
      if tr '\0' ' ' <"/proc/$p/cmdline" 2>/dev/null | grep -q "$WEBAPP_DIR"; then
        kill "$p" 2>/dev/null || true
        echo "Tailwind Maven process $p (this webapp) stopped."
      fi
    done
  fi
}

stop_jetty() {
  local pid
  pid="$(read_pid_file "$JETTY_PID_FILE" 2>/dev/null || true)"
  if [[ -n "$pid" ]] && pid_alive "$pid"; then
    kill "$pid" 2>/dev/null || true
    echo "Jetty Maven launcher (PID $pid) stopped."
  fi
  rm -f "$JETTY_PID_FILE"

  local lp
  lp="$(jetty_listening_pids)"
  if [[ -n "$lp" ]]; then
    # shellcheck disable=SC2086
    kill $lp 2>/dev/null || true
    echo "Stopped process(es) listening on tcp/$PORT."
  else
    echo "Nothing listening on tcp/$PORT."
  fi
}

do_build() {
  local skip_tests=()
  if [[ "${PAW_RUN_SKIP_TESTS:-}" == "1" ]]; then
    skip_tests=(-DskipTests)
    echo "Building with -DskipTests (PAW_RUN_SKIP_TESTS=1)."
  fi
  cd "$ROOT_DIR"
  echo "Building webapp and required modules..."
  mvn install "${skip_tests[@]}" -pl webapp -am -q
  echo "Build finished."
}

start_bg() {
  local name="$1"
  local logfile="$2"
  local pidfile="$3"
  shift 3
  : >"$logfile"
  setsid bash -c '
    cd "$1" || exit 1
    shift
    exec >>"$1" 2>&1
    shift
    exec "$@"
  ' _ "$WEBAPP_DIR" "$logfile" "$@" </dev/null &
  local pid=$!
  echo "$pid" >"$pidfile"
  disown "$pid" 2>/dev/null || true
  echo "$name started (PID $pid); log: $logfile"
}

do_start() {
  ensure_layout
  check_prereqs

  if tcp_port_open 127.0.0.1 "$PORT"; then
    echo "warning: something already accepts connections on 127.0.0.1:$PORT — start anyway? Stopping listeners first."
    stop_jetty || true
    sleep 1
  fi

  do_build

  start_bg "Tailwind watch" "$TAILWIND_LOG" "$TAILWIND_PID_FILE" mvn tailwind:watch
  local tw_pid
  tw_pid="$(read_pid_file "$TAILWIND_PID_FILE")"

  sleep 1
  if ! pid_alive "$tw_pid"; then
    echo "Tailwind exited immediately. Last lines of $TAILWIND_LOG:" >&2
    tail -n 40 "$TAILWIND_LOG" >&2 || true
    die "Tailwind watch failed to stay running"
  fi

  start_bg "Jetty" "$JETTY_LOG" "$JETTY_PID_FILE" mvn jetty:run
  local j_pid
  j_pid="$(read_pid_file "$JETTY_PID_FILE")"

  echo
  echo "Waiting for TCP $PORT (up to ${WAIT_SECS}s; first run can be slow)..."
  if ! wait_for_tcp "$PORT" "$WAIT_SECS"; then
    echo "Nothing is listening on $PORT yet. Last lines of $JETTY_LOG:" >&2
    tail -n 60 "$JETTY_LOG" >&2 || true
    die "Jetty did not open port $PORT — see $JETTY_LOG"
  fi

  local code
  code="$(http_status_code "$JETTY_URL/")"
  if [[ -z "$code" ]]; then
    echo "TCP $PORT is open (install curl or use python3 to see HTTP status)."
  elif [[ "$code" == "200" || "$code" == "302" || "$code" == "301" ]]; then
    echo "HTTP $code — app responded at $JETTY_URL"
  elif [[ "$code" == "503" ]]; then
    echo "HTTP 503 — Jetty is up but the webapp failed to start (often PostgreSQL / Flyway). See $JETTY_LOG"
    tail -n 40 "$JETTY_LOG" >&2 || true
  else
    echo "HTTP $code — check $JETTY_URL and $JETTY_LOG if unexpected."
  fi

  if ! pid_alive "$j_pid"; then
    echo "Jetty Maven process exited. Last lines of $JETTY_LOG:" >&2
    tail -n 60 "$JETTY_LOG" >&2 || true
    die "Jetty process died — see $JETTY_LOG"
  fi

  echo
  echo "Commands: ./run.sh status   ./run.sh logs   ./run.sh stop"
  printf 'Open: \033]8;;%s\033\\%s\033]8;;\033\\\n' "$JETTY_URL" "$JETTY_URL"
}

do_stop() {
  ensure_layout
  stop_tailwind
  stop_jetty
}

do_status() {
  ensure_layout
  echo "State directory: $STATE_DIR"
  if tcp_port_open 127.0.0.1 "$PORT"; then
    echo "TCP 127.0.0.1:$PORT: open"
  else
    echo "TCP 127.0.0.1:$PORT: closed"
  fi
  local code
  code="$(http_status_code "$JETTY_URL/")"
  if [[ -n "$code" ]]; then
    echo "HTTP GET $JETTY_URL/ → $code"
    if [[ "$code" == "503" ]]; then
      echo "  → Spring context or DB likely failed; tail $JETTY_LOG"
    fi
  else
    echo "HTTP: (install curl or python3 for status code)"
  fi
  local p
  p="$(read_pid_file "$JETTY_PID_FILE" 2>/dev/null || true)"
  [[ -n "$p" ]] && echo "jetty.pid: $p $(pid_alive "$p" && echo alive || echo dead)"
  p="$(read_pid_file "$TAILWIND_PID_FILE" 2>/dev/null || true)"
  [[ -n "$p" ]] && echo "tailwind.pid: $p $(pid_alive "$p" && echo alive || echo dead)"
  echo
  echo "Last 15 lines — Jetty:"
  tail -n 15 "$JETTY_LOG" 2>/dev/null || echo "(no log yet)"
  echo
  echo "Last 15 lines — Tailwind:"
  tail -n 15 "$TAILWIND_LOG" 2>/dev/null || echo "(no log yet)"
}

do_logs() {
  ensure_layout
  touch "$JETTY_LOG" "$TAILWIND_LOG"
  local target="${1:-both}"
  case "$target" in
    jetty) tail -f "$JETTY_LOG" ;;
    tailwind) tail -f "$TAILWIND_LOG" ;;
    both) tail -f "$JETTY_LOG" "$TAILWIND_LOG" ;;
    *) die "logs target must be jetty, tailwind, or both" ;;
  esac
}

main() {
  [[ $# -ge 1 ]] || usage
  ensure_layout

  case "$1" in
    start)
      do_start
      ;;
    stop)
      do_stop
      ;;
    restart)
      do_stop || true
      sleep 1
      do_start
      ;;
    status)
      do_status
      ;;
    logs)
      do_logs "${2:-both}"
      ;;
    *)
      usage
      ;;
  esac
}

main "$@"
