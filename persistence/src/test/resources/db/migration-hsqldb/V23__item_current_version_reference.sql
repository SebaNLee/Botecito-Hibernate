ALTER TABLE item_publication_version ADD COLUMN active BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE item_publication_version ADD COLUMN owner_delete_token VARCHAR(120);
ALTER TABLE item_publication_version ADD COLUMN item_created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;

UPDATE item_publication_version v
SET active = (SELECT i.active FROM item i WHERE i.id = v.item_id),
    item_created_at = (SELECT i.created_at FROM item i WHERE i.id = v.item_id);

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
ALTER TABLE item ADD CONSTRAINT fk_item_current_version FOREIGN KEY (version_id) REFERENCES item_publication_version(id);
ALTER TABLE item ADD CONSTRAINT uq_item_version_id UNIQUE (version_id);
ALTER TABLE item_publication_version
    ADD CONSTRAINT fk_item_publication_version_owner FOREIGN KEY (owner_id) REFERENCES users(id);
ALTER TABLE item_publication_version
    ADD CONSTRAINT fk_item_publication_version_type FOREIGN KEY (type_id) REFERENCES item_type(id);
ALTER TABLE item_publication_version
    ADD CONSTRAINT fk_item_publication_version_location_option FOREIGN KEY (location_option_id) REFERENCES location_option(id);
ALTER TABLE item_publication_version
    ADD CONSTRAINT uq_item_publication_version_owner_delete_token UNIQUE (owner_delete_token);

DROP INDEX idx_item_publication_version_item_id;

ALTER TABLE item_booking DROP CONSTRAINT fk_item_booking_publication_version_item;
ALTER TABLE item_publication_version DROP CONSTRAINT uq_item_publication_version_id_item;

ALTER TABLE item DROP CONSTRAINT fk_item_owner;
ALTER TABLE item DROP CONSTRAINT fk_item_type;
ALTER TABLE item DROP CONSTRAINT fk_item_location_option;
ALTER TABLE item DROP CONSTRAINT chk_item_price;
ALTER TABLE item DROP CONSTRAINT chk_item_capacity_people;
ALTER TABLE item DROP CONSTRAINT chk_item_max_weight;
ALTER TABLE item DROP CONSTRAINT chk_item_difficulty_level;
ALTER TABLE item DROP CONSTRAINT uq_item_owner_delete_token;

ALTER TABLE item_publication_version DROP COLUMN item_id;

ALTER TABLE item DROP COLUMN owner_id;
ALTER TABLE item DROP COLUMN type_id;
ALTER TABLE item DROP COLUMN title;
ALTER TABLE item DROP COLUMN description;
ALTER TABLE item DROP COLUMN price_per_hour;
ALTER TABLE item DROP COLUMN capacity_people;
ALTER TABLE item DROP COLUMN max_weight_kg;
ALTER TABLE item DROP COLUMN difficulty_level;
ALTER TABLE item DROP COLUMN location_option_id;
ALTER TABLE item DROP COLUMN active;
ALTER TABLE item DROP COLUMN owner_delete_token;
ALTER TABLE item DROP COLUMN created_at;
