ALTER TABLE item ADD COLUMN location_option_id INT;

INSERT INTO location_option (name)
SELECT DISTINCT TRIM(i.location)
FROM item i
WHERE i.location IS NOT NULL
    AND TRIM(i.location) <> ''
    AND NOT EXISTS (
        SELECT 1
        FROM location_option lo
        WHERE LOWER(lo.name) = LOWER(TRIM(i.location))
    );

UPDATE item i
SET location_option_id = (
    SELECT MIN(lo.id)
    FROM location_option lo
    WHERE LOWER(lo.name) = LOWER(TRIM(i.location))
);

ALTER TABLE item ALTER COLUMN location_option_id SET NOT NULL;
ALTER TABLE item ADD CONSTRAINT fk_item_location_option FOREIGN KEY (location_option_id) REFERENCES location_option(id);
ALTER TABLE item DROP COLUMN location;
