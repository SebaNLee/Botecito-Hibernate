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
    CAST(CAST(d.slot_date AS VARCHAR(10)) || ' ' || CAST(d.start_time AS VARCHAR(8)) || ' -03:00' AS TIMESTAMP WITH TIME ZONE),
    CAST(CAST(d.slot_date AS VARCHAR(10)) || ' ' || CAST(d.end_time AS VARCHAR(8)) || ' -03:00' AS TIMESTAMP WITH TIME ZONE),
    'BOOKING_CONFIRMED',
    NULL,
    'migrated-disabled-slot-' || CAST(d.id AS VARCHAR(20)),
    COALESCE(d.created_at, CURRENT_TIMESTAMP),
    COALESCE(d.created_at, CURRENT_TIMESTAMP),
    COALESCE(d.created_at, CURRENT_TIMESTAMP)
FROM disabled_time_slot d
JOIN item i ON i.id = d.item_id
LEFT JOIN item_booking b
    ON b.item_id = d.item_id
    AND b.guest_id = i.owner_id
    AND b.start_time = CAST(CAST(d.slot_date AS VARCHAR(10)) || ' ' || CAST(d.start_time AS VARCHAR(8)) || ' -03:00' AS TIMESTAMP WITH TIME ZONE)
    AND b.end_time = CAST(CAST(d.slot_date AS VARCHAR(10)) || ' ' || CAST(d.end_time AS VARCHAR(8)) || ' -03:00' AS TIMESTAMP WITH TIME ZONE)
WHERE b.id IS NULL;

DROP INDEX idx_disabled_slot_item_date IF EXISTS;
DROP TABLE disabled_time_slot IF EXISTS;
