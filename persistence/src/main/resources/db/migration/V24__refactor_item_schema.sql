-- =============================================================================
-- V2__refactor_schema.sql
-- Refactor schema: flatten versioning, rename tables, reshape relationships
-- =============================================================================

BEGIN;

-- =============================================================================
-- STEP 1: Create new enums
-- =============================================================================

CREATE TYPE item_status_enum AS ENUM ('ACTIVE', 'INACTIVE', 'DELETED');

CREATE TYPE booking_status_enum AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'PAID',
    'CONFIRMED',
    'REFUSED',
    'CANCELLED',
    'FINISHED'
);

CREATE TYPE weekday_enum AS ENUM (
    'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'
);

CREATE TYPE target_enum AS ENUM ('ITEM', 'USER');

-- =============================================================================
-- STEP 2: Create new tables (no FKs yet — data load first)
-- =============================================================================

-- new_users (was: users)
CREATE TABLE new_users (
    id              SERIAL PRIMARY KEY,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(100)    NOT NULL UNIQUE,
    phone           VARCHAR(30),
    language        VARCHAR(10)     NOT NULL,
    alias           VARCHAR(30),
    password_hash   VARCHAR(255),
    mail_token      VARCHAR(100),
    mail_token_emitted_at TIMESTAMP,
    verified        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL
);

