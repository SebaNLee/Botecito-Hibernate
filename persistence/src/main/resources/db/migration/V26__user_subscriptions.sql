CREATE TABLE user_subscription (
    subscriber_id    INT       NOT NULL,
    subscribed_to_id INT       NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (subscriber_id, subscribed_to_id),
    CONSTRAINT fk_user_subscription_subscriber
        FOREIGN KEY (subscriber_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_subscription_subscribed_to
        FOREIGN KEY (subscribed_to_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_user_subscription_not_self
        CHECK (subscriber_id <> subscribed_to_id)
);
