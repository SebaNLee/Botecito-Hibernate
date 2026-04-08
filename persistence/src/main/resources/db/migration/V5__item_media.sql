CREATE TABLE IF NOT EXISTS item_media (
    id SERIAL PRIMARY KEY,
    item_id INT NOT NULL,
    image_data BYTEA NOT NULL,
    FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE
);