-- item_type (unchanged structurally)
CREATE TABLE new_item_type (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- location (was: location_option)
CREATE TABLE location (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE
);

-- image (new — split out from item_media)
CREATE TABLE image (
    id   SERIAL PRIMARY KEY,
    data BYTEA NOT NULL
);

-- item (reshaped — drops version_id circular ref, gains host_id/status)
CREATE TABLE new_item (
    id         SERIAL PRIMARY KEY,
    host_id    INT,                       -- nullable: SET NULL when user is deleted
    status     item_status_enum NOT NULL,
    created_at TIMESTAMP       NOT NULL
);

-- version (was: item_publication_version, reshaped)
CREATE TABLE version (
    id          SERIAL PRIMARY KEY,
    item_id     INT             NOT NULL,
    type_id     INT             NOT NULL,
    title       VARCHAR(1000)   NOT NULL,
    description VARCHAR(100),
    price       NUMERIC(12,2)   NOT NULL,
    capacity    INT             NOT NULL,
    weight      INT             NOT NULL,
    difficulty  INT             NOT NULL,
    location_id INT             NOT NULL,
    timezone    VARCHAR(50)     NOT NULL DEFAULT 'UTC',
    created_at  TIMESTAMP       NOT NULL
);

-- media (was: item_media reshaped into junction table)
CREATE TABLE media (
    version_id  INT NOT NULL,
    image_id    INT NOT NULL,
    index       INT NOT NULL,
    PRIMARY KEY (version_id, index)
);

-- availability (was: item_availability — now references version not item)
CREATE TABLE availability (
    id         SERIAL PRIMARY KEY,
    version_id INT                      NOT NULL,
    weekday    weekday_enum             NOT NULL,
    start_time TIME                     NOT NULL,
    end_time   TIME                     NOT NULL
);

-- booking (was: item_booking, reshaped)
CREATE TABLE booking (
    id         SERIAL PRIMARY KEY,
    version_id INT                  NOT NULL,
    guest_id   INT,
    start      TIMESTAMP            NOT NULL,
    "end"      TIMESTAMP            NOT NULL,
    status     booking_status_enum  NOT NULL,
    msg        VARCHAR(255),
    created_at TIMESTAMP            NOT NULL,
    updated_at TIMESTAMP            NOT NULL
);

-- payment_proof (was: booking_payment_proof, reshaped)
CREATE TABLE payment_proof (
    id           SERIAL PRIMARY KEY,
    booking_id   INT          NOT NULL UNIQUE,
    filename     VARCHAR(100) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_data    BYTEA        NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    refuse_msg   VARCHAR(255),
    refused_at   TIMESTAMP,
    reply_msg    VARCHAR(255),
    replied_at   TIMESTAMP
);

-- review (reshaped — drops reviewee_user_id and target_id)
CREATE TABLE new_review (
    id          SERIAL PRIMARY KEY,
    booking_id  INT             NOT NULL,
    sender_id   INT,
    target_type target_enum     NOT NULL,
    rating      NUMERIC(2,1)    NOT NULL,
    comment     VARCHAR(255),
    created_at  TIMESTAMP       NOT NULL
);

-- =============================================================================
-- STEP 3: Migrate data
-- =============================================================================

-- new_users
INSERT INTO new_users (id, first_name, last_name, email, phone, language, alias,
                    password_hash, mail_token, mail_token_emitted_at, verified, created_at)
SELECT
    id,
    given_name,
    last_name,
    LEFT(email, 100),
    phone,
    preferred_language,
    LEFT(payment_alias, 30),
    password_hash,
    LEFT(password_recovery_token, 100),
    password_recovery_used_at,
    TRUE,
    created_at AT TIME ZONE 'UTC'
FROM public.users;

-- item_type
INSERT INTO new_item_type (id, name)
SELECT id, name FROM public.item_type;

-- location
INSERT INTO location (id, name)
SELECT id, name FROM public.location_option;

-- item
-- host_id comes from the current version's owner_id
-- status: active=true -> ACTIVE, active=false -> INACTIVE
INSERT INTO new_item (id, host_id, status, created_at)
SELECT
    i.id,
    ipv.owner_id,
    CASE WHEN ipv.active THEN 'ACTIVE'::item_status_enum
         ELSE 'INACTIVE'::item_status_enum
    END,
    ipv.item_created_at AT TIME ZONE 'UTC'
FROM public.item i
JOIN public.item_publication_version ipv ON ipv.id = i.version_id;

-- version (was item_publication_version)
-- weight and difficulty now required in new schema:
--   max_weight_kg NULL -> 2000
--   difficulty_level NULL -> 1
-- price: old is price_per_hour integer (cents?), new is NUMERIC(12,2) — cast directly
-- timezone: old schema had none on version level, defaulting to 'UTC'
-- NOTE: all versions are migrated, not just current ones
INSERT INTO version (id, item_id, type_id, title, description, price,
                     capacity, weight, difficulty, location_id, timezone, created_at)
SELECT
    ipv.id,
    ipv_to_item.item_id,
    ipv.type_id,
    ipv.title,
    LEFT(ipv.description, 100),
    ipv.price_per_hour::NUMERIC(12,2),
    ipv.capacity_people,
    COALESCE(ipv.max_weight_kg::INT, 2000),
    COALESCE(ipv.difficulty_level, 1),
    ipv.location_option_id,
    'UTC',
    ipv.created_at AT TIME ZONE 'UTC'
FROM public.item_publication_version ipv
-- Resolve item_id for every version via the item that currently points to it,
-- then walk back to find the item for non-current versions via item_id on item_booking.
-- Simplest correct approach: build a version->item map from all bookings + current pointer.
JOIN (
    -- current version pointer
    SELECT i.version_id AS ipv_id, i.id AS item_id FROM public.item i
    UNION
    -- historical versions referenced by bookings
    SELECT ib.item_version_id AS ipv_id, ib.item_id AS item_id
    FROM public.item_booking ib
    WHERE ib.item_version_id IS NOT NULL
) ipv_to_item ON ipv_to_item.ipv_id = ipv.id
-- deduplicate in case the same version appears via both paths
GROUP BY ipv.id, ipv_to_item.item_id, ipv.type_id, ipv.title, ipv.description,
         ipv.price_per_hour, ipv.capacity_people, ipv.max_weight_kg,
         ipv.difficulty_level, ipv.location_option_id, ipv.created_at;
-- NOTE: versions not referenced by any item pointer or booking are orphaned and dropped.

-- image + media: migrate item_media rows
-- Each old item_media row becomes one image row and one media row.
-- Cover image (display_order=0 or lowest) gets index=0.
-- We normalise display_order → index by dense-ranking within each item's current version.
INSERT INTO image (id, data)
SELECT id, image_data
FROM public.item_media;

INSERT INTO media (version_id, image_id, index)
SELECT
    i.version_id,
    im.id,
    -- remap display_order to 0-based index relative to the version
    ROW_NUMBER() OVER (PARTITION BY im.item_id ORDER BY im.display_order ASC) - 1
FROM public.item_media im
JOIN public.item i ON i.id = im.item_id;

-- Sync image sequence before inserting cover images so generated ids don't collide
-- with the preserved ids from item_media.
SELECT setval(pg_get_serial_sequence('image', 'id'), COALESCE(MAX(id), 1)) FROM image;

-- cover_image_data from item_publication_version
-- Use a temp table to capture generated image ids alongside their version ids,
-- avoiding any need to match by image content.
CREATE TEMP TABLE cover_image_staging (
    version_id INT,
    data       BYTEA,
    image_id   INT
);

INSERT INTO cover_image_staging (version_id, data)
SELECT ipv.id, ipv.cover_image_data
FROM public.item_publication_version ipv
JOIN public.item i ON i.version_id = ipv.id
WHERE ipv.cover_image_data IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM media m WHERE m.version_id = ipv.id AND m.index = 0
  );

