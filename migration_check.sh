#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   chmod +x verify_v24_preservation.sh
#   ./verify_v24_preservation.sh <old_db> <new_db> [host] [port] [user]
#
# Example:
#   ./verify_v24_preservation.sh paw_v23_check paw_v24_check localhost 5432 pawuser

OLD_DB="${1:-}"
NEW_DB="${2:-}"
PGHOST_ARG="${3:-localhost}"
PGPORT_ARG="${4:-5432}"
PGUSER_ARG="${5:-${USER}}"

if [[ -z "$OLD_DB" || -z "$NEW_DB" ]]; then
  echo "Usage: $0 <old_db> <new_db> [host] [port] [user]"
  exit 1
fi

export PGHOST="$PGHOST_ARG"
export PGPORT="$PGPORT_ARG"
export PGUSER="$PGUSER_ARG"

PASS=0
FAIL=0

runq() {
  local db="$1"
  local sql="$2"
  psql -X -d "$db" -At -c "$sql"
}

check_eq() {
  local label="$1"
  local got="$2"
  local exp="$3"
  if [[ "$got" == "$exp" ]]; then
    printf "PASS | %s | %s\n" "$label" "$got"
    PASS=$((PASS+1))
  else
    printf "FAIL | %s | got=%s expected=%s\n" "$label" "$got" "$exp"
    FAIL=$((FAIL+1))
  fi
}

check_num_eq() {
  local label="$1"
  local got="$2"
  local exp="$3"
  if [[ "${got:-0}" -eq "${exp:-0}" ]]; then
    printf "PASS | %s | %s\n" "$label" "$got"
    PASS=$((PASS+1))
  else
    printf "FAIL | %s | got=%s expected=%s\n" "$label" "$got" "$exp"
    FAIL=$((FAIL+1))
  fi
}

echo "== Connection info =="
echo "OLD_DB=$OLD_DB NEW_DB=$NEW_DB host=$PGHOST port=$PGPORT user=$PGUSER"
echo

echo "== Core row-count parity =="
old_users="$(runq "$OLD_DB" "SELECT COUNT(*) FROM users;")"
new_user="$(runq "$NEW_DB" "SELECT COUNT(*) FROM \"user\";")"
check_eq "users -> user count" "$new_user" "$old_users"

old_item_type="$(runq "$OLD_DB" "SELECT COUNT(*) FROM item_type;")"
new_item_type="$(runq "$NEW_DB" "SELECT COUNT(*) FROM item_type;")"
check_eq "item_type count" "$new_item_type" "$old_item_type"

old_loc="$(runq "$OLD_DB" "SELECT COUNT(*) FROM location_option;")"
new_loc="$(runq "$NEW_DB" "SELECT COUNT(*) FROM location;")"
check_eq "location_option -> location count" "$new_loc" "$old_loc"

old_item="$(runq "$OLD_DB" "SELECT COUNT(*) FROM item;")"
new_item="$(runq "$NEW_DB" "SELECT COUNT(*) FROM item;")"
check_eq "item count" "$new_item" "$old_item"

old_booking="$(runq "$OLD_DB" "SELECT COUNT(*) FROM item_booking;")"
new_booking="$(runq "$NEW_DB" "SELECT COUNT(*) FROM booking;")"
# New can be lower due to explicit skip of unresolved version rows.
if [[ "$new_booking" -le "$old_booking" ]]; then
  printf "PASS | booking count (new<=old) | new=%s old=%s\n" "$new_booking" "$old_booking"
  PASS=$((PASS+1))
else
  printf "FAIL | booking count (new<=old) | new=%s old=%s\n" "$new_booking" "$old_booking"
  FAIL=$((FAIL+1))
fi

old_proof="$(runq "$OLD_DB" "SELECT COUNT(*) FROM booking_payment_proof;")"
new_proof="$(runq "$NEW_DB" "SELECT COUNT(*) FROM payment_proof;")"
check_eq "payment_proof count" "$new_proof" "$old_proof"

