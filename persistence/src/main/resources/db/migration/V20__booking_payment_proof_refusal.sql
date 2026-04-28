-- flyway:executeInTransaction=false

ALTER TYPE booking_state ADD VALUE IF NOT EXISTS 'BOOKING_PAYMENT_REFUSED';

ALTER TABLE booking_payment_proof
    ADD COLUMN IF NOT EXISTS refusal_reason TEXT,
    ADD COLUMN IF NOT EXISTS refused_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS guest_reply TEXT;
