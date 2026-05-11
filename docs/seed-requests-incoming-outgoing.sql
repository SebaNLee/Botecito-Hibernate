-- =============================================================================
-- Seed data for /requests/incoming and /requests/outgoing (local QA)
-- =============================================================================
-- Target user (must already exist — register or insert this account first):
--   andresgarbarz@gmail.com
--
-- What this script does:
--   1) One ACTIVE publication owned by that user, with many bookings where
--      another user is the guest (incoming list as host).
--   2) Several publications owned by other users, each with multiple bookings
--      where that user is the guest (outgoing list).
--
-- Safe re-run: removes only rows created by this script (tracked via
--   version.title prefix "PAW seed — ").
--
-- Run against your PostgreSQL app database, for example:
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f docs/seed-requests-incoming-outgoing.sql
-- =============================================================================

BEGIN;

-- ---------------------------------------------------------------------------
-- Cleanup from a previous run (version title prefix)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS seed_item_cleanup;
CREATE TEMP TABLE seed_item_cleanup (id INT PRIMARY KEY);

INSERT INTO seed_item_cleanup (id)
SELECT DISTINCT v.item_id
FROM version v
WHERE v.title LIKE 'PAW seed — %';

DELETE FROM booking b
WHERE b.version_id IN (SELECT id FROM version WHERE title LIKE 'PAW seed — %');

DELETE FROM media m
WHERE m.version_id IN (SELECT id FROM version WHERE title LIKE 'PAW seed — %');

DELETE FROM availability a
WHERE a.version_id IN (SELECT id FROM version WHERE title LIKE 'PAW seed — %');

DELETE FROM version v
WHERE v.title LIKE 'PAW seed — %';

DELETE FROM item i
WHERE i.id IN (SELECT id FROM seed_item_cleanup);

DELETE FROM users u
WHERE u.email IN (
    'paw-seed-requests-guest@example.invalid',
    'paw-seed-requests-host-a@example.invalid',
    'paw-seed-requests-host-b@example.invalid'
);

-- ---------------------------------------------------------------------------
-- Resolve catalogue ids and main user
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_andres_id        INT;
    v_guest_id         INT;
    v_host_a_id        INT;
    v_host_b_id        INT;
    v_type_id          INT;
    v_loc_id           INT;
    v_image_id         INT;
    v_item_incoming_id INT;
    v_ver_incoming_id  INT;
    v_item_a_id        INT;
    v_ver_a_id         INT;
    v_item_b_id        INT;
    v_ver_b_id         INT;
    v_day              DATE := DATE '2026-06-15';
    v_statuses         booking_status_enum[] := ARRAY[
        'PENDING'::booking_status_enum,
        'ACCEPTED'::booking_status_enum,
        'PAID'::booking_status_enum,
        'CONFIRMED'::booking_status_enum,
        'REJECTED'::booking_status_enum,
        'CANCELLED'::booking_status_enum,
        'REFUSED'::booking_status_enum,
        'FINISHED'::booking_status_enum
    ];
    v_i                INT;
    v_start_ts         TIMESTAMP;
    v_end_ts           TIMESTAMP;
    v_created          TIMESTAMP;
