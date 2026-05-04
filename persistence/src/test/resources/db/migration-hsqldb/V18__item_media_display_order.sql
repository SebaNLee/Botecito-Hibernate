ALTER TABLE item_media ADD COLUMN display_order INT;

ALTER TABLE item_media ALTER COLUMN display_order SET NOT NULL;

ALTER TABLE item_media
    ADD CONSTRAINT item_media_item_position_unique UNIQUE (item_id, display_order);

CREATE INDEX IF NOT EXISTS idx_item_media_item_order
    ON item_media (item_id, display_order);
