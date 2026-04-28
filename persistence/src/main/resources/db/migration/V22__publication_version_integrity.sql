ALTER TABLE item_publication_version
    ADD CONSTRAINT uq_item_publication_version_id_item UNIQUE (id, item_id);

ALTER TABLE item_booking
    ADD CONSTRAINT fk_item_booking_publication_version_item
        FOREIGN KEY (item_version_id, item_id)
        REFERENCES item_publication_version(id, item_id);

CREATE OR REPLACE FUNCTION prevent_item_publication_version_update()
RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'item_publication_version rows are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_item_publication_version_no_update
    BEFORE UPDATE ON item_publication_version
    FOR EACH ROW
    EXECUTE FUNCTION prevent_item_publication_version_update();