BEGIN
    SELECT u.id INTO v_andres_id
    FROM users u
    WHERE u.email = 'andresgarbarz@gmail.com';

    IF v_andres_id IS NULL THEN
        RAISE EXCEPTION 'No user with email andresgarbarz@gmail.com — create the account first.';
    END IF;

    SELECT t.id INTO v_type_id FROM item_type t ORDER BY t.id LIMIT 1;
    SELECT l.id INTO v_loc_id FROM location l ORDER BY l.id LIMIT 1;

    IF v_type_id IS NULL OR v_loc_id IS NULL THEN
        RAISE EXCEPTION 'item_type or location table is empty — run migrations / base seed first.';
    END IF;

    -- Helper users (guest on your listing; hosts for your outgoing bookings)
    INSERT INTO users (first_name, last_name, email, phone, language, alias, password_hash,
                       mail_token, mail_token_emitted_at, verified, created_at)
    VALUES ('Seed', 'Guest', 'paw-seed-requests-guest@example.invalid', NULL, 'es', NULL, NULL,
            NULL, NULL, TRUE, NOW()),
           ('Seed', 'HostA', 'paw-seed-requests-host-a@example.invalid', NULL, 'es', NULL, NULL,
            NULL, NULL, TRUE, NOW()),
           ('Seed', 'HostB', 'paw-seed-requests-host-b@example.invalid', NULL, 'es', NULL, NULL,
            NULL, NULL, TRUE, NOW());

    SELECT id INTO v_guest_id FROM users WHERE email = 'paw-seed-requests-guest@example.invalid';
    SELECT id INTO v_host_a_id FROM users WHERE email = 'paw-seed-requests-host-a@example.invalid';
    SELECT id INTO v_host_b_id FROM users WHERE email = 'paw-seed-requests-host-b@example.invalid';

    -- Minimal 1x1 PNG (valid BYTEA for image.data)
    INSERT INTO image (data)
    VALUES (decode(
        '89504e470d0a1a0a0000000d49484452000000010000000108060000001f15c4890000000a49444154789c63000100000500010d0a2db40000000049454e44ae426082',
        'hex'
    ))
    RETURNING id INTO v_image_id;

    -- ========== Incoming: your item + many bookings by guest ==========
    INSERT INTO item (host_id, status, created_at)
    VALUES (v_andres_id, 'ACTIVE'::item_status_enum, NOW())
    RETURNING id INTO v_item_incoming_id;

    INSERT INTO version (item_id, type_id, title, description, price, capacity, weight, difficulty,
                         location_id, timezone, created_at)
    VALUES (
        v_item_incoming_id,
        v_type_id,
        'PAW seed — incoming host item (kayak demo)',
        'Seed data for incoming requests UI.',
        2500.00,
        4,
        120,
        2,
        v_loc_id,
        'America/Argentina/Buenos_Aires',
        NOW()
    )
    RETURNING id INTO v_ver_incoming_id;

    INSERT INTO media (version_id, image_id, index)
    VALUES (v_ver_incoming_id, v_image_id, 0);

    INSERT INTO availability (version_id, weekday, start_time, end_time)
    SELECT v_ver_incoming_id, d.wd, TIME '06:00', TIME '22:00'
    FROM (VALUES
        ('MONDAY'::weekday_enum),
        ('TUESDAY'::weekday_enum),
        ('WEDNESDAY'::weekday_enum),
        ('THURSDAY'::weekday_enum),
        ('FRIDAY'::weekday_enum),
        ('SATURDAY'::weekday_enum),
        ('SUNDAY'::weekday_enum)
    ) AS d(wd);

    -- 14 bookings from the same other user; spaced by day so slot rules stay realistic
    FOR v_i IN 0..13 LOOP
        v_start_ts := (v_day + (v_i || ' days')::interval) + TIME '13:00';
        v_end_ts   := v_start_ts + INTERVAL '3 hours';
        v_created  := NOW() - (v_i || ' hours')::interval;
        INSERT INTO booking (version_id, guest_id, start, "end", status, msg, created_at, updated_at)
        VALUES (
            v_ver_incoming_id,
            v_guest_id,
            v_start_ts,
            v_end_ts,
            v_statuses[1 + (v_i % array_length(v_statuses, 1))],
            NULL,
            v_created,
            v_created
        );
    END LOOP;

    -- ========== Outgoing: two other hosts, many bookings where you are guest ==========
    INSERT INTO item (host_id, status, created_at)
    VALUES (v_host_a_id, 'ACTIVE'::item_status_enum, NOW())
    RETURNING id INTO v_item_a_id;

    INSERT INTO version (item_id, type_id, title, description, price, capacity, weight, difficulty,
                         location_id, timezone, created_at)
    VALUES (
        v_item_a_id,
        v_type_id,
        'PAW seed — outgoing host item A',
        'Host A seed.',
        1800.00,
        2,
        90,
        1,
        v_loc_id,
        'America/Argentina/Buenos_Aires',
        NOW()
    )
    RETURNING id INTO v_ver_a_id;

    INSERT INTO media (version_id, image_id, index)
    VALUES (v_ver_a_id, v_image_id, 0);

    INSERT INTO availability (version_id, weekday, start_time, end_time)
    SELECT v_ver_a_id, d.wd, TIME '06:00', TIME '22:00'
    FROM (VALUES
        ('MONDAY'::weekday_enum),
        ('TUESDAY'::weekday_enum),
        ('WEDNESDAY'::weekday_enum),
        ('THURSDAY'::weekday_enum),
        ('FRIDAY'::weekday_enum),
        ('SATURDAY'::weekday_enum),
        ('SUNDAY'::weekday_enum)
    ) AS d(wd);

    FOR v_i IN 0..9 LOOP
        v_start_ts := (DATE '2026-07-01' + (v_i || ' days')::interval) + TIME '10:00';
        v_end_ts   := v_start_ts + INTERVAL '3 hours';
        v_created  := NOW() - ((20 + v_i) || ' hours')::interval;
        INSERT INTO booking (version_id, guest_id, start, "end", status, msg, created_at, updated_at)
        VALUES (
            v_ver_a_id,
            v_andres_id,
            v_start_ts,
            v_end_ts,
            v_statuses[1 + (v_i % array_length(v_statuses, 1))],
            NULL,
            v_created,
            v_created
        );
    END LOOP;

    INSERT INTO item (host_id, status, created_at)
    VALUES (v_host_b_id, 'ACTIVE'::item_status_enum, NOW())
    RETURNING id INTO v_item_b_id;

    INSERT INTO version (item_id, type_id, title, description, price, capacity, weight, difficulty,
                         location_id, timezone, created_at)
    VALUES (
        v_item_b_id,
        v_type_id,
        'PAW seed — outgoing host item B',
        'Host B seed.',
        3200.00,
        3,
        100,
        3,
        v_loc_id,
        'UTC',
        NOW()
    )
    RETURNING id INTO v_ver_b_id;

    INSERT INTO media (version_id, image_id, index)
    VALUES (v_ver_b_id, v_image_id, 0);

    INSERT INTO availability (version_id, weekday, start_time, end_time)
    SELECT v_ver_b_id, d.wd, TIME '06:00', TIME '22:00'
    FROM (VALUES
        ('MONDAY'::weekday_enum),
        ('TUESDAY'::weekday_enum),
        ('WEDNESDAY'::weekday_enum),
        ('THURSDAY'::weekday_enum),
        ('FRIDAY'::weekday_enum),
        ('SATURDAY'::weekday_enum),
        ('SUNDAY'::weekday_enum)
    ) AS d(wd);

    FOR v_i IN 0..9 LOOP
        v_start_ts := (DATE '2026-08-01' + (v_i || ' days')::interval) + TIME '15:00';
        v_end_ts   := v_start_ts + INTERVAL '3 hours';
        v_created  := NOW() - ((40 + v_i) || ' hours')::interval;
        INSERT INTO booking (version_id, guest_id, start, "end", status, msg, created_at, updated_at)
        VALUES (
            v_ver_b_id,
            v_andres_id,
            v_start_ts,
            v_end_ts,
            v_statuses[1 + ((v_i + 3) % array_length(v_statuses, 1))],
            NULL,
            v_created,
            v_created
        );
    END LOOP;

    -- Keep SERIAL/sequences aligned (Postgres)
    PERFORM setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1));
    PERFORM setval(pg_get_serial_sequence('item', 'id'), COALESCE((SELECT MAX(id) FROM item), 1));
    PERFORM setval(pg_get_serial_sequence('version', 'id'), COALESCE((SELECT MAX(id) FROM version), 1));
    PERFORM setval(pg_get_serial_sequence('booking', 'id'), COALESCE((SELECT MAX(id) FROM booking), 1));
    PERFORM setval(pg_get_serial_sequence('image', 'id'), COALESCE((SELECT MAX(id) FROM image), 1));
    PERFORM setval(pg_get_serial_sequence('availability', 'id'), COALESCE((SELECT MAX(id) FROM availability), 1));
END $$;

COMMIT;
