ALTER TABLE users ADD COLUMN admin BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TYPE report_enum AS ENUM (
    'FAKE',
    'ABANDONED',
    'DUPLICATE',
    'SPAM',
    'IRRELEVANT',
    'INAPPROPRIATE',
    'OTHER'
);

CREATE TABLE reports (
    id          SERIAL PRIMARY KEY,
    sender_id   INT REFERENCES users(id) ON DELETE SET NULL,
    item_id     INT NOT NULL REFERENCES item(id) ON DELETE CASCADE,
    reason      report_enum NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reports_sender_item UNIQUE (sender_id, item_id)
);

CREATE INDEX idx_reports_created_at ON reports(created_at);
