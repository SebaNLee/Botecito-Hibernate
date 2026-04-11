UPDATE item
SET description = left(description, 1000)
WHERE description IS NOT NULL
    AND length(description) > 1000;

ALTER TABLE item
ALTER COLUMN description TYPE VARCHAR(1000);

UPDATE item_booking
SET request_message = left(request_message, 1000)
WHERE request_message IS NOT NULL
    AND length(request_message) > 1000;

ALTER TABLE item_booking
ALTER COLUMN request_message TYPE VARCHAR(1000);
