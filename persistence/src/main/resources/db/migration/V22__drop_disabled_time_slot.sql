INSERT INTO item_booking (
    item_id,
    guest_id,
    start_time,
    end_time,
    state,
    request_message,
    host_decision_token,
    host_decision_used_at,
    created_at,
    updated_at
)
SELECT
    d.item_id,
    i.owner_id,
    ((d.slot_date + d.start_time) AT TIME ZONE 'America/Argentina/Buenos_Aires'),
    ((d.slot_date + d.end_time) AT TIME ZONE 'America/Argentina/Buenos_Aires'),
    'BOOKING_CONFIRMED'::booking_state,
    NULL,
    'migrated-disabled-slot-' || d.id,
    COALESCE(d.created_at, CURRENT_TIMESTAMP),
    COALESCE(d.created_at, CURRENT_TIMESTAMP),
    COALESCE(d.created_at, CURRENT_TIMESTAMP)
FROM disabled_time_slot d
JOIN item i ON i.id = d.item_id
LEFT JOIN item_booking b
    ON b.item_id = d.item_id
    AND b.guest_id = i.owner_id
    AND b.start_time = ((d.slot_date + d.start_time) AT TIME ZONE 'America/Argentina/Buenos_Aires')
    AND b.end_time = ((d.slot_date + d.end_time) AT TIME ZONE 'America/Argentina/Buenos_Aires')
WHERE b.id IS NULL;

DROP INDEX IF EXISTS idx_disabled_slot_item_date;
DROP TABLE IF EXISTS disabled_time_slot;
