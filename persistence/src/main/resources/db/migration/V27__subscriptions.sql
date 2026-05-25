CREATE TABLE subscriptons (
    subscriber_id    INT       NOT NULL,
    subscribed_to_id INT       NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (subscriber_id, subscribed_to_id),
    CONSTRAINT fk_subscriptons_subscriber
        FOREIGN KEY (subscriber_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_subscriptons_subscribed_to
        FOREIGN KEY (subscribed_to_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_subscriptons_not_self
        CHECK (subscriber_id <> subscribed_to_id)
);
