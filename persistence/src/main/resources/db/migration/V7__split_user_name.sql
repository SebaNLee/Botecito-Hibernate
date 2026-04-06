ALTER TABLE users ADD COLUMN names VARCHAR(100);
ALTER TABLE users ADD COLUMN surnames VARCHAR(100);

-- assumes Admin Botecito as the sole existing user, not deployed yet
UPDATE users SET
    names = split_part(trim(name), ' ', 1),
    surnames = CASE
        WHEN strpos(trim(name), ' ') = 0 THEN ''
        ELSE trim(substring(trim(name) from strpos(trim(name), ' ') + 1))
    END;

ALTER TABLE users ALTER COLUMN names SET NOT NULL;
ALTER TABLE users ALTER COLUMN surnames SET NOT NULL;

ALTER TABLE users DROP COLUMN name;
