ALTER TABLE item_type
    ADD COLUMN slug VARCHAR(100);

UPDATE item_type
SET slug = CASE name
    WHEN 'Otros' THEN 'otros'
    WHEN 'Kayak' THEN 'kayak'
    WHEN 'Paddle' THEN 'paddle'
    WHEN 'Canoa' THEN 'canoa'
    WHEN 'Windsurf' THEN 'windsurf'
    WHEN 'eFoil' THEN 'efoil'
    WHEN 'Optimist' THEN 'optimist'
    ELSE 'type-' || CAST(id AS VARCHAR(10))
END;

ALTER TABLE item_type
    ALTER COLUMN slug SET NOT NULL;

ALTER TABLE item_type
    ADD CONSTRAINT uq_item_type_slug UNIQUE (slug);


ALTER TABLE location
    ADD COLUMN slug VARCHAR(100);

UPDATE location
SET slug = CASE name
    WHEN 'Portezuelo' THEN 'portezuelo'
    WHEN 'Club Nordelta (Sede Central)' THEN 'club-nordelta-sede-central'
    WHEN 'Los Castores' THEN 'los-castores'
    WHEN 'Cabos del Lago' THEN 'cabos-del-lago'
    WHEN 'La Isla' THEN 'la-isla'
    WHEN 'Puerto Canoas' THEN 'puerto-canoas'
    WHEN 'Yoo' THEN 'yoo'
    WHEN 'El Golf' THEN 'el-golf'
    WHEN 'Islas del Golf' THEN 'islas-del-golf'
    WHEN 'Virazon' THEN 'virazon'
    WHEN 'El Muelle' THEN 'el-muelle'
    WHEN 'Nordelta Rio / Bahia Grande' THEN 'nordelta-rio-bahia-grande'
    WHEN 'El Yacht' THEN 'el-yacht'
    ELSE 'loc-' || CAST(id AS VARCHAR(10))
END;

ALTER TABLE location
    ALTER COLUMN slug SET NOT NULL;

ALTER TABLE location
    ADD CONSTRAINT uq_location_slug UNIQUE (slug);
