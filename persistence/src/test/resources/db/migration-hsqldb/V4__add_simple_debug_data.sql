INSERT INTO users (name, email, phone)
VALUES ('Admin Botecito', 'botecito.dev@gmail.com', '1100000000');

INSERT INTO item (owner_id, type_id, title, description, price_per_hour, capacity_people, max_weight_kg, difficulty_level, location, active)
VALUES
    ((SELECT id FROM users WHERE email = 'botecito.dev@gmail.com'), 2, 'Kayak Admin Delta', 'Kayak recreativo para paseos tranquilos por el Delta con gran estabilidad para principiantes.', 1800, 2, 180.00, 2, 'Islas del Delta, Tigre', true),
    ((SELECT id FROM users WHERE email = 'botecito.dev@gmail.com'), 3, 'Paddle Admin Madero', 'Tabla de paddle all-round ideal para salidas urbanas al atardecer con equipo listo para usar.', 2200, 1, 120.00, 2, 'Puerto Madero, Buenos Aires', true);

INSERT INTO item_availability (item_id, weekday, start_time, end_time)
VALUES
    ((SELECT id FROM item WHERE title = 'Kayak Admin Delta'), 'SATURDAY', '09:00:00', '13:00:00'),
    ((SELECT id FROM item WHERE title = 'Kayak Admin Delta'), 'SUNDAY', '10:00:00', '14:00:00'),
    ((SELECT id FROM item WHERE title = 'Paddle Admin Madero'), 'FRIDAY', '16:00:00', '20:00:00'),
    ((SELECT id FROM item WHERE title = 'Paddle Admin Madero'), 'SATURDAY', '07:00:00', '11:00:00');
