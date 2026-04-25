ALTER TABLE users
ADD COLUMN password_recovery_token VARCHAR(120);

ALTER TABLE users
ADD COLUMN password_recovery_used_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE users
ADD CONSTRAINT uq_users_password_recovery_token UNIQUE (password_recovery_token);
