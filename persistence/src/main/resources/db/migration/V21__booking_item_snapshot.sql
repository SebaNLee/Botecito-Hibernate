CREATE TABLE item_publication_version (
    id SERIAL,
    item_id INT NOT NULL,
    owner_id INT NOT NULL,
    type_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    price_per_hour INT NOT NULL,
    capacity_people INT NOT NULL,
    max_weight_kg DECIMAL(10,2),
    difficulty_level INT,
    location_option_id INT NOT NULL,
    location_name VARCHAR(120) NOT NULL,
    cover_image_data BYTEA,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_item_publication_version PRIMARY KEY (id),
    CONSTRAINT chk_item_publication_version_price CHECK (price_per_hour >= 0),
    CONSTRAINT chk_item_publication_version_capacity CHECK (capacity_people > 0),
    CONSTRAINT chk_item_publication_version_max_weight CHECK (max_weight_kg IS NULL OR max_weight_kg > 0),
    CONSTRAINT chk_item_publication_version_difficulty CHECK (difficulty_level IS NULL OR difficulty_level BETWEEN 1 AND 5)
);

ALTER TABLE item_booking ADD COLUMN item_version_id INT;
ALTER TABLE item_booking
    ADD CONSTRAINT fk_item_booking_publication_version FOREIGN KEY (item_version_id) REFERENCES item_publication_version(id);

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
    cover_image_data
)
SELECT DISTINCT
    i.id,
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
    )
FROM item_booking b
JOIN item i ON i.id = b.item_id
JOIN location_option lo ON lo.id = i.location_option_id;

UPDATE item_booking b
SET item_version_id = version_match.id
FROM item i
JOIN location_option lo ON lo.id = i.location_option_id
JOIN item_publication_version version_match
    ON version_match.item_id = i.id
    AND version_match.owner_id = i.owner_id
    AND version_match.type_id = i.type_id
    AND version_match.title = i.title
    AND version_match.description IS NOT DISTINCT FROM i.description
    AND version_match.price_per_hour = i.price_per_hour
    AND version_match.capacity_people = i.capacity_people
    AND version_match.max_weight_kg IS NOT DISTINCT FROM i.max_weight_kg
    AND version_match.difficulty_level IS NOT DISTINCT FROM i.difficulty_level
    AND version_match.location_option_id = i.location_option_id
    AND version_match.location_name = lo.name
    AND version_match.cover_image_data IS NOT DISTINCT FROM (
        SELECT m.image_data
        FROM item_media m
        WHERE m.item_id = i.id
        ORDER BY m.display_order ASC, m.id ASC
        LIMIT 1
    )
WHERE b.item_id = i.id;

ALTER TABLE item_publication_version
    ADD CONSTRAINT uq_item_publication_version_id_item UNIQUE (id, item_id);

ALTER TABLE item_booking
    ADD CONSTRAINT fk_item_booking_publication_version_item
        FOREIGN KEY (item_version_id, item_id)
        REFERENCES item_publication_version(id, item_id);

CREATE INDEX idx_item_publication_version_item_id ON item_publication_version(item_id);
CREATE INDEX idx_item_publication_version_owner_id ON item_publication_version(owner_id);
CREATE INDEX idx_item_booking_item_version_id ON item_booking(item_version_id);

ALTER TABLE item_booking
    DROP CONSTRAINT fk_booking_item;

ALTER TABLE item
    DROP COLUMN owner_delete_used_at;
