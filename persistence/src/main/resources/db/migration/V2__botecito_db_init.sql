
-- BOOKING_PENDING: request created and waiting for owner decision
-- BOOKING_CONFIRMED: request accepted by the owner
-- BOOKING_REJECTED: request rejected by the owner
-- BOOKING_CANCELLED: booking cancelled by guest or owner (after being confirmed)
-- BOOKING_COMPLETED: booking finished after item usage
CREATE TYPE booking_state AS ENUM ('BOOKING_PENDING', 'BOOKING_CONFIRMED', 'BOOKING_REJECTED', 'BOOKING_CANCELLED', 'BOOKING_COMPLETED');

CREATE TABLE users (
    id SERIAL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(30),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE item_type (
    id SERIAL,
    name VARCHAR(100) NOT NULL,

    CONSTRAINT pk_item_type PRIMARY KEY (id),
    CONSTRAINT uq_item_type_name UNIQUE (name)
);

CREATE TABLE item (
    id SERIAL,
    owner_id INT NOT NULL,
    type_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    price_per_hour INT NOT NULL,
    capacity_people INT NOT NULL,
    max_weight_kg DECIMAL(10,2),
    difficulty_level INT,
    location VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_item PRIMARY KEY (id),
    CONSTRAINT fk_item_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT fk_item_type FOREIGN KEY (type_id) REFERENCES item_type(id),
    CONSTRAINT chk_item_price CHECK (price_per_hour >= 0),
    CONSTRAINT chk_item_capacity_people CHECK (capacity_people > 0),
    CONSTRAINT chk_item_max_weight CHECK (max_weight_kg IS NULL OR max_weight_kg > 0),
    CONSTRAINT chk_item_difficulty_level CHECK (difficulty_level IS NULL OR difficulty_level BETWEEN 1 AND 5)
);

CREATE TABLE item_availability (
    id SERIAL,
    item_id INT NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_item_availability PRIMARY KEY (id),
    CONSTRAINT fk_availability_item FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE,
    CONSTRAINT chk_availability_time CHECK (start_time < end_time)
);

CREATE TABLE item_booking (
    id SERIAL,
    item_id INT NOT NULL,
    guest_id INT NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    state booking_state NOT NULL DEFAULT 'BOOKING_PENDING',
    request_message TEXT,
    host_decision_token VARCHAR(120) NOT NULL, -- pre-auth: the owner receives this token by email and can approve/reject without login
    host_decision_used_at TIMESTAMPTZ, -- pre-auth: marks one-time link usage to avoid processing the same decision twice
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_item_booking PRIMARY KEY (id),
    CONSTRAINT fk_booking_item FOREIGN KEY (item_id) REFERENCES item(id),
    CONSTRAINT fk_booking_guest FOREIGN KEY (guest_id) REFERENCES users(id),
    CONSTRAINT uq_booking_host_decision_token UNIQUE (host_decision_token),
    CONSTRAINT chk_booking_time CHECK (start_time < end_time)
);

INSERT INTO item_type (name) VALUES
    ('Otros'),
    ('Kayak'),
    ('Paddle'),
    ('Canoa'),
    ('Windsurf'),
    ('eFoil'),
    ('Optimist');
