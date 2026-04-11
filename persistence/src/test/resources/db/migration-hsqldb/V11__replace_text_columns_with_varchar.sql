UPDATE item
SET description = SUBSTRING(description, 1, 1000)
WHERE description IS NOT NULL
    AND LENGTH(description) > 1000;

ALTER TABLE item
ALTER COLUMN description VARCHAR(1000);

UPDATE item_booking
SET request_message = SUBSTRING(request_message, 1, 1000)
WHERE request_message IS NOT NULL
    AND LENGTH(request_message) > 1000;

ALTER TABLE item_booking
ALTER COLUMN request_message VARCHAR(1000);
