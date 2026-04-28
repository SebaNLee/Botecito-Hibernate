ALTER TABLE item_booking DROP CONSTRAINT chk_booking_state;

ALTER TABLE item_booking ADD CONSTRAINT chk_booking_state CHECK (
    state IN (
        'BOOKING_PENDING',
        'BOOKING_CONFIRMED',
        'BOOKING_REJECTED',
        'BOOKING_CANCELLED',
        'BOOKING_COMPLETED',
        'BOOKING_PAYMENT_SUBMITTED',
        'BOOKING_PAID',
        'BOOKING_PAYMENT_REFUSED'
    )
);

ALTER TABLE booking_payment_proof ADD COLUMN refusal_reason LONGVARCHAR;
ALTER TABLE booking_payment_proof ADD COLUMN refused_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE booking_payment_proof ADD COLUMN guest_reply LONGVARCHAR;
