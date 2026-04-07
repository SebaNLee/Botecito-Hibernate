ALTER TABLE users ADD COLUMN given_name VARCHAR(100);
ALTER TABLE users ADD COLUMN last_name VARCHAR(100);

-- assumes Admin Botecito as the sole existing user, not deployed yet
UPDATE users SET
    given_name = split_part(trim(name), ' ', 1),
    last_name = CASE
        WHEN strpos(trim(name), ' ') = 0 THEN ''
        ELSE trim(substring(trim(name) from strpos(trim(name), ' ') + 1))
    END;

ALTER TABLE users ALTER COLUMN given_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN last_name SET NOT NULL;
ALTER TABLE users DROP COLUMN name;
