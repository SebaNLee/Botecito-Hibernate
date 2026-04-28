CREATE TABLE item_booking_snapshot (
    booking_id INT NOT NULL,
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

    CONSTRAINT pk_item_booking_snapshot PRIMARY KEY (booking_id),
    CONSTRAINT fk_item_booking_snapshot_booking FOREIGN KEY (booking_id) REFERENCES item_booking(id) ON DELETE CASCADE,
    CONSTRAINT chk_item_booking_snapshot_price CHECK (price_per_hour >= 0),
    CONSTRAINT chk_item_booking_snapshot_capacity CHECK (capacity_people > 0),
    CONSTRAINT chk_item_booking_snapshot_max_weight CHECK (max_weight_kg IS NULL OR max_weight_kg > 0),
    CONSTRAINT chk_item_booking_snapshot_difficulty CHECK (difficulty_level IS NULL OR difficulty_level BETWEEN 1 AND 5)
);

INSERT INTO item_booking_snapshot (
    booking_id,
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
SELECT
    b.id,
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
JOIN location_option lo ON lo.id = i.location_option_id
WHERE NOT EXISTS (
    SELECT 1
    FROM item_booking_snapshot existing_snapshot
    WHERE existing_snapshot.booking_id = b.id
);

CREATE INDEX idx_item_booking_snapshot_item_id ON item_booking_snapshot(item_id);
CREATE INDEX idx_item_booking_snapshot_owner_id ON item_booking_snapshot(owner_id);
