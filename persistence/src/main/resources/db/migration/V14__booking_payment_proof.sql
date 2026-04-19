-- flyway:executeInTransaction=false

ALTER TYPE booking_state ADD VALUE IF NOT EXISTS 'BOOKING_PAYMENT_SUBMITTED';
ALTER TYPE booking_state ADD VALUE IF NOT EXISTS 'BOOKING_PAID';

CREATE TABLE booking_payment_proof (
    id SERIAL,
    booking_id INT NOT NULL,
    uploader_id INT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_data BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_booking_payment_proof PRIMARY KEY (id),
    CONSTRAINT fk_booking_payment_proof_booking FOREIGN KEY (booking_id) REFERENCES item_booking(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_payment_proof_uploader FOREIGN KEY (uploader_id) REFERENCES users(id),
    CONSTRAINT uq_booking_payment_proof_booking UNIQUE (booking_id)
);
