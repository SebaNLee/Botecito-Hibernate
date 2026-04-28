ALTER TABLE item_publication_version
    ADD CONSTRAINT uq_item_publication_version_id_item UNIQUE (id, item_id);

ALTER TABLE item_booking
    ADD CONSTRAINT fk_item_booking_publication_version_item
        FOREIGN KEY (item_version_id, item_id)
        REFERENCES item_publication_version(id, item_id);