-- Insert images and write back the generated ids using a sequence-matched CTE.
WITH inserted AS (
    INSERT INTO image (data)
    SELECT data FROM cover_image_staging ORDER BY version_id
    RETURNING id
),
ranked_inserted AS (
    SELECT id, ROW_NUMBER() OVER () AS rn FROM inserted
),
ranked_staging AS (
    SELECT version_id, ROW_NUMBER() OVER (ORDER BY version_id) AS rn FROM cover_image_staging
)
UPDATE cover_image_staging cs
SET image_id = ri.id
FROM ranked_inserted ri
JOIN ranked_staging rs ON rs.rn = ri.rn
WHERE cs.version_id = rs.version_id;

INSERT INTO media (version_id, image_id, index)
SELECT version_id, image_id, 0
FROM cover_image_staging;

DROP TABLE cover_image_staging;

-- availability
-- Old: references item_id. New: references version_id.
-- Map via item.version_id (current version of each item).
INSERT INTO availability (id, version_id, weekday, start_time, end_time)
SELECT
    ia.id,
    i.version_id,
    ia.weekday::TEXT::weekday_enum,
    ia.start_time,
    ia.end_time
FROM public.item_availability ia
JOIN public.item i ON i.id = ia.item_id;

-- booking
-- Booking status mapping:
--   BOOKING_PENDING           -> PENDING
--   BOOKING_CONFIRMED         -> ACCEPTED   (host accepted pre-booking)
--   BOOKING_REJECTED          -> REJECTED
--   BOOKING_CANCELLED         -> CANCELLED
--   BOOKING_COMPLETED         -> FINISHED
--   BOOKING_PAYMENT_SUBMITTED -> PAID       (guest uploaded proof, awaiting host confirmation)
--   BOOKING_PAID              -> CONFIRMED  (host confirmed payment received)
--   BOOKING_PAYMENT_REFUSED   -> REFUSED    (host rejected payment proof)
INSERT INTO booking (id, version_id, guest_id, start, "end", status, msg, created_at, updated_at)
SELECT
    ib.id,
    COALESCE(ib.item_version_id, i.version_id),  -- prefer explicit version_id, fallback to item's current version
    ib.guest_id,
    ib.start_time AT TIME ZONE 'UTC',
    ib.end_time AT TIME ZONE 'UTC',
    CASE ib.state
        WHEN 'BOOKING_PENDING'           THEN 'PENDING'::booking_status_enum
        WHEN 'BOOKING_CONFIRMED'         THEN 'ACCEPTED'::booking_status_enum
        WHEN 'BOOKING_REJECTED'          THEN 'REJECTED'::booking_status_enum
        WHEN 'BOOKING_CANCELLED'         THEN 'CANCELLED'::booking_status_enum
        WHEN 'BOOKING_COMPLETED'         THEN 'FINISHED'::booking_status_enum
        WHEN 'BOOKING_PAYMENT_SUBMITTED' THEN 'PAID'::booking_status_enum
        WHEN 'BOOKING_PAID'              THEN 'CONFIRMED'::booking_status_enum
        WHEN 'BOOKING_PAYMENT_REFUSED'   THEN 'REFUSED'::booking_status_enum
    END,
    ib.request_message,
    ib.created_at AT TIME ZONE 'UTC',
    ib.updated_at AT TIME ZONE 'UTC'
FROM public.item_booking ib
LEFT JOIN public.item i ON i.id = ib.item_id
-- Skip bookings where version cannot be resolved (both item_version_id and item are gone)
WHERE COALESCE(ib.item_version_id, i.version_id) IS NOT NULL;

-- payment_proof
INSERT INTO payment_proof (id, booking_id, filename, content_type, file_data,
                           created_at, refuse_msg, refused_at, reply_msg, replied_at)
SELECT
    bpp.id,
    bpp.booking_id,
    LEFT(bpp.file_name, 100),
    bpp.content_type,
    bpp.file_data,
    bpp.created_at AT TIME ZONE 'UTC',
    LEFT(bpp.refusal_reason, 255),
    bpp.refused_at AT TIME ZONE 'UTC',
    LEFT(bpp.guest_reply, 255),
    NULL   -- old schema had no replied_at, defaulting to NULL
FROM public.booking_payment_proof bpp;

-- review
-- Drops: reviewee_user_id (derivable from booking), target_id (redundant with target_type+booking)
-- sender_id was reviewer_user_id
-- rating: old is INT (1-5), new is NUMERIC(2,1) — cast directly
INSERT INTO new_review (id, booking_id, sender_id, target_type, rating, comment, created_at)
SELECT
    id,
    booking_id,
    reviewer_user_id,
    target_type::TEXT::target_enum,
    rating::NUMERIC(2,1),
    LEFT(comment, 255),
    created_at AT TIME ZONE 'UTC'
FROM public.review;

