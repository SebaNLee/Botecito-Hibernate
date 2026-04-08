CREATE TABLE location_option (
    id SERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    CONSTRAINT uq_location_option_name UNIQUE (name)
);

INSERT INTO location_option (name) VALUES
    ('Portezuelo'),
    ('Club Nordelta (Sede Central)'),
    ('Los Castores'),
    ('Cabos del Lago'),
    ('La Isla'),
    ('Puerto Canoas'),
    ('Yoo'),
    ('El Golf'),
    ('Islas del Golf'),
    ('Virazón'),
    ('El Muelle'),
    ('Nordelta Río / Bahía Grande'),
    ('El Yacht');
