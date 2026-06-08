UPDATE payment_proof pp
SET refuse_msg = NULL,
    refused_at = NULL
FROM booking b
WHERE pp.booking_id = b.id
  AND b.status IN ('CONFIRMED', 'FINISHED')
  AND (pp.refuse_msg IS NOT NULL OR pp.refused_at IS NOT NULL);

ALTER TABLE payment_proof RENAME COLUMN refuse_msg TO host_msg;
ALTER TABLE payment_proof RENAME COLUMN refused_at TO host_at;
ALTER TABLE payment_proof RENAME COLUMN reply_msg TO guest_msg;
ALTER TABLE payment_proof RENAME COLUMN replied_at TO guest_at;
