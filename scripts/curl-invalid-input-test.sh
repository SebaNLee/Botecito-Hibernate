#!/usr/bin/env bash

#Note: user mail and pass from in app user

# Example call:
#
#  BASE_URL=http://localhost:8080 \
#  EMAIL="lpizzutobeltran@itba.edu.ar" \
#  PASSWORD="lorenzo9" \
#  ITEM_ID=15 \
#  ./scripts/curl-invalid-input-smoke.sh
#
# if that user exist in the db it should work

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="${EMAIL:-}"
PASSWORD="${PASSWORD:-}"
ITEM_ID="${ITEM_ID:-}"

if [[ -z "$EMAIL" || -z "$PASSWORD" || -z "$ITEM_ID" ]]; then
  cat >&2 <<'USAGE'
Usage:
  BASE_URL=http://localhost:8080 \
  EMAIL=user@example.com \
  PASSWORD=password \
  ITEM_ID=15 \
  ./scripts/curl-invalid-input-smoke.sh

If the app uses a context path, include it in BASE_URL, for example:
  BASE_URL=http://localhost:8080/webapp
USAGE
  exit 2
fi

COOKIE_JAR="$(mktemp)"
BODY_FILE="$(mktemp)"
HEADER_FILE="$(mktemp)"
trap 'rm -f "$COOKIE_JAR" "$BODY_FILE" "$HEADER_FILE"' EXIT

pass() {
  printf 'PASS: %s\n' "$1"
}

fail() {
  printf 'FAIL: %s\n' "$1" >&2
  printf 'Last response headers:\n' >&2
  sed -n '1,20p' "$HEADER_FILE" >&2 || true
  exit 1
}

post_no_follow() {
  local path="$1"
  shift
  curl -sS -D "$HEADER_FILE" -o "$BODY_FILE" \
    -b "$COOKIE_JAR" -c "$COOKIE_JAR" \
    "$@" \
    "${BASE_URL}${path}"
}

assert_no_redirect_to() {
  local forbidden_location="$1"
  local label="$2"
  if grep -Eiq "^Location: .*$forbidden_location" "$HEADER_FILE"; then
    fail "$label advanced to $forbidden_location"
  fi
  pass "$label did not advance"
}

assert_no_success_message() {
  local label="$1"
  if grep -Fqi "Your request was sent to Botecito for review." "$BODY_FILE" \
    || grep -Fqi "Tu solicitud fue enviada a Botecito para revision." "$BODY_FILE"; then
    fail "$label created a reservation request"
  fi
  pass "$label did not create a reservation request"
}

printf 'Logging in as %s at %s\n' "$EMAIL" "$BASE_URL"
post_no_follow "/login" \
  -d "j_username=$EMAIL" \
  -d "j_password=$PASSWORD"

curl -sS -L -o "$BODY_FILE" -b "$COOKIE_JAR" -c "$COOKIE_JAR" "${BASE_URL}/profile"
if grep -qi 'name="j_username"' "$BODY_FILE"; then
  fail "login did not create an authenticated session"
fi
pass "login created an authenticated session"

printf '\nTesting publish form validation\n'
post_no_follow "/publish" \
  -d "title=Bad curl boat" \
  -d "description=invalid price" \
  -d "itemTypeId=1" \
  -d "pricePerHour=-10" \
  -d "marina=1" \
  -d "capacity=2" \
  -d "difficultyLevel=1"
assert_no_redirect_to "/publish/availability" "negative publish price"

post_no_follow "/publish" \
  -d "title=Curl smoke boat" \
  -d "description=valid first step" \
  -d "itemTypeId=1" \
  -d "pricePerHour=2000" \
  -d "marina=1" \
  -d "capacity=2" \
  -d "difficultyLevel=1"
if ! grep -Eiq '^Location: .*/publish/availability' "$HEADER_FILE"; then
  fail "valid publish step 1 did not advance to availability"
fi
pass "valid publish step 1 advanced to availability"

post_no_follow "/publish/availability" \
  -d "enabledDays=MONDAY" \
  -d "availabilityRanges=MONDAY|10:00|11:00"
assert_no_redirect_to "/publish/contact" "one-hour availability slot"

post_no_follow "/publish/availability" \
  -d "enabledDays=MONDAY" \
  -d "availabilityRanges=MONDAY|10:15|12:15"
if grep -Eiq '^Location: .*/publish/contact' "$HEADER_FILE"; then
  pass "non-30-minute availability reached final submit step"
  post_no_follow "/publish/contact"
  assert_no_redirect_to "/publish/success" "non-30-minute availability final submit"
else
  pass "non-30-minute availability did not advance"
fi

printf '\nTesting reservation validation against item %s\n' "$ITEM_ID"
post_no_follow "/item/${ITEM_ID}" \
  -d "date=2026-05-10" \
  -d "startTime=12:00" \
  -d "endTime=10:00" \
  -d "requestMessage=end before start"
assert_no_success_message "reservation end before start"

post_no_follow "/item/${ITEM_ID}" \
  -d "date=2026-05-10" \
  -d "startTime=10:00" \
  -d "endTime=11:00" \
  -d "requestMessage=too short"
assert_no_success_message "one-hour reservation"

LONG_MSG="$(printf 'a%.0s' {1..1001})"
post_no_follow "/item/${ITEM_ID}" \
  -d "date=2026-05-10" \
  -d "startTime=10:00" \
  -d "endTime=12:00" \
  --data-urlencode "requestMessage=${LONG_MSG}"
assert_no_success_message "oversized reservation message"

printf '\nDone. These checks verify that invalid curl submissions do not advance the server-side flows.\n'
