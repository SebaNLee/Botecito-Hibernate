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
    CONSTRAINT fk_item_publication_version_item FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE,
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
FROM item_booking_snapshot;

UPDATE item_booking b
SET item_version_id = version_match.id
FROM item_booking_snapshot s
JOIN item_publication_version version_match
    ON version_match.item_id = s.item_id
    AND version_match.owner_id = s.owner_id
    AND version_match.type_id = s.type_id
    AND version_match.title = s.title
    AND version_match.description IS NOT DISTINCT FROM s.description
    AND version_match.price_per_hour = s.price_per_hour
    AND version_match.capacity_people = s.capacity_people
    AND version_match.max_weight_kg IS NOT DISTINCT FROM s.max_weight_kg
    AND version_match.difficulty_level IS NOT DISTINCT FROM s.difficulty_level
    AND version_match.location_option_id = s.location_option_id
    AND version_match.location_name = s.location_name
    AND version_match.cover_image_data IS NOT DISTINCT FROM s.cover_image_data
WHERE b.id = s.booking_id;

CREATE INDEX idx_item_publication_version_item_id ON item_publication_version(item_id);
CREATE INDEX idx_item_publication_version_owner_id ON item_publication_version(owner_id);
CREATE INDEX idx_item_booking_item_version_id ON item_booking(item_version_id);
