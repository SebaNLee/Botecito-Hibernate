ALTER TABLE item_publication_version
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN owner_delete_token VARCHAR(120),
    ADD COLUMN item_created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE item_publication_version v
SET active = i.active,
    item_created_at = i.created_at
FROM item i
WHERE i.id = v.item_id;

ALTER TABLE item ADD COLUMN version_id INT;

INSERT INTO item_publication_version (
    item_id,
    owner_id,
    type_id,
    title,
    description,
    price_per_hour,
    capacity_people,
    max_weight_kg,
    difficulty_level,
    location_option_id,
    location_name,
    cover_image_data,
    active,
    owner_delete_token,
    item_created_at,
    created_at
)
SELECT i.id,
       i.owner_id,
       i.type_id,
       i.title,
       i.description,
       i.price_per_hour,
       i.capacity_people,
       i.max_weight_kg,
       i.difficulty_level,
       i.location_option_id,
       lo.name,
       (
           SELECT m.image_data
           FROM item_media m
           WHERE m.item_id = i.id
           ORDER BY m.display_order ASC, m.id ASC
           LIMIT 1
       ),
       i.active,
       i.owner_delete_token,
       i.created_at,
       i.created_at
FROM item i
JOIN location_option lo ON lo.id = i.location_option_id;

UPDATE item i
SET version_id = (
    SELECT MAX(v.id)
    FROM item_publication_version v
    WHERE v.item_id = i.id
);

ALTER TABLE item ALTER COLUMN version_id SET NOT NULL;

ALTER TABLE item
    ADD CONSTRAINT fk_item_current_version FOREIGN KEY (version_id) REFERENCES item_publication_version(id);

ALTER TABLE item
    ADD CONSTRAINT uq_item_version_id UNIQUE (version_id);

ALTER TABLE item_publication_version
    ADD CONSTRAINT fk_item_publication_version_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    ADD CONSTRAINT fk_item_publication_version_type FOREIGN KEY (type_id) REFERENCES item_type(id),
    ADD CONSTRAINT fk_item_publication_version_location_option FOREIGN KEY (location_option_id) REFERENCES location_option(id);

ALTER TABLE item_publication_version
    ADD CONSTRAINT uq_item_publication_version_owner_delete_token UNIQUE (owner_delete_token);

DROP INDEX IF EXISTS idx_item_publication_version_item_id;

ALTER TABLE item_booking
    DROP CONSTRAINT fk_item_booking_publication_version_item;

ALTER TABLE item_publication_version
    DROP CONSTRAINT uq_item_publication_version_id_item;

ALTER TABLE item
    DROP CONSTRAINT fk_item_owner,
    DROP CONSTRAINT fk_item_type,
    DROP CONSTRAINT fk_item_location_option,
    DROP CONSTRAINT chk_item_price,
    DROP CONSTRAINT chk_item_capacity_people,
    DROP CONSTRAINT chk_item_max_weight,
    DROP CONSTRAINT chk_item_difficulty_level,
    DROP CONSTRAINT uq_item_owner_delete_token;

ALTER TABLE item_publication_version
    DROP COLUMN item_id;

ALTER TABLE item
    DROP COLUMN owner_id,
    DROP COLUMN type_id,
    DROP COLUMN title,
    DROP COLUMN description,
    DROP COLUMN price_per_hour,
    DROP COLUMN capacity_people,
    DROP COLUMN max_weight_kg,
    DROP COLUMN difficulty_level,
    DROP COLUMN location_option_id,
    DROP COLUMN active,
    DROP COLUMN owner_delete_token,
    DROP COLUMN created_at;
