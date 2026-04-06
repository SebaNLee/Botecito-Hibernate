
ALTER TABLE users ADD COLUMN preferred_language VARCHAR(10) NOT NULL DEFAULT 'es';

ALTER TABLE users ADD CONSTRAINT chk_users_preferred_language CHECK (preferred_language IN ('es', 'en'));

ALTER TABLE item ADD COLUMN owner_delete_token VARCHAR(120);
ALTER TABLE item ADD COLUMN owner_delete_used_at TIMESTAMPTZ;
ALTER TABLE item ADD CONSTRAINT uq_item_owner_delete_token UNIQUE (owner_delete_token);