old_review="$(runq "$OLD_DB" "SELECT COUNT(*) FROM review;")"
new_review="$(runq "$NEW_DB" "SELECT COUNT(*) FROM review;")"
check_eq "review count" "$new_review" "$old_review"

echo
echo "== Truncation checks (must be 0 in NEW_DB) =="
check_num_eq "user.email > 100" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM \"user\" WHERE length(email) > 100;")" 0
check_num_eq "user.alias > 30" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM \"user\" WHERE alias IS NOT NULL AND length(alias) > 30;")" 0
check_num_eq "user.mail_token > 100" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM \"user\" WHERE mail_token IS NOT NULL AND length(mail_token) > 100;")" 0
check_num_eq "version.description > 100" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM \"version\" WHERE description IS NOT NULL AND length(description) > 100;")" 0
check_num_eq "payment_proof.filename > 100" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM payment_proof WHERE length(filename) > 100;")" 0
check_num_eq "payment_proof.refuse_msg > 255" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM payment_proof WHERE refuse_msg IS NOT NULL AND length(refuse_msg) > 255;")" 0
check_num_eq "payment_proof.reply_msg > 255" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM payment_proof WHERE reply_msg IS NOT NULL AND length(reply_msg) > 255;")" 0
check_num_eq "review.comment > 255" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM review WHERE comment IS NOT NULL AND length(comment) > 255;")" 0

echo
echo "== Defaulting checks (must be 0) =="
check_num_eq "version.weight NULL" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM \"version\" WHERE weight IS NULL;")" 0
check_num_eq "version.difficulty NULL" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM \"version\" WHERE difficulty IS NULL;")" 0

echo
echo "== FK orphan checks (must be 0) =="
check_num_eq "orphan version.item_id" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM \"version\" v LEFT JOIN item i ON i.id=v.item_id WHERE i.id IS NULL;")" 0
check_num_eq "orphan booking.version_id" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM booking b LEFT JOIN \"version\" v ON v.id=b.version_id WHERE v.id IS NULL;")" 0
check_num_eq "orphan availability.version_id" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM availability a LEFT JOIN \"version\" v ON v.id=a.version_id WHERE v.id IS NULL;")" 0
check_num_eq "orphan media.version_id" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM media m LEFT JOIN \"version\" v ON v.id=m.version_id WHERE v.id IS NULL;")" 0
check_num_eq "orphan media.image_id" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM media m LEFT JOIN image i ON i.id=m.image_id WHERE i.id IS NULL;")" 0
check_num_eq "orphan payment_proof.booking_id" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM payment_proof p LEFT JOIN booking b ON b.id=p.booking_id WHERE b.id IS NULL;")" 0
check_num_eq "orphan review.booking_id" \
  "$(runq "$NEW_DB" "SELECT COUNT(*) FROM review r LEFT JOIN booking b ON b.id=r.booking_id WHERE b.id IS NULL;")" 0

echo
echo "== Informational distributions =="
echo "-- OLD booking states --"
runq "$OLD_DB" "SELECT state, COUNT(*) FROM item_booking GROUP BY state ORDER BY state;"
echo "-- NEW booking statuses --"
runq "$NEW_DB" "SELECT status, COUNT(*) FROM booking GROUP BY status ORDER BY status;"
echo "-- NEW version weight distribution (top 20) --"
runq "$NEW_DB" "SELECT weight, COUNT(*) FROM \"version\" GROUP BY weight ORDER BY weight LIMIT 20;"
echo "-- NEW version difficulty distribution --"
runq "$NEW_DB" "SELECT difficulty, COUNT(*) FROM \"version\" GROUP BY difficulty ORDER BY difficulty;"

echo
echo "== Summary =="
echo "PASS=$PASS FAIL=$FAIL"
if [[ "$FAIL" -gt 0 ]]; then
  exit 2
fi