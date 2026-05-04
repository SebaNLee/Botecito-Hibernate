CREATE TYPE review_target_type AS ENUM ('ITEM', 'USER');

CREATE TABLE review (
    id SERIAL,
    booking_id INT NOT NULL,
    reviewer_user_id INT NOT NULL,
    reviewee_user_id INT NOT NULL,
    target_type review_target_type NOT NULL,
    target_id INT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_review PRIMARY KEY (id),
    CONSTRAINT fk_review_booking FOREIGN KEY (booking_id) REFERENCES item_booking(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES users(id),
    CONSTRAINT fk_review_reviewee FOREIGN KEY (reviewee_user_id) REFERENCES users(id),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_review_reviewer_reviewee CHECK (reviewer_user_id <> reviewee_user_id),
    CONSTRAINT uq_review_direction UNIQUE (booking_id, reviewer_user_id, target_type)
);

CREATE INDEX idx_review_target ON review(target_type, target_id);
CREATE INDEX idx_review_reviewee ON review(reviewee_user_id);
CREATE INDEX idx_review_reviewer ON review(reviewer_user_id);
