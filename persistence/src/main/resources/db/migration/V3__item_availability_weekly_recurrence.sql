-- done before deploying a working version of the website with DB connection
-- therefore assumes there is no real client data is stored

CREATE TYPE availability_weekday AS ENUM ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY');

ALTER TABLE item_availability
    ADD COLUMN weekday availability_weekday NOT NULL,
    ALTER COLUMN start_time TYPE TIME USING start_time::TIME,
    ALTER COLUMN end_time TYPE TIME USING end_time::TIME;
