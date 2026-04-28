ALTER TABLE item_booking
    DROP CONSTRAINT fk_booking_item;

ALTER TABLE item_publication_version
    DROP CONSTRAINT fk_item_publication_version_item;

ALTER TABLE item
    DROP COLUMN owner_delete_used_at;
