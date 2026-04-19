CREATE TABLE disabled_time_slot (
    id         SERIAL PRIMARY KEY,
    item_id    INT  NOT NULL REFERENCES item(id) ON DELETE CASCADE,
    slot_date  DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time   TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_disabled_slot UNIQUE (item_id, slot_date, start_time, end_time),
    CONSTRAINT chk_disabled_slot_time CHECK (start_time < end_time)
);

CREATE INDEX idx_disabled_slot_item_date ON disabled_time_slot(item_id, slot_date);
