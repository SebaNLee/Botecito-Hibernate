#!/usr/bin/env bash

set -euo pipefail

stop_tailwind() {
  local patterns=(
    'tailwind:watch'
    'tailwindcss'
  )
  local found=0
  local pattern

  for pattern in "${patterns[@]}"; do
    if pgrep -f "$pattern" >/dev/null 2>&1; then
      pkill -f "$pattern"
      found=1
    fi
  done

  if [[ "$found" -eq 0 ]]; then
    echo "Tailwind watch is not running."
    return 0
  fi

  echo "Tailwind watch stopped."
}

stop_jetty() {
  local pids
  pids="$(lsof -ti tcp:8080 -sTCP:LISTEN 2>/dev/null || true)"

  if [[ -z "$pids" ]]; then
    echo "Jetty is not running on port 8080."
    return 0
  fi

  kill $pids
  echo "Jetty stopped."
}

stop_tailwind
stop_jetty
