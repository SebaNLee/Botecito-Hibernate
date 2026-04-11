ALTER TABLE item ADD COLUMN location_option_id INT;

INSERT INTO location_option (name)
SELECT locations.location_value
FROM (
    SELECT DISTINCT trim(location) AS location_value
    FROM item
    WHERE location IS NOT NULL
) locations
LEFT JOIN (
    SELECT lower(name) AS normalized_name
    FROM location_option
    GROUP BY lower(name)
) existing_locations ON lower(locations.location_value) = existing_locations.normalized_name
WHERE existing_locations.normalized_name IS NULL;

UPDATE item i
SET location_option_id = normalized_locations.id
FROM (
    SELECT min(id) AS id, lower(name) AS normalized_name
    FROM location_option
    GROUP BY lower(name)
) normalized_locations
WHERE lower(trim(i.location)) = normalized_locations.normalized_name;

ALTER TABLE item ALTER COLUMN location_option_id SET NOT NULL;
ALTER TABLE item ADD CONSTRAINT fk_item_location_option FOREIGN KEY (location_option_id) REFERENCES location_option(id);
ALTER TABLE item DROP COLUMN location;
