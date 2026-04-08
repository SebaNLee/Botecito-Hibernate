ALTER TABLE item_availability DROP CONSTRAINT chk_availability_time;

ALTER TABLE item_availability ADD COLUMN weekday VARCHAR(10);
UPDATE item_availability SET weekday = 'MONDAY';
ALTER TABLE item_availability ALTER COLUMN weekday SET NOT NULL;

ALTER TABLE item_availability ALTER COLUMN start_time TIME;
ALTER TABLE item_availability ALTER COLUMN end_time TIME;

ALTER TABLE item_availability ADD CONSTRAINT chk_availability_time CHECK (start_time < end_time);
ALTER TABLE item_availability ADD CONSTRAINT chk_item_availability_weekday CHECK (weekday IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'));
