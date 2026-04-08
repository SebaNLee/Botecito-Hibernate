ALTER TABLE users ADD COLUMN given_name VARCHAR(100);
ALTER TABLE users ADD COLUMN last_name VARCHAR(100);

UPDATE users
SET
    given_name = CASE
        WHEN LOCATE(' ', TRIM(name)) = 0 THEN TRIM(name)
        ELSE SUBSTRING(TRIM(name), 1, LOCATE(' ', TRIM(name)) - 1)
    END,
    last_name = CASE
        WHEN LOCATE(' ', TRIM(name)) = 0 THEN ''
        ELSE LTRIM(SUBSTRING(TRIM(name), LOCATE(' ', TRIM(name)) + 1))
    END;

ALTER TABLE users ALTER COLUMN given_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN last_name SET NOT NULL;
ALTER TABLE users DROP COLUMN name;