-- =============================================================================
-- STEP 4: Add foreign key constraints now that data is loaded
-- =============================================================================

ALTER TABLE new_item
    ADD CONSTRAINT fk_item_host FOREIGN KEY (host_id) REFERENCES new_users(id) ON DELETE SET NULL;

ALTER TABLE version
    ADD CONSTRAINT fk_version_item     FOREIGN KEY (item_id)     REFERENCES new_item(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_version_type     FOREIGN KEY (type_id)     REFERENCES new_item_type(id),
    ADD CONSTRAINT fk_version_location FOREIGN KEY (location_id) REFERENCES location(id);

ALTER TABLE media
    ADD CONSTRAINT fk_media_version FOREIGN KEY (version_id) REFERENCES version(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_media_image  FOREIGN KEY (image_id)  REFERENCES image(id)   ON DELETE CASCADE;

ALTER TABLE availability
    ADD CONSTRAINT fk_availability_version FOREIGN KEY (version_id) REFERENCES version(id) ON DELETE CASCADE;

ALTER TABLE booking
    ADD CONSTRAINT fk_booking_version FOREIGN KEY (version_id) REFERENCES version(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_booking_guest   FOREIGN KEY (guest_id)   REFERENCES new_users(id) ON DELETE SET NULL;

ALTER TABLE payment_proof
    ADD CONSTRAINT fk_payment_proof_booking FOREIGN KEY (booking_id) REFERENCES booking(id) ON DELETE CASCADE;

ALTER TABLE new_review
    ADD CONSTRAINT fk_review_booking FOREIGN KEY (booking_id) REFERENCES booking(id)  ON DELETE CASCADE,
    ADD CONSTRAINT fk_review_sender  FOREIGN KEY (sender_id)  REFERENCES new_users(id) ON DELETE SET NULL;

-- =============================================================================
-- STEP 5: Drop old tables and enums
-- =============================================================================

DROP TABLE public.review                    CASCADE;
DROP TABLE public.booking_payment_proof     CASCADE;
DROP TABLE public.item_booking              CASCADE;
DROP TABLE public.item_availability         CASCADE;
DROP TABLE public.item_media                CASCADE;
DROP TABLE public.item                      CASCADE;
DROP TABLE public.item_publication_version  CASCADE;
DROP TABLE public.location_option           CASCADE;
DROP TABLE public.item_type                 CASCADE;
DROP TABLE public.users                     CASCADE;

DROP TYPE public.booking_state;
DROP TYPE public.availability_weekday;
DROP TYPE public.review_target_type;

-- =============================================================================
-- STEP 6: Rename new tables to final names
-- =============================================================================

ALTER TABLE new_item_type RENAME TO item_type;
ALTER TABLE new_item      RENAME TO item;
ALTER TABLE new_review    RENAME TO review;
ALTER TABLE new_users     RENAME TO users;

-- Existing self-bookings must end up in a terminal accepted state:
-- if already ended, mark FINISHED; otherwise mark CONFIRMED.
UPDATE booking b
SET status = CASE
    WHEN b."end" < CURRENT_TIMESTAMP THEN 'FINISHED'::booking_status_enum
    ELSE 'CONFIRMED'::booking_status_enum
END
FROM version v
JOIN item i ON i.id = v.item_id
WHERE b.version_id = v.id
  AND b.guest_id IS NOT NULL
  AND b.guest_id = i.host_id;

-- =============================================================================
-- STEP 7: Sync sequences to max existing id so next insert doesn't collide
-- =============================================================================

SELECT setval(pg_get_serial_sequence('users',        'id'), COALESCE(MAX(id), 1)) FROM users;
SELECT setval(pg_get_serial_sequence('item_type',    'id'), COALESCE(MAX(id), 1)) FROM item_type;
SELECT setval(pg_get_serial_sequence('location',     'id'), COALESCE(MAX(id), 1)) FROM location;
SELECT setval(pg_get_serial_sequence('image',        'id'), COALESCE(MAX(id), 1)) FROM image;
SELECT setval(pg_get_serial_sequence('item',         'id'), COALESCE(MAX(id), 1)) FROM item;
SELECT setval(pg_get_serial_sequence('version',      'id'), COALESCE(MAX(id), 1)) FROM version;
SELECT setval(pg_get_serial_sequence('availability', 'id'), COALESCE(MAX(id), 1)) FROM availability;
SELECT setval(pg_get_serial_sequence('booking',      'id'), COALESCE(MAX(id), 1)) FROM booking;
SELECT setval(pg_get_serial_sequence('payment_proof','id'), COALESCE(MAX(id), 1)) FROM payment_proof;
SELECT setval(pg_get_serial_sequence('review',       'id'), COALESCE(MAX(id), 1)) FROM review;

COMMIT;