-- remove legacy debug rows introduced in V4
-- not necessary to migrate to location_option (we already have real data)
DELETE FROM item_booking
WHERE item_id IN (
    SELECT id
    FROM item
    WHERE title IN (
        'Kayak Admin Delta',
        'Paddle Admin Madero'
    )
);

DELETE FROM item_availability
WHERE item_id IN (
    SELECT id
    FROM item
    WHERE title IN (
        'Kayak Admin Delta',
        'Paddle Admin Madero'
    )
);

DELETE FROM item_media
WHERE item_id IN (
    SELECT id
    FROM item
    WHERE title IN (
        'Kayak Admin Delta',
        'Paddle Admin Madero'
    )
);

DELETE FROM item
WHERE title IN (
    'Kayak Admin Delta',
    'Paddle Admin Madero'
);
